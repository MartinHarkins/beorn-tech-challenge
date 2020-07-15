package com.beorntech

import java.text.ParseException

interface JSInterpreter {
    fun eval(scriptContents: String)
    fun evalAsString(expression: String): String
    fun close()
    /* Returns a new interpreter including the contents of scriptContents*/
    fun evalAsBoolean(expression: String): Boolean

    @Throws(ParseException::class)
    fun evalAsArray(expression: String): Array<Any>

    fun inject(vararg pairs: Pair<String, Any>?): JSInterpreter
    fun createLocalScope(): JSInterpreter
}