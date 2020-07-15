package com.beorntech

import java.text.ParseException

interface JSInterpreter {
    /**
     * Evaluates a script
     */
    fun eval(scriptContents: String)

    /**
     * Evaluates an expression as a String
     */
    fun evalAsString(expression: String): String

    /* Evaluates an expression as a Boolean */
    fun evalAsBoolean(expression: String): Boolean

    /**
     * Evaluates an expression as an Array
     */
    @Throws(ParseException::class)
    fun evalAsArray(expression: String): Array<Any>

    /**
     * Inject the interpreter with Java Objects
     */
    fun inject(vararg pairs: Pair<String, Any>?): JSInterpreter

    /**
     * Create a child interpreter inheriting from this scope's variables
     */
    fun createChild(): JSInterpreter

    /**
     * Close the Context
     */
    fun close()
}