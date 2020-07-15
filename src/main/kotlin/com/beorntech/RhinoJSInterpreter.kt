package com.beorntech

import org.mozilla.javascript.*
import java.text.ParseException

typealias Scoped<T> = (i: T) -> Unit

/**
 * Interprets javascript using mozilla Rhino's engine
 *
 * @param withJavaContext a hash map of java objects and they're corresponding name in the JS
 */
class RhinoJSInterpreter(continuence: Scoped<JSInterpreter>?, var parent: RhinoJSInterpreter?)
    :                     JSInterpreter {
    constructor() : this(null, null)
    constructor(parent: RhinoJSInterpreter): this(null, parent)
    constructor(continuence: Scoped<JSInterpreter>) : this(continuence, null)

    var scriptCount = 0
    var expressionCount = 0

    private var ctx: Context;
    private lateinit var scope: ScriptableObject;

    // TODO: use a builder pattern to return the RhinoJSInterpreter
    init {
        ctx = Context.enter()

        // Optionally use a continuance pattern in which case we close the context for the consumer.
        if (continuence != null) {
            try {
                configure(ctx)
                continuence.invoke(this);
            } finally {
                Context.exit()
            }
        } else {
            configure(ctx)
        }
    }

    fun getScope(): ScriptableObject {
        return scope;
    }

    fun getContext(): Context {
        return ctx;
    }

    private fun configure(ctx: Context) {
        ctx.languageVersion = Context.VERSION_ES6

        // Allow for parent scopes
        if (parent != null) {
            val scriptable = ctx.initStandardObjects(parent!!.getScope())
            if (scriptable is ScriptableObject) {
                scope = scriptable;
            }
        } else {
            scope = ctx.initStandardObjects()
        }
    }

    override fun close() {
        Context.exit()
    }

    override fun createLocalScope(): JSInterpreter {
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