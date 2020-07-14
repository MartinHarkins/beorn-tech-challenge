package com.beorntech

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable


class HtmlParserTest : StringSpec({
//    "should find script tags with our server javascript handle" {
//        val script1 = "script1"
//        val script2 = "script2"
//        val htmlBase = """
//                <script type="server/javascript">$script1</script>
//                <div/>
//                <script type="server/javascript">$script2</script>
//            """".trimIndent()
//        val res = parseHtml(htmlBase)
//        res.size shouldBe 2
//        res[0] shouldBe script1
//        res[1] shouldBe script2
//    }

    "should do something with a script" {
        val script = """
            obj = {a:1, b:['x','y']}
        """.trimIndent()

        val rhinoInterpreter = RhinoJSInterpreter();
        rhinoInterpreter.eval(script)

        val obj: Scriptable = rhinoInterpreter.getScope().get("obj", rhinoInterpreter.getScope()) as Scriptable
        obj.get("a", obj) as Double shouldBe 1

        rhinoInterpreter.close();
    }

    "should allow for pre-injected java objects in the script scope" {
        class Dummy(name: String) {
            var name: String = ""
            fun writeName(name: String) {
                this.name = name
            }

            init {
                writeName(name)
            }
        }

        val dummyInstance = Dummy("you")
        val script = """
            var initialName = dummy.name;
            dummy.writeName("me");
            var finalName = dummy.name;
        """.trimIndent()

        val rhinoJSInterpreter = RhinoJSInterpreter(withJavaContext = hashMapOf(Pair("dummy", dummyInstance)))

        rhinoJSInterpreter.eval(script)

        val scope = rhinoJSInterpreter.getScope()

        val initialName: NativeJavaObject = scope.get("initialName", scope) as NativeJavaObject
        initialName.unwrap() shouldBe "you"
        val finalName: NativeJavaObject = scope.get("finalName", scope) as NativeJavaObject
        finalName.unwrap() shouldBe "me"

        // Go get the name in the java bean object too.
        val dummyFromJS: NativeJavaObject = scope.get("dummy", scope) as NativeJavaObject
        if (dummyFromJS !== Scriptable.NOT_FOUND) {
            val theName = dummyFromJS.get("name", dummyFromJS) as NativeJavaObject
            theName.unwrap() shouldBe "me"
        }
        rhinoJSInterpreter.close()
    }

    "should evaluate valid expressions" {
        isTemplatingExpression("\${person.test}") shouldBe true
        isTemplatingExpression("\${someVar}") shouldBe true
        isTemplatingExpression("\${someFunc()}") shouldBe true
        isTemplatingExpression("\${person.someFunc()}") shouldBe true
        isTemplatingExpression("\${person.test}suffix") shouldBe false
        isTemplatingExpression("prefix\${person.test}") shouldBe false
        isTemplatingExpression("\${}") shouldBe false

        // sample of incomplete tests
//        shouldEvaluate("\${()}") shouldBe false
//        shouldEvaluate("\${2var}") shouldBe false
    }

    "should read simple expression" {
        RhinoJSInterpreter { jsInterpreter ->
            jsInterpreter.evalAsString(stripTemplating("\${5}")) shouldBe "5"
        }
    }

    "should translate expressions within attributes and elements" {
        val expression1 = "\${1}"
        val expression2 = "\${2}"

        val res = parseHtml(
                """<html><head></head><body><div some-attr="$expression1">$expression2</div></body></html>"""
        )
        assertHtmlEquals(
                res,
                """<html><head></head><body><div some-attr="1">2</div></body></html>""")
    }

    "should translate expressions using scopped vars" {
        val expression = "\${a}"
        val res = parseHtml("""
<html>
    <head></head>
    <body>
        <script type="server/javascript">
        var a = 1;
        </script>
        <div>$expression</div>
    </body>
</html>""")

        assertHtmlEquals(
                res,
                """<html><head></head><body><div>1</div></body></html>""") // dunno why but despite output settings, jsoup ouputs the div on a newline.
    }

    "should translate expressions using injected java models" {
        class Dummy(name: String) {
            var name: String = ""
            fun writeName(name: String) {
                this.name = name
            }

            init {
                writeName(name)
            }
        }

        val dummyInstance = Dummy("you")

        val expression = "\${dummy.name}"
        val res = parseHtml("""
<html>
    <head></head>
    <body>
        <script type="server/javascript">
        dummy.writeName("me")
        </script>
        <div>$expression</div>
    </body>
</html>""", withJavaObjects = hashMapOf(Pair("dummy", dummyInstance)))
        assertHtmlEquals(res,
                """
<html>
    <head></head>
    <body>
        <div>me</div>
    </body>
</html>""")
    }
})

fun assertHtmlEquals(str1: String, str2: String) {
    onelineHtml(str1) shouldBe onelineHtml(str2)
}

fun onelineHtml(str: String): String {
    // todo: this has been a big big pain and it still is a very poor solution.
    return str.replace("  ", "")
            .replace("\n", "")
            .replace(" <", "<")
            .replace("> ", ">")
            .replace("\n", "")
//    Jsoup just doesn't do a good job at cleaning up
//    The following is not respected in all the cases
//    val doc = Jsoup.parse(str);
//    return doc.outputSettings(
//                    Document.OutputSettings()
//                            .indentAmount(0)
//                            .outline(false)
//                            .prettyPrint(false)
//            ).toString()
}