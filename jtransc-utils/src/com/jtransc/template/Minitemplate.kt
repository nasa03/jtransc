package com.jtransc.template

import com.jtransc.ds.ListReader
import com.jtransc.error.invalidOp
import com.jtransc.error.noImpl
import com.jtransc.lang.Dynamic
import com.jtransc.text.StrReader
import com.jtransc.text.isLetterDigitOrUnderscore
import com.jtransc.text.readWhile

class Minitemplate(val template: String) {
	val templateTokens = Token.tokenize(template)
	val node = BlockNode.parse(templateTokens)

	class Scope(val map: Any?, val parent: Scope? = null) {
		operator fun get(key: Any?): Any? {
			return Dynamic.accessAny(map, key) ?: parent?.get(key)
		}

		operator fun set(key: Any?, value: Any?) {
			Dynamic.setAny(map, key, value)
		}
	}

	operator fun invoke(args: Any?): String {
		val context = Context(Scope(args))
		context.createScope { node.eval(context) }
		return context.str.toString()
	}

	class Context(var scope: Scope) {
		var str = StringBuilder()

		fun write(text: String) = this.apply { str.append(text) }
		inline fun createScope(callback: () -> Unit) = this.apply {
			val old = this.scope
			this.scope = Scope(hashMapOf<Any?, Any?>(), old)
			callback()
			this.scope = old
		}
	}

	interface ExprNode {
		fun eval(context: Context): Any?

		data class VAR(val name: String) : ExprNode {
			override fun eval(context: Context): Any? = context.scope[name]
		}

		data class LIT(val value: Any?) : ExprNode {
			override fun eval(context: Context): Any? = value
		}

		data class ACCESS(val expr: ExprNode, val name: ExprNode) : ExprNode {
			override fun eval(context: Context): Any? = Dynamic.accessAny(expr.eval(context), name.eval(context))
		}

		data class BINOP(val l: ExprNode, val r: ExprNode, val op: String) : ExprNode {
			override fun eval(context: Context): Any? = Dynamic.binop(l.eval(context), r.eval(context), op)
		}

		data class UNOP(val r: ExprNode, val op: String) : ExprNode {
			override fun eval(context: Context): Any? = Dynamic.unop(r.eval(context), op)
		}

		companion object {
			fun parse(str: String): ExprNode {
				val tokens = Token.tokenize(str)
				val result = parse(ListReader(tokens))
				return result
			}

			fun parse(r: ListReader<Token>): ExprNode {
				return parseBinop(r)
			}

			private val BINOPS = setOf(
				"+", "-", "*", "/", "%",
				"&&", "||"
			)

			fun parseBinop(r: ListReader<Token>): ExprNode {
				var result = parseFinal(r)
				while (r.hasMore) {
					if (r.peek() !is Token.TOperator || r.peek().text !in BINOPS) break
					val operator = r.read().text
					var right = parseFinal(r)
					result = ExprNode.BINOP(result, right, operator)
				}
				// @TODO: Fix order!
				return result
			}

			fun parseFinal(r: ListReader<Token>): ExprNode {
				when (r.peek().text) {
					"!", "~", "-", "+" -> {
						val op = r.read().text
						return ExprNode.UNOP(parseFinal(r), op)
					}
					"(" -> {
						r.read()
						val result = parse(r)
						if (r.read().text != ")") throw RuntimeException("Expected ')'")
						return result
					}
				}
				if (r.peek() is Token.TNumber) {
					return ExprNode.LIT(r.read().text.toDouble())
				}

				var construct: ExprNode = ExprNode.VAR(r.read().text)
				loop@while (r.hasMore) {
					when (r.peek().text) {
						"." -> {
							r.read()
							val id = r.read().text
							construct = ExprNode.ACCESS(construct, ExprNode.LIT(id))
							continue@loop
						}
						"[" -> {
							r.read()
							val expr = parse(r)
							construct = ExprNode.ACCESS(construct, expr)
							if (r.read().text != "]") throw RuntimeException("Expected ']'")
						}
						"(" -> {
							noImpl("Not implemented function calls!")
						}
						else -> break@loop
					}
				}
				return construct
			}
		}

		interface Token {
			val text: String

			data class TId(override val text: String) : Token
			data class TNumber(override val text: String) : Token
			data class TString(override val text: String) : Token
			data class TOperator(override val text: String) : Token
			data class TEnd(override val text: String = "") : Token

			companion object {
				private val OPERATORS = setOf(
					"(", ")",
					"[", "]",
					"{", "}",
					"&&", "||",
					"&", "^",
					"+", "-", "*", "/", "%", "**",
					"!", "~",
					"."
				)

				fun tokenize(str: String): List<Token> {
					val r = StrReader(str)
					val out = arrayListOf<Token>()
					fun emit(str: Token) {
						out += str
					}
					while (r.hasMore) {
						val start = r.offset
						r.skipSpaces()
						val id = r.readWhile { it.isLetterDigitOrUnderscore() }
						if (id != null) {
							if (id[0].isDigit()) emit(TNumber(id)) else emit(TId(id))
						}
						r.skipSpaces()
						if (r.peek(3) in OPERATORS) emit(TOperator(r.read(3)))
						if (r.peek(2) in OPERATORS) emit(TOperator(r.read(2)))
						if (r.peek(1) in OPERATORS) emit(TOperator(r.read(1)))
						val end = r.offset
						if (end == start) invalidOp("Don't know how to handle '${r.peekch()}'")
					}
					emit(TEnd())
					return out
				}
			}
		}
	}

	interface BlockNode {
		fun eval(context: Context): BlockNode

		/*
			is BlockNode.FOR -> {
			}
			is BlockNode.FOR -> {

			}
			else -> noImpl("Not implemented $it")

		 */

		data class GROUP(val children: List<BlockNode>) : BlockNode {
			override fun eval(context: Context) = this.apply { for (n in children) n.eval(context) }
		}

		data class TEXT(val content: String) : BlockNode {
			override fun eval(context: Context) = this.apply { context.write(content) }
		}

		data class EXPR(val expr: ExprNode) : BlockNode {
			override fun eval(context: Context) = this.apply { context.write(this.expr.eval(context).toString()) }
		}

		data class IF(val cond: ExprNode, val trueContent: BlockNode, val falseContent: BlockNode?) : BlockNode {
			override fun eval(context: Context) = this.apply {
				if (Dynamic.toBool(cond.eval(context))) {
					trueContent.eval(context)
				} else {
					falseContent?.eval(context)
				}
			}
		}

		data class FOR(val varname: String, val expr: ExprNode, val loop: BlockNode) : BlockNode {
			override fun eval(context: Context) = this.apply {
				context.createScope {
					for (v in Dynamic.toIterable(expr.eval(context))) {
						context.scope[varname] = v
						loop.eval(context)
					}
				}
			}
		}

		companion object {
			fun group(children: List<BlockNode>): BlockNode = if (children.size == 1) children[0] else GROUP(children.toList())

			fun parse(tokens: List<Token>): BlockNode {
				val tr = ListReader(tokens)
				fun handle(tag: Tag, token: Token.TTag): BlockNode {
					val parts = arrayListOf<TagPart>()
					var currentToken = token
					val children = arrayListOf<BlockNode>()

					fun emitPart() {
						parts += TagPart(currentToken, BlockNode.group(children))
					}

					loop@while (!tr.eof) {
						val it = tr.read()
						when (it) {
							is Token.TLiteral -> children += BlockNode.TEXT(it.content)
							is Token.TExpr -> children += BlockNode.EXPR(ExprNode.parse(it.content))
							is Token.TTag -> {
								when (it.name) {
									tag.end -> break@loop
									in tag.nextList -> {
										emitPart()
										currentToken = it
										children.clear()
									}
									else -> {
										val newtag = Tag.ALL_MAP[it.name]!!
										if (tag.end != null) {
											children += handle(newtag, it)
										} else {
											children += newtag.buildNode(listOf(TagPart(it, BlockNode.TEXT(""))))
										}
									}
								}
							}
							else -> break@loop
						}
					}

					emitPart()

					return tag.buildNode(parts)
				}
				return handle(Tag.EMPTY, Token.TTag("", ""))
			}
		}
	}

	data class TagPart(val token: Token.TTag, val body: BlockNode)

	data class Tag(val name: String, val nextList: Set<String>, val end: String?, val buildNode: (parts: List<TagPart>) -> BlockNode) {
		companion object {
			val EMPTY = Tag("", setOf(""), "") { parts ->
				BlockNode.group(parts.map { it.body })
			}
			val IF = Tag("if", setOf("else"), "end") { parts ->
				val main = parts[0]
				val elseBlock = parts.getOrNull(1)
				BlockNode.IF(ExprNode.parse(main.token.content), main.body, elseBlock?.body)
			}
			val FOR = Tag("for", setOf(), "end") { parts ->
				val main = parts[0]
				val parts2 = main.token.content.split("in", limit = 2).map { it.trim() }
				BlockNode.FOR(parts2[0], ExprNode.parse(parts2[1]), main.body)
			}
			val SET = Tag("set", setOf(), null) {
				noImpl
			}

			val ALL = listOf(EMPTY, IF, FOR, SET)
			val ALL_MAP = ALL.associateBy { it.name }
		}
	}

	interface Token {
		data class TLiteral(val content: String) : Token
		data class TExpr(val content: String) : Token
		data class TTag(val name: String, val content: String) : Token

		companion object {
			private val TOKENS = Regex("(\\{[%\\{])(.*?)[%\\}]\\}")
			fun tokenize(str: String): List<Token> {
				val out = arrayListOf<Token>()
				var lastPos = 0

				fun emit(token: Token) {
					if (token is TLiteral && token.content.isEmpty()) return
					out += token
				}

				for (tok in TOKENS.findAll(str)) {
					emit(TLiteral(str.substring(lastPos until tok.range.start)))
					val content = str.substring(tok.groups[2]!!.range).trim()
					if (tok.groups[1]?.value == "{{") {
						emit(TExpr(content))
					} else {
						val parts = content.split(' ', limit = 2)
						emit(TTag(parts[0], parts.getOrElse(1) { "" }))
					}
					lastPos = tok.range.endInclusive + 1
				}
				emit(TLiteral(str.substring(lastPos, str.length)))
				return out
			}
		}
	}
}
