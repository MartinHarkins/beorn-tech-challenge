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
fun parseHtml(htmlContent: String, vararg injectables: Pair<String, Any>?): String {
    val doc: Document = Jsoup.parse(htmlContent)


    RhinoJSInterpreter() { interpreter ->
        walkElement(doc, interpreter.inject(*injectables))
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
            val attributes = ArrayList<Attribute>(element.attributes().asList());
            attributes.forEach { attr ->
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
                        val arr = jsInterpreter.evalAsArray(attr.value);

                        element.removeAttr(attr.key);

                        arr.forEach { obj ->
                            val localScopeInterpreter = jsInterpreter
                                    .createLocalScope()
                                    .inject(Pair(varName.toString(), obj))
                            walkElement(
                                    element.clone().appendTo(element.parent()), localScopeInterpreter)
                        }

                        // don't forget to remove the initial element. Or should we try to reuse it?
                        // not a very big perf gain in any case.
                        element.remove()
                        return;
                    }
                    else -> {
                        parseAttribute(attr, jsInterpreter)
                    }
                }
            }

            if (element.childrenSize() > 0) {
                val children = ArrayList<Element>(element.children())
                children.forEach { child -> walkElement(child, jsInterpreter) }
            } else {
                // element.text() will return any text contained within the subtree of element.
                // that means we should already have filtered down to elements not containing child elements.

                val text = element.text().replace(allTemplatingExpressions) { matchResult ->
                    val matchGroup = matchResult.groups[1]
                    if (matchGroup != null) {
                        jsInterpreter.evalAsString(matchGroup.value)
                    } else
                        ""
                }
                element.text(text)
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
val allTemplatingExpressions = Regex("\\$\\{([^}]+)}")

/**
 * remove ${ and } from around the expression
 */
fun stripTemplating(str: String): String {
    return str.subSequence(2, str.length - 1).toString();
}
