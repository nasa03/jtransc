package com.jtransc.io;

import com.jtransc.JTranscSystem;
import com.jtransc.annotation.*;


import java.util.Objects;

public class JTranscConsole {
	static public void log(char[] v) {
		logString(new String(v));
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.istr(p0));")
	@JTranscMethodBody(target = "cpp", value = "N::log(p0 ? p0->{% METHOD java.lang.Object:toString %}() : N::str(std::wstring(L\"null\")));")
	@JTranscMethodBody(target = "cs", value = "Console.WriteLine((p0 != null) ? p0.ToString() : \"null\");")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void logString(String v) {
		JTranscSystem.checkInJVM("logObject");
		System.out.println(v);
	}

	static public void log(Object v) {
		logString(Objects.toString(v));
	}

	@JTranscMethodBody(target = "js", value = "console.log(p0);")
	@JTranscMethodBody(target = "cpp", value = "N::log(p0 ? p0->{% METHOD java.lang.Object:toString %}() : N::str(std::wstring(L\"null\")));")
	@JTranscMethodBody(target = "cs", value = "Console.WriteLine((p0 != null) ? p0.ToString() : \"null\");")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void dump(Object v) {
		log(v);
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.str(p0));")
	@JTranscMethodBody(target = "cpp", value = "N::log(p0 ? L\"true\" : L\"false\");")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void log(boolean v) {
		log(Boolean.toString(v));
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.str(p0));")
	@JTranscMethodBody(target = "cpp", value = "wprintf(L\"%d\\n\", (int32_t)p0); fflush(stdout);")
	@JTranscMethodBody(target = "cs", value = "Console.WriteLine(p0);")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void log(byte v) {
		log(Byte.toString(v));
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.str(p0));")
	@JTranscMethodBody(target = "cpp", value = "wprintf(L\"%d\\n\", (int32_t)p0); fflush(stdout);")
	@JTranscMethodBody(target = "cs", value = "Console.WriteLine(p0);")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void log(short v) {
		log(Short.toString(v));
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.ichar(p0));")
	@JTranscMethodBody(target = "cpp", value = "wprintf(L\"%lc\\n\", (wchar_t)p0); fflush(stdout);")
	@JTranscMethodBody(target = "cs", value = "Console.WriteLine((char)p0);")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void log(char v) {
		log(Character.toString(v));
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.str(p0));")
	@JTranscMethodBody(target = "cpp", value = "wprintf(L\"%d\\n\", (int32_t)p0); fflush(stdout);")
	@JTranscMethodBody(target = "cs", value = "Console.WriteLine(p0);")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void log(int v) {
		log(Integer.toString(v));
	}

	@SuppressWarnings("PointlessBitwiseExpression")

	@JTranscMethodBody(target = "js", value = "console.log('Int64(' + N.lhigh(p0) + ',' + N.llow(p0) + ')');")
	static public void log(long v) {
		logLong((int) (v >> 32), (int) (v >> 0));
	}

	static public void logLong(int high, int low) {
		log("Int64(" + high + "," + low + ")");
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.str(p0));")
	@JTranscMethodBody(target = "cpp", value = "wprintf(L\"%f\\n\", (float32_t)p0); fflush(stdout);")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void log(float v) {
		JTranscSystem.checkInJVM("logFloat");
		System.out.println(v);
	}


	@JTranscMethodBody(target = "js", value = "console.log(N.str(p0));")
	@JTranscMethodBody(target = "cpp", value = "wprintf(L\"%llf\\n\", (float64_t)p0); fflush(stdout);")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void log(double v) {
		JTranscSystem.checkInJVM("logDouble");
		System.out.println(v);
	}

	@JTranscMethodBody(target = "js", value = "console.error(N.istr(p0));")
	@JTranscMethodBody(target = "cs", value = "Console.Error.WriteLine(p0);")
	@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void errorString(String msg) {
		JTranscSystem.checkInJVM("logError");
		System.err.println(msg);
	}

	static public void error(String msg) {
		errorString(msg);
	}

	//@JTranscMethodBody(target = "js", value = "console.error('' + p0);")
	//@JTranscMethodBody(target = "cs", value = "Console.Error.WriteLine(p0);")
	//@JTranscMethodBody(target = "dart", value = "print(p0);")
	static public void error(Object msg) {
		errorString(Objects.toString(msg));
	}

	static public void logOrError(Object msg, boolean error) {
		if (error) {
			JTranscConsole.error(msg);
		} else {
			JTranscConsole.log(msg);
		}
	}

	public static void syncPrintStackTrace(Throwable e) {
		error("JTranscConsole.printStackTrace:");
		error(e.getMessage());
	}
}

/*
new PrintStream(new OutputStream() {
		@Override

		native public void write(int b) throws IOException;
	});new PrintStream(new OutputStream() {
		@Override

		native public void write(int b) throws IOException;
	});
 */
