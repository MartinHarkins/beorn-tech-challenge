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
fun parseHtml(htmlContent: String): List<String> {
    val doc: Document = Jsoup.parse(htmlContent)

    initializeContext { context, scope ->
        parseElement(context, scope, doc);
    }

//    parseElement(doc);

    val scriptsToInterpret = doc.select("script[type=server/javascript]")
    return scriptsToInterpret.map { e -> e.data() };
}

fun parseElement(context: Context, parentScope: ScriptableObject, element: Element) {
    var scope: ScriptableObject = parentScope;
    // Could you switch/when pattern
    // Could use adapter pattern
    when (element.tagName()) {
//        "script" -> scope = parseScriptTag(element)
        else -> {
            element.attributes().forEach { attr -> parseAttribute(context, scope, attr) }
            if (shouldEvaluate(element.text())) {
                read(context, scope, element.text())
            }
            element.children().forEach { child -> parseElement(context, scope, child) }
        }
    }
}

fun parseAttribute(context: Context, parentScope: ScriptableObject, attribute: Attribute) {
    if (shouldEvaluate(attribute.value)) {
        read(context, parentScope, attribute.value)
    }
}

//val expression = Regex("""^\$\{((\w|\d|\.|\(|\))+)}\$""")
// handle basic expressions without going too overboard.
val expression = Regex("^\\$\\{\\w(\\w|\\d|\\.|\\(|\\))*}$")
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
    println("expression $expression")
    val evaluatedObj = context.evaluateString(scope, expression, "evalnum" + (evalCount++), 1, null);

    if (evaluatedObj is NativeJavaObject) {
        return evaluatedObj.unwrap() as String
    }
    println("could not evaluate expression for $string")
    return expression;
}

fun parseScriptTag(element: Element) {
    if (!"server/javascript".equals(element.attr("type"))) {
        // do nothing
        return;
    }
    val script = element.data();

    // todo what?
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
