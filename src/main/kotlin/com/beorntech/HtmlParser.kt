package com.beorntech

import org.jsoup.Jsoup
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@Throws(IllegalArgumentException::class)
fun parseHtml(htmlContent: String): String {
    return parseHtml(htmlContent, null)
}

@Throws(IllegalArgumentException::class)
fun parseHtml(htmlContent: String, withJavaObjects: HashMap<String, Any>?): String {
    val doc: Document = Jsoup.parse(htmlContent)


    RhinoJSInterpreter(withJavaObjects) { interpreter ->
        walkElement(doc, interpreter)
    }

    return doc.toString()
}

/**
 *
 */
fun walkElement(element: Element, jsInterpreter: JSInterpreter) {
    // Could you switch/when pattern
    // Could use adapter pattern
    when (element.tagName()) {
        "script" -> {
            if (!"server/javascript".equals(element.attr("type"))) {
                // do nothing
                return
            }

            jsInterpreter.eval(element.html())

            // once processed, remove this script tag from the document
            element.remove()
        }
        else -> {
            element.attributes().forEach { attr ->
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
                        // todo: handle for loops
                    }
                    else -> {
                        parseAttribute(attr, jsInterpreter)
                    }
                }
            }

            if (element.childrenSize() > 0) {
                element.children().forEach { child -> walkElement(child, jsInterpreter) }
            } else {
                // we want to handle an element's text at an atomic level and so that element should not have any children.
                // element.text() will return any text contained within the subtree of element.
                val text = element.text().trim();
                if (isTemplatingExpression(text)) {
                    element.text(
                            jsInterpreter.evalAsString(stripTemplating(text)))
                }
            }
        }
    }
}

fun parseAttribute(attribute: Attribute, jsInterpreter: JSInterpreter) {
    if (isTemplatingExpression(attribute.value)) {
        attribute.setValue(jsInterpreter.evalAsString(stripTemplating(attribute.value)))
    }
}

// handle basic expressions without going too overboard.
val expression = Regex("^\\$\\{(\\w|\\d|\\.| |\\+|\\(|\\))+}$")
fun isTemplatingExpression(string: String): Boolean {
    // TODO: extract the expression
    return expression.matches(string)
}

/**
 * remove ${ and } from around the expression
 */
fun stripTemplating(str: String): String {
    return str.subSequence(2, str.length - 1).toString();
}
