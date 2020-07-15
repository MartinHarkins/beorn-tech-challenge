package com.beorntech

typealias getLocalScope<T> = () -> T

interface JSInterpreter {
    fun eval(scriptContents: String)
    fun evalAsString(expression: String): String
    fun close()
    /* Returns a new interpreter including the contents of scriptContents */
    fun createLocalScope(scriptContents: String): JSInterpreter
    fun evalAsBoolean(expression: String): Boolean
}