package com.beorntech

import org.jsoup.Jsoup
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.ScriptableObject
import javax.script.ScriptException

class HtmlParser(htmlContent: String) {
    val ctx: Context
    val doc: Document;

    init {
        doc = Jsoup.parse(htmlContent)
        ctx = Context.enter()
    }
}

@Throws(IllegalArgumentException::class)
fun parseHtml(htmlContent: String): String {
    return parseHtml(htmlContent, null)
}
@Throws(IllegalArgumentException::class)
fun parseHtml(htmlContent: String, withJavaObjects: HashMap<String, Any>?): String {
    val doc: Document = Jsoup.parse(htmlContent)


    initializeContext { context, scope ->

        // inject java obj into scope
        withJavaObjects?.forEach { name, obj ->
            val wrappedOut = Context.javaToJS(obj, scope);
            ScriptableObject.putProperty(scope, name, wrappedOut);
        }

        parseElement(context, scope, doc);
    }

    return doc.toString()
}

fun parseElement(context: Context, parentScope: ScriptableObject, element: Element) {
    val scope: ScriptableObject = parentScope;
    // Could you switch/when pattern
    // Could use adapter pattern
    when (element.tagName()) {
        "script" -> parseScriptTag(context, scope, element)
        else -> {
            element.attributes().forEach { attr -> parseAttribute(context, scope, attr) }
            element.children().forEach { child -> parseElement(context, scope, child) }

            // element.text() will return any text contained within the subtree of element.
            // we want to handle an element's text at an atomic level and so that element should not have any children.
            if (element.childrenSize() == 0 && shouldEvaluate(element.text())) {
                element.text(read(context, scope, element.text()))
            }
        }
    }
}

fun parseAttribute(context: Context, parentScope: ScriptableObject, attribute: Attribute) {
    if (shouldEvaluate(attribute.value)) {
        attribute.setValue(read(context, parentScope, attribute.value))
    }
}

// handle basic expressions without going too overboard.
val expression = Regex("^\\$\\{(\\w|\\d|\\.| |\\+|\\(|\\))+}$")
fun shouldEvaluate(string: String): Boolean {
    // TODO: extract the expression
    return expression.matches(string);
}

// Just using a simple counter to name source evaluations.
// No known use for name or count !yet!
var evalCount = 0;

/**
 * @return the resulting expression result or if it fails, the expression itself.
 */
fun read(context: Context, scope: ScriptableObject, string: String): String {
    // simple strip of the leading ${ and trailing }
    val expression = string.subSequence(2, string.length - 1).toString();
    val evaluatedObj = context.evaluateString(scope, expression, "evalnum" + (evalCount++), 1, null);

    if (evaluatedObj is NativeJavaObject) {
        return evaluatedObj.unwrap() as String
    } else if (evaluatedObj is Double) {
        return evaluatedObj.toInt().toString()
    }
    println("could not evaluate expression for $string. evaluatedObj was $evaluatedObj")
    return expression;
}

var scriptCount = 0;
fun parseScriptTag(context: Context, scope: ScriptableObject, element: Element) {
    if (!"server/javascript".equals(element.attr("type"))) {
        // do nothing
        return;
    }

    context.evaluateString(scope, element.html(), "scriptnum" + (scriptCount++), 1, null);

    // once processed, remove this script tag from the document
    element.remove();
}

typealias RunWithScope = (ctx: Context, scope: ScriptableObject) -> Unit;

typealias ContinueWithPair<A, B> = (first: A, second: B) -> Unit;

fun initializeContext(continuence: ContinueWithPair<Context, ScriptableObject>?) {
    val ctx = Context.enter()
    try {
        ctx.languageVersion = Context.VERSION_ES6;
        val scope = ctx.initStandardObjects();

        continuence?.invoke(ctx, scope);
    } finally {
        Context.exit();
    }
}

@Throws(ScriptException::class)
fun parseScript(jsSource: String, jsSourceName: String, withJavaObjects: HashMap<String, Any>?, runWithScope: RunWithScope?) {
    initializeContext { ctx, scope ->
        // inject java obj into scope
        withJavaObjects?.forEach { name, obj ->
            val wrappedOut = Context.javaToJS(obj, scope);
            ScriptableObject.putProperty(scope, name, wrappedOut);
        }

        ctx.evaluateString(scope, jsSource, jsSourceName, 1, null);

        //
        runWithScope?.invoke(ctx, scope);

//        val obj: Scriptable = scope.get("obj", scope) as Scriptable;
//
//        // Should print "obj == result" (Since the result of an assignment
//        // expression is the value that was assigned)
//
//        // Should print "obj == result" (Since the result of an assignment
//        // expression is the value that was assigned)
//        println("obj " + (if (obj === result) "==" else "!=") +
//                " result")
//
//        // Should print "obj.a == 1"
//
//        // Should print "obj.a == 1"
//        System.out.println("obj.a == " + obj.get("a", obj))

    }
}
