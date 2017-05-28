/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.nio.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

public abstract class CharsetEncoder {
	private static final int RESET = 0;
	private static final int ONGOING = 1;
	private static final int END_OF_INPUT = 2;
	private static final int FLUSHED = 3;

	private final Charset charset;

	private final float averageBytesPerChar;
	private final float maxBytesPerChar;

	private byte[] replacementBytes;

	private int state = RESET;

	static private CoderResult dummyCR = CoderResult.OVERFLOW;
	static private CodingErrorAction malformedInputAction = CodingErrorAction.REPORT;
	static private CodingErrorAction unmappableCharacterAction = CodingErrorAction.REPORT;

	// decoder instance for this encoder's charset, used for replacement value checking
	private CharsetDecoder decoder;

	/**
	 * Constructs a new {@code CharsetEncoder} using the given parameters and
	 * the replacement byte array {@code { (byte) '?' }}.
	 */
	protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar) {
		this(cs, averageBytesPerChar, maxBytesPerChar, new byte[] { (byte) '?' });
	}

	/**
	 * Constructs a new <code>CharsetEncoder</code> using the given
	 * <code>Charset</code>, replacement byte array, average number and
	 * maximum number of bytes created by this encoder for one input character.
	 *
	 * @param cs
	 *            the <code>Charset</code> to be used by this encoder.
	 * @param averageBytesPerChar
	 *            average number of bytes created by this encoder for one single
	 *            input character, must be positive.
	 * @param maxBytesPerChar
	 *            maximum number of bytes which can be created by this encoder
	 *            for one single input character, must be positive.
	 * @param replacement
	 *            the replacement byte array, cannot be null or empty, its
	 *            length cannot be larger than <code>maxBytesPerChar</code>,
	 *            and must be a legal replacement, which can be justified by
	 *            {@link #isLegalReplacement(byte[]) isLegalReplacement}.
	 * @throws IllegalArgumentException
	 *             if any parameters are invalid.
	 */
	protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement) {
		this(cs, averageBytesPerChar, maxBytesPerChar, replacement, false);
	}

	CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement, boolean trusted) {
		if (averageBytesPerChar <= 0 || maxBytesPerChar <= 0) {
			throw new IllegalArgumentException("averageBytesPerChar and maxBytesPerChar must both be positive");
		}
		if (averageBytesPerChar > maxBytesPerChar) {
			throw new IllegalArgumentException("averageBytesPerChar is greater than maxBytesPerChar");
		}
		this.charset = cs;
		this.averageBytesPerChar = averageBytesPerChar;
		this.maxBytesPerChar = maxBytesPerChar;
		if (trusted) {
			// The RI enforces unnecessary restrictions on the replacement bytes. We trust ICU to
			// know what it's doing. Doing so lets us support ICU's EUC-JP, SCSU, and Shift_JIS.
			this.replacementBytes = replacement;
		} else {
			replaceWith(replacement);
		}
	}

	/**
	 * Returns the average number of bytes created by this encoder for a single
	 * input character.
	 */
	public final float averageBytesPerChar() {
		return averageBytesPerChar;
	}

	/**
	 * Tests whether the given character can be encoded by this encoder.
	 *
	 * <p>Note that this method may change the internal state of this encoder, so
	 * it should not be called when another encoding process is ongoing,
	 * otherwise it will throw an <code>IllegalStateException</code>.
	 *
	 * @throws IllegalStateException if another encode process is ongoing.
	 */
	public boolean canEncode(char c) {
		return canEncode(CharBuffer.wrap(new char[] { c }));
	}

	/**
	 * Tests whether the given <code>CharSequence</code> can be encoded by this
	 * encoder.
	 *
	 * <p>Note that this method may change the internal state of this encoder, so
	 * it should not be called when another encode process is ongoing, otherwise
	 * it will throw an <code>IllegalStateException</code>.
	 *
	 * @throws IllegalStateException if another encode process is ongoing.
	 */
	public boolean canEncode(CharSequence sequence) {
		CharBuffer cb;
		if (sequence instanceof CharBuffer) {
			cb = ((CharBuffer) sequence).duplicate();
		} else {
			cb = CharBuffer.wrap(sequence);
		}

		if (state == FLUSHED) {
			reset();
		}
		if (state != RESET) {
			throw illegalStateException();
		}

		CodingErrorAction originalMalformedInputAction = malformedInputAction;
		CodingErrorAction originalUnmappableCharacterAction = unmappableCharacterAction;
		onMalformedInput(CodingErrorAction.REPORT);
		onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			encode(cb);
			return true;
		} catch (CharacterCodingException e) {
			return false;
		} finally {
			onMalformedInput(originalMalformedInputAction);
			onUnmappableCharacter(originalUnmappableCharacterAction);
			reset();
		}
	}

	/**
	 * Returns the {@link Charset} which this encoder uses.
	 */
	public final Charset charset() {
		return charset;
	}

	/**
	 * This is a facade method for the encoding operation.
	 * <p>
	 * This method encodes the remaining character sequence of the given
	 * character buffer into a new byte buffer. This method performs a complete
	 * encoding operation, resets at first, then encodes, and flushes at last.
	 * <p>
	 * This method should not be invoked if another encode operation is ongoing.
	 *
	 * @param in
	 *            the input buffer.
	 * @return a new <code>ByteBuffer</code> containing the bytes produced by
	 *         this encoding operation. The buffer's limit will be the position
	 *         of the last byte in the buffer, and the position will be zero.
	 * @throws IllegalStateException
	 *             if another encoding operation is ongoing.
	 * @throws MalformedInputException
	 *             if an illegal input character sequence for this charset is
	 *             encountered, and the action for malformed error is
	 *             {@link CodingErrorAction#REPORT CodingErrorAction.REPORT}
	 * @throws UnmappableCharacterException
	 *             if a legal but unmappable input character sequence for this
	 *             charset is encountered, and the action for unmappable
	 *             character error is
	 *             {@link CodingErrorAction#REPORT CodingErrorAction.REPORT}.
	 *             Unmappable means the Unicode character sequence at the input
	 *             buffer's current position cannot be mapped to a equivalent
	 *             byte sequence.
	 * @throws CharacterCodingException
	 *             if other exception happened during the encode operation.
	 */
	public final ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
		int length = (int) (in.remaining() * averageBytesPerChar);
		ByteBuffer out = ByteBuffer.allocate(length);

		reset();

		while (state != FLUSHED) {
			CoderResult result = encode(in, out, true);
			if (result == CoderResult.OVERFLOW) {
				out = allocateMore(out);
				continue; // No point trying to flush to an already-full buffer.
			} else {
				checkCoderResult(result);
			}

			result = flush(out);
			if (result == CoderResult.OVERFLOW) {
				out = allocateMore(out);
			} else {
				checkCoderResult(result);
			}
		}

		out.flip();
		return out;
	}

	private void checkCoderResult(CoderResult result) throws CharacterCodingException {
		if (malformedInputAction == CodingErrorAction.REPORT && result.isMalformed()) {
			throw new MalformedInputException(result.length());
		} else if (unmappableCharacterAction == CodingErrorAction.REPORT && result.isUnmappable()) {
			throw new UnmappableCharacterException(result.length());
		}
	}

	private ByteBuffer allocateMore(ByteBuffer output) {
		if (output.capacity() == 0) {
			return ByteBuffer.allocate(1);
		}
		ByteBuffer result = ByteBuffer.allocate(output.capacity() * 2);
		output.flip();
		result.put(output);
		return result;
	}

	/**
	 * Encodes characters starting at the current position of the given input
	 * buffer, and writes the equivalent byte sequence into the given output
	 * buffer from its current position.
	 * <p>
	 * The buffers' position will be changed with the reading and writing
	 * operation, but their limits and marks will be kept intact.
	 * <p>
	 * A <code>CoderResult</code> instance will be returned according to
	 * following rules:
	 * <ul>
	 * <li>A {@link CoderResult#malformedForLength(int) malformed input} result
	 * indicates that some malformed input error was encountered, and the
	 * erroneous characters start at the input buffer's position and their
	 * number can be got by result's {@link CoderResult#length() length}. This
	 * kind of result can be returned only if the malformed action is
	 * {@link CodingErrorAction#REPORT CodingErrorAction.REPORT}.</li>
	 * <li>{@link CoderResult#UNDERFLOW CoderResult.UNDERFLOW} indicates that
	 * as many characters as possible in the input buffer have been encoded. If
	 * there is no further input and no characters left in the input buffer then
	 * this task is complete. If this is not the case then the client should
	 * call this method again supplying some more input characters.</li>
	 * <li>{@link CoderResult#OVERFLOW CoderResult.OVERFLOW} indicates that the
	 * output buffer has been filled, while there are still some characters
	 * remaining in the input buffer. This method should be invoked again with a
	 * non-full output buffer.</li>
	 * <li>A {@link CoderResult#unmappableForLength(int) unmappable character}
	 * result indicates that some unmappable character error was encountered,
	 * and the erroneous characters start at the input buffer's position and
	 * their number can be got by result's {@link CoderResult#length() length}.
	 * This kind of result can be returned only on
	 * {@link CodingErrorAction#REPORT CodingErrorAction.REPORT}.</li>
	 * </ul>
	 * <p>
	 * The <code>endOfInput</code> parameter indicates if the invoker can
	 * provider further input. This parameter is true if and only if the
	 * characters in the current input buffer are all inputs for this encoding
	 * operation. Note that it is common and won't cause an error if the invoker
	 * sets false and then has no more input available, while it may cause an
	 * error if the invoker always sets true in several consecutive invocations.
	 * This would make the remaining input to be treated as malformed input.
	 * input.
	 * <p>
	 * This method invokes the
	 * {@link #encodeLoop(CharBuffer, ByteBuffer) encodeLoop} method to
	 * implement the basic encode logic for a specific charset.
	 *
	 * @param in
	 *            the input buffer.
	 * @param out
	 *            the output buffer.
	 * @param endOfInput
	 *            true if all the input characters have been provided.
	 * @return a <code>CoderResult</code> instance indicating the result.
	 * @throws IllegalStateException
	 *             if the encoding operation has already started or no more
	 *             input is needed in this encoding process.
	 * @throws CoderMalfunctionError
	 *             If the {@link #encodeLoop(CharBuffer, ByteBuffer) encodeLoop}
	 *             method threw an <code>BufferUnderflowException</code> or
	 *             <code>BufferUnderflowException</code>.
	 */
	public final CoderResult encode(CharBuffer in, ByteBuffer out, boolean endOfInput) {
		if (state != RESET && state != ONGOING && !(endOfInput && state == END_OF_INPUT)) {
			throw illegalStateException();
		}

		state = endOfInput ? END_OF_INPUT : ONGOING;

		while (true) {
			CoderResult result;
			try {
				result = encodeLoop(in, out);
			} catch (BufferOverflowException ex) {
				throw new CoderMalfunctionError(ex);
			} catch (BufferUnderflowException ex) {
				throw new CoderMalfunctionError(ex);
			}

			if (result == CoderResult.UNDERFLOW) {
				if (endOfInput && in.hasRemaining()) {
					result = CoderResult.malformedForLength(in.remaining());
				} else {
					return result;
				}
			} else if (result == CoderResult.OVERFLOW) {
				return result;
			}

			// We have a real error, so do what the appropriate action tells us what to do...
			CodingErrorAction action =
				result.isUnmappable() ? unmappableCharacterAction : malformedInputAction;
			if (action == CodingErrorAction.REPORT) {
				return result;
			} else if (action == CodingErrorAction.REPLACE) {
				if (out.remaining() < replacementBytes.length) {
					return CoderResult.OVERFLOW;
				}
				out.put(replacementBytes);
			}
			in.position(in.position() + result.length());
		}
	}

	/**
	 * Encodes characters into bytes. This method is called by
	 * {@link #encode(CharBuffer, ByteBuffer, boolean) encode}.
	 * <p>
	 * This method will implement the essential encoding operation, and it won't
	 * stop encoding until either all the input characters are read, the output
	 * buffer is filled, or some exception is encountered. Then it will
	 * return a <code>CoderResult</code> object indicating the result of the
	 * current encoding operation. The rule to construct the
	 * <code>CoderResult</code> is the same as for
	 * {@link #encode(CharBuffer, ByteBuffer, boolean) encode}. When an
	 * exception is encountered in the encoding operation, most implementations
	 * of this method will return a relevant result object to the
	 * {@link #encode(CharBuffer, ByteBuffer, boolean) encode} method, and
	 * subclasses may handle the exception and
	 * implement the error action themselves.
	 * <p>
	 * The buffers are scanned from their current positions, and their positions
	 * will be modified accordingly, while their marks and limits will be
	 * intact. At most {@link CharBuffer#remaining() in.remaining()} characters
	 * will be read, and {@link ByteBuffer#remaining() out.remaining()} bytes
	 * will be written.
	 * <p>
	 * Note that some implementations may pre-scan the input buffer and return
	 * <code>CoderResult.UNDERFLOW</code> until it receives sufficient input.
	 * <p>
	 * @param in
	 *            the input buffer.
	 * @param out
	 *            the output buffer.
	 * @return a <code>CoderResult</code> instance indicating the result.
	 */
	protected abstract CoderResult encodeLoop(CharBuffer in, ByteBuffer out);

	/**
	 * Flushes this encoder.
	 * <p>
	 * This method will call {@link #implFlush(ByteBuffer) implFlush}. Some
	 * encoders may need to write some bytes to the output buffer when they have
	 * read all input characters, subclasses can overridden
	 * {@link #implFlush(ByteBuffer) implFlush} to perform writing action.
	 * <p>
	 * The maximum number of written bytes won't larger than
	 * {@link ByteBuffer#remaining() out.remaining()}. If some encoder wants to
	 * write more bytes than the output buffer's available remaining space, then
	 * <code>CoderResult.OVERFLOW</code> will be returned, and this method
	 * must be called again with a byte buffer that has free space. Otherwise
	 * this method will return <code>CoderResult.UNDERFLOW</code>, which
	 * means one encoding process has been completed successfully.
	 * <p>
	 * During the flush, the output buffer's position will be changed
	 * accordingly, while its mark and limit will be intact.
	 *
	 * @param out
	 *            the given output buffer.
	 * @return <code>CoderResult.UNDERFLOW</code> or
	 *         <code>CoderResult.OVERFLOW</code>.
	 * @throws IllegalStateException
	 *             if this encoder isn't already flushed or at end of input.
	 */
	public final CoderResult flush(ByteBuffer out) {
		if (state != FLUSHED && state != END_OF_INPUT) {
			throw illegalStateException();
		}
		CoderResult result = implFlush(out);
		if (result == CoderResult.UNDERFLOW) {
			state = FLUSHED;
		}
		return result;
	}

	/**
	 * Flushes this encoder. The default implementation does nothing and always
	 * returns <code>CoderResult.UNDERFLOW</code>; this method can be
	 * overridden if needed.
	 *
	 * @param out
	 *            the output buffer.
	 * @return <code>CoderResult.UNDERFLOW</code> or
	 *         <code>CoderResult.OVERFLOW</code>.
	 */
	protected CoderResult implFlush(ByteBuffer out) {
		return CoderResult.UNDERFLOW;
	}

	/**
	 * Notifies that this encoder's <code>CodingErrorAction</code> specified
	 * for malformed input error has been changed. The default implementation
	 * does nothing; this method can be overridden if needed.
	 *
	 * @param newAction
	 *            the new action.
	 */
	protected void implOnMalformedInput(CodingErrorAction newAction) {
		// default implementation is empty
	}

	/**
	 * Notifies that this encoder's <code>CodingErrorAction</code> specified
	 * for unmappable character error has been changed. The default
	 * implementation does nothing; this method can be overridden if needed.
	 *
	 * @param newAction
	 *            the new action.
	 */
	protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
		// default implementation is empty
	}

	/**
	 * Notifies that this encoder's replacement has been changed. The default
	 * implementation does nothing; this method can be overridden if needed.
	 *
	 * @param newReplacement
	 *            the new replacement string.
	 */
	protected void implReplaceWith(byte[] newReplacement) {
		// default implementation is empty
	}

	/**
	 * Resets this encoder's charset related state. The default implementation
	 * does nothing; this method can be overridden if needed.
	 */
	protected void implReset() {
		// default implementation is empty
	}

	/**
	 * Tests whether the given argument is legal as this encoder's replacement byte
	 * array. The given byte array is legal if and only if it can be decoded into
	 * characters.
	 */
	public boolean isLegalReplacement(byte[] replacement) {
		if (decoder == null) {
			decoder = charset.newDecoder();
			decoder.onMalformedInput(CodingErrorAction.REPORT);
			decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		}
		ByteBuffer in = ByteBuffer.wrap(replacement);
		CharBuffer out = CharBuffer.allocate((int) (replacement.length * decoder.maxCharsPerByte()));
		CoderResult result = decoder.decode(in, out, true);
		return !result.isError();
	}

	/**
	 * Returns this encoder's <code>CodingErrorAction</code> when a malformed
	 * input error occurred during the encoding process.
	 */
	public CodingErrorAction malformedInputAction() {
		return malformedInputAction;
	}

	/**
	 * Returns the maximum number of bytes which can be created by this encoder for
	 * one input character, must be positive.
	 */
	public final float maxBytesPerChar() {
		return maxBytesPerChar;
	}

	/**
	 * Sets this encoder's action on malformed input error.
	 *
	 * This method will call the
	 * {@link #implOnMalformedInput(CodingErrorAction) implOnMalformedInput}
	 * method with the given new action as argument.
	 *
	 * @param newAction
	 *            the new action on malformed input error.
	 * @return this encoder.
	 * @throws IllegalArgumentException
	 *             if the given newAction is null.
	 */
	public final CharsetEncoder onMalformedInput(CodingErrorAction newAction) {
		if (newAction == null) {
			throw new IllegalArgumentException("newAction == null");
		}
		malformedInputAction = newAction;
		implOnMalformedInput(newAction);
		return this;
	}

	/**
	 * Sets this encoder's action on unmappable character error.
	 *
	 * This method will call the
	 * {@link #implOnUnmappableCharacter(CodingErrorAction) implOnUnmappableCharacter}
	 * method with the given new action as argument.
	 *
	 * @param newAction
	 *            the new action on unmappable character error.
	 * @return this encoder.
	 * @throws IllegalArgumentException
	 *             if the given newAction is null.
	 */
	public final CharsetEncoder onUnmappableCharacter(CodingErrorAction newAction) {
		if (newAction == null) {
			throw new IllegalArgumentException("newAction == null");
		}
		unmappableCharacterAction = newAction;
		implOnUnmappableCharacter(newAction);
		return this;
	}

	/**
	 * Returns the replacement byte array, which is never null or empty.
	 */
	public final byte[] replacement() {
		return replacementBytes;
	}

	/**
	 * Sets the new replacement value.
	 *
	 * This method first checks the given replacement's validity, then changes
	 * the replacement value and finally calls the
	 * {@link #implReplaceWith(byte[]) implReplaceWith} method with the given
	 * new replacement as argument.
	 *
	 * @param replacement
	 *            the replacement byte array, cannot be null or empty, its
	 *            length cannot be larger than <code>maxBytesPerChar</code>,
	 *            and it must be legal replacement, which can be justified by
	 *            calling <code>isLegalReplacement(byte[] replacement)</code>.
	 * @return this encoder.
	 * @throws IllegalArgumentException
	 *             if the given replacement cannot satisfy the requirement
	 *             mentioned above.
	 */
	public final CharsetEncoder replaceWith(byte[] replacement) {
		if (replacement == null) {
			throw new IllegalArgumentException("replacement == null");
		}
		if (replacement.length == 0) {
			throw new IllegalArgumentException("replacement.length == 0");
		}
		if (replacement.length > maxBytesPerChar()) {
			throw new IllegalArgumentException("replacement.length > maxBytesPerChar: " +
				replacement.length + " > " + maxBytesPerChar());
		}
		if (!isLegalReplacement(replacement)) {
			throw new IllegalArgumentException("Bad replacement: " + Arrays.toString(replacement));
		}
		// It seems like a bug, but the RI doesn't clone, and we have tests that check we don't.
		this.replacementBytes = replacement;
		implReplaceWith(replacementBytes);
		return this;
	}

	/**
	 * Resets this encoder. This method will reset the internal state and then
	 * calls {@link #implReset} to reset any state related to the
	 * specific charset.
	 */
	public final CharsetEncoder reset() {
		state = RESET;
		implReset();
		return this;
	}

	/**
	 * Returns this encoder's <code>CodingErrorAction</code> when unmappable
	 * character occurred during encoding process.
	 */
	public CodingErrorAction unmappableCharacterAction() {
		return unmappableCharacterAction;
	}

	private IllegalStateException illegalStateException() {
		throw new IllegalStateException("State: " + state);
	}
}