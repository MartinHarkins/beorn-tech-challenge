package com.beorntech

import org.jsoup.Jsoup
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*
import kotlin.collections.ArrayList

/**
 * Parse HTML document and evaluate JS code
 * <p>
 *  The entry point to the parser, uses JSoup under the hood to walk and manipulate the DOM
 * </p>
 *
 * @param jsInterpreter the interpreter used to evaluate the embedded javascript code
 * @param htmlContent the html document to parse
 * @return the parsed html document
 */
fun parseHtml(jsInterpreter: JSInterpreter, htmlContent: String): String {
    val doc: Document = Jsoup.parse(htmlContent)

    parseElement(jsInterpreter, doc)

    return doc.toString()
}

// handle basic expressions without going too overboard.
// todo: clean this up
private val expression: Regex = Regex("^\\$\\{(\\w|\\d|\\.| |\\+|\\(|\\))+}$")
private val allTemplatingExpressions = Regex("\\$\\{([^}]+)}")

/**
 *  Parse an element using the specified JSInterpreter
 */
private fun parseElement(jsInterpreter: JSInterpreter, element: Element) {
    // Could you switch/when pattern
    // Could use adapter pattern

    // 1. handle script tags
    // 2. handle attributes -> attributes will affect the element itself and need to be parsed first.
    // 3. handle the element subtree

    if ("script".equals(element.tagName())) {
        if ("server/javascript" != element.attr("type")) {
            // let unrecognized script tags types through
            return
        }

        jsInterpreter.eval(element.html())

        // once processed, remove this script tag from the document
        element.remove()
        return
    }

    val attributes = Collections.unmodifiableList(element.attributes().asList())
    // hacky way to up and quit if the processing of an attribute should stop.
    if (attributes.any { attr -> parseAttribute(jsInterpreter, element, attr) }) return

    // element.text() will return any text contained within the subtree of element.
    // that means we should already have filtered down to elements not containing child elements.
    if (element.childrenSize() > 0) {
        // unsure about element list safety, so copying the array
        val children = Collections.unmodifiableList(element.children())
        children.forEach { child -> parseElement(jsInterpreter, child) }
    } else {
        val text = element.text().replace(allTemplatingExpressions, evaluateMatch(jsInterpreter))
        element.text(text)
    }
}

private fun evaluateMatch(jsInterpreter: JSInterpreter): (MatchResult) -> CharSequence {
    return { matchResult ->
        val matchGroup = matchResult.groups[1]
        if (matchGroup != null) {
            jsInterpreter.evalAsString(matchGroup.value)
        } else
            ""
    }
}

/**
 * Parses an Attribute of an Element
 *
 * @returns true if the element was removed from the DOM tree (pretty bad impl I know)
 */
private fun parseAttribute(jsInterpreter: JSInterpreter, element: Element, attr: Attribute): Boolean {
    when {
        "data-if" == attr.key -> {
            // if the expression evaluates as true, only remove the attribute
            // otherwise, remove the element from the dom tree
            if (jsInterpreter.evalAsBoolean(attr.value))
                element.removeAttr(attr.key)
            else
                element.remove()
        }
        attr.key.startsWith("data-for-") -> {
            val varName = attr.key.subSequence("data-for-".length, attr.key.length)
            val arr = jsInterpreter.evalAsArray(attr.value)

            element.removeAttr(attr.key)

            arr.forEach { obj ->
                val localScopeInterpreter = jsInterpreter
                        .createChild()
                        .inject(Pair(varName.toString(), obj))
                parseElement(
                        localScopeInterpreter, element.clone().appendTo(element.parent()))
            }

            // don't forget to remove the initial element. Or should we try to reuse it?
            // not a very big perf gain in any case.
            element.remove()
            return true
        }
        else -> {
            if (isTemplatingExpression(attr.value)) {
                attr.setValue(jsInterpreter.evalAsString(stripTemplating(attr.value)))
            }
        }
    }
    return false
}

fun isTemplatingExpression(string: String): Boolean {
    // TODO: extract the expression
    return expression.matches(string)
}


/**
 * remove ${ and } from around the expression
 */
fun stripTemplating(str: String): String {
    return str.subSequence(2, str.length - 1).toString()
}
