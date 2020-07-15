package com.beorntech

import org.mozilla.javascript.*
import java.text.ParseException

typealias Scoped<T> = (i: T) -> Unit

/**
 * Interprets javascript using mozilla Rhino's engine
 *
 * @param withJavaContext a hash map of java objects and they're corresponding name in the JS
 */
class RhinoJSInterpreter(private val parent: RhinoJSInterpreter?, continuance: Scoped<JSInterpreter>?)
    :                     JSInterpreter {
    constructor() : this(null, null)
    constructor(parent: RhinoJSInterpreter): this(parent, null)
    constructor(continuance: Scoped<JSInterpreter>) : this(null, continuance)

    private var scriptCount = 0 // used for source tagging
    private var expressionCount = 0 // used for source tagging

    private var ctx: Context

    lateinit var scope: ScriptableObject
        private set

    init {
        ctx = Context.enter()

        // Optionally use a continuance pattern in which case we close the context for the consumer.
        if (continuance != null) {
            try {
                configure(ctx)
                continuance.invoke(this);
            } finally {
                Context.exit()
            }
        } else {
            configure(ctx)
        }
    }

    /**
     * Configure the current context and build the scope.
     */
    private fun configure(ctx: Context) {
        ctx.languageVersion = Context.VERSION_ES6

        if (parent == null) {
            scope = ctx.initStandardObjects()
        }
        else {
            val scriptable = ctx.initStandardObjects(parent.scope)
            if (scriptable is ScriptableObject) {
                scope = scriptable;
            }
        }
    }

    override fun close() {
        Context.exit()
    }

    override fun createChild(): JSInterpreter {
        return RhinoJSInterpreter(this)
    }

    override fun eval(scriptContents: String) {
        this.ctx.evaluateString(this.scope, scriptContents, "scriptnum" + (scriptCount++), 1, null)
    }

    override fun evalAsString(expression: String): String {
        val evaluatedObj = ctx.evaluateString(scope, expression, "expression" + (expressionCount++), 1, null)

        // Todo: handle more types
        when (evaluatedObj) {
            is String -> {
                return evaluatedObj
            }
            is NativeJavaObject -> {
                return evaluatedObj.unwrap() as String
            }
            is Double -> {
                return evaluatedObj.toInt().toString()
            }
            else -> {
                println("could not evaluate expression for $expression. evaluatedObj was $evaluatedObj")
                return expression
            }
        }
    }


    override fun evalAsBoolean(expression: String): Boolean {
        val evaluatedObj = ctx.evaluateString(scope, expression, "expression" + (expressionCount++), 1, null)

        // Todo: handle more types
        if (evaluatedObj is NativeJavaObject) {
            return evaluatedObj.unwrap() as Boolean
        } else if (evaluatedObj is Boolean) {
            return evaluatedObj
        }
        println("could not evaluate expression for $expression. evaluatedObj was $evaluatedObj")
        return false
    }

    @Throws(ParseException::class)
    override fun evalAsArray(expression: String): Array<Any> {
        val evaluatedObj = ctx.evaluateString(scope, expression, "expression" + (expressionCount++), 1, null)
        if (evaluatedObj is NativeArray) {
            return evaluatedObj.toArray();
        }
        println("could not evaluate expression for $expression. evaluatedObj was $evaluatedObj")

        // hacky and basic exception throwing but this is better than swallowing the errors
        throw ParseException("could not evaluate expression for $expression. evaluatedObj was $evaluatedObj", 0)
    }

    override fun inject(vararg pairs: Pair<String, Any>?): JSInterpreter {
        pairs.forEach { pair ->
            if (pair != null) {
                val wrappedOut = Context.javaToJS(pair.second, scope)
                ScriptableObject.putProperty(scope, pair.first, wrappedOut)
            }
        }
        return this
    }
}