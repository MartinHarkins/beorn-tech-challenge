package com.beorntech

import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.ScriptableObject

typealias Scoped<T> = (i: T) -> Unit

/**
 * Interprets javascript using mozilla Rhino's engine
 *
 * @param withJavaContext a hash map of java objects and they're corresponding name in the JS
 */
class RhinoJSInterpreter(withJavaContext: HashMap<String, Any>?, continuence: Scoped<JSInterpreter>?)
    :                     JSInterpreter {
    constructor(continuence: Scoped<JSInterpreter>) : this(null, continuence)
    constructor(withJavaContext: HashMap<String, Any>) : this(withJavaContext, null)
    constructor() : this(null, null)

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
                configure(ctx, withJavaContext)
                continuence.invoke(this);
            } finally {
                Context.exit()
            }
        } else {
            configure(ctx, withJavaContext)
        }
    }

    fun getScope(): ScriptableObject {
        return scope;
    }

    fun getContext(): Context {
        return ctx;
    }

    private fun configure(ctx: Context, withJavaContext: HashMap<String, Any>?) {
        ctx.languageVersion = Context.VERSION_ES6
        scope = ctx.initStandardObjects()

        // inject java obj into scope
        withJavaContext?.forEach { name, obj ->
            val wrappedOut = Context.javaToJS(obj, scope)
            ScriptableObject.putProperty(scope, name, wrappedOut)
        }
    }

    override fun close() {
        Context.exit()
    }

    override fun createLocalScope(scriptContents: String): JSInterpreter {
        TODO("Not yet implemented")
        // TODO: clone this with a new scope.
    }

    override fun eval(scriptContents: String) {
        this.ctx.evaluateString(this.scope, scriptContents, "scriptnum" + (scriptCount++), 1, null)
    }

    override fun evalAsString(expression: String): String {
        val evaluatedObj = ctx.evaluateString(scope, expression, "expression" + (expressionCount++), 1, null)

        // Todo: handle more types
        if (evaluatedObj is NativeJavaObject) {
            return evaluatedObj.unwrap() as String
        } else if (evaluatedObj is Double) {
            return evaluatedObj.toInt().toString()
        }
        println("could not evaluate expression for $expression. evaluatedObj was $evaluatedObj")
        return expression
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
}