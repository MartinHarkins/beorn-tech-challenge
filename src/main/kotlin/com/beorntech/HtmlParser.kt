package com.beorntech

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import javax.script.ScriptException


@Throws(IllegalArgumentException::class)
fun parseHtml(htmlContent: String): List<String> {
    val doc: Document = Jsoup.parse(htmlContent)

    val scriptsToInterpret = doc.select("script[type=server/javascript]")
    val scripts = scriptsToInterpret.map { e -> e.data() }

    return scripts;
}

typealias RunWithScope = (ctx: Context, scope: ScriptableObject) -> Unit;

@Throws(ScriptException::class)
fun parseScript(jsSource: String, jsSourceName: String, withJavaObjects: HashMap<String, Any>?, runWithScope: RunWithScope?) {
    val ctx = Context.enter()
    try {
        ctx.languageVersion = Context.VERSION_ES6;
        val scope = ctx.initStandardObjects();


        withJavaObjects?.forEach { name, obj ->
            val wrappedOut = Context.javaToJS(obj, scope);
            ScriptableObject.putProperty(scope, name, wrappedOut);
        }

        ctx.evaluateString(scope, jsSource, jsSourceName, 1, null);
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

    } finally {
        Context.exit();
    }
}
