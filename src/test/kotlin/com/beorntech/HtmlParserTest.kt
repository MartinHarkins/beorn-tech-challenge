package com.beorntech

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe


class HtmlParserTest : StringSpec({

    "should evaluate valid template expressions" {
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

    "should translate expressions within attributes and elements" {
        val expression1 = "\${1}"
        val expression2 = "\${2}"

        val res = parseHtml(
                RhinoJSInterpreter(),
                """<html><head></head><body><div some-attr="$expression1">$expression2</div></body></html>"""
        )
        assertHtmlEquals(
                res,
                """<html><head></head><body><div some-attr="1">2</div></body></html>""")
    }

    "should translate expressions using scopped vars" {
        val expression = "\${a}"
        val res = parseHtml(RhinoJSInterpreter(),
                """
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

        val res = parseHtml(
                RhinoJSInterpreter().inject(Pair("dummy", dummyInstance)),
                """
<html>
    <head></head>
    <body>
        <script type="server/javascript">
        dummy.writeName("me")
        </script>
        <div>$expression</div>
    </body>
</html>""")
        assertHtmlEquals(res,
                """
<html>
    <head></head>
    <body>
        <div>me</div>
    </body>
</html>""")
    }

    "should handle data-if expression to decide on block rendering" {
        val res = parseHtml(RhinoJSInterpreter(),
                """
<html>
    <head></head>
    <body>
        <script type="server/javascript">
        var show = true;
        var dontshow = false;
        </script>
        <div data-if="show">should be returned</div>
        <div data-if="dontshow">should NOT be returned</div>
    </body>
</html>""")
        assertHtmlEquals(
                res,
                """
<html>
    <head></head>
    <body>
        <div>should be returned</div>
    </body>
</html>""")
    }

    "should handle data-for-x expressions" {
        val expression = "\${num}"
        val res =
                parseHtml(RhinoJSInterpreter(),
                        """
<html>
    <head></head>
    <body>
        <script type="server/javascript">
        var arr = ["1", "2", "3"];
        </script>
        <div data-for-num="arr">number: $expression</div>
    </body>
</html>""")
        assertHtmlEquals(
                res,
                """
<html>
    <head></head>
    <body>
        <div>number: 1</div>
        <div>number: 2</div>
        <div>number: 3</div>
    </body>
</html>""")
    }

    "should handle data-for-x expressions and keep scoped vars" {
        val expression = "\${num}"
        val expression2 = "\${someVar}"
        val res = parseHtml(RhinoJSInterpreter(),
                """
<html>
    <head></head>
    <body>
        <script type="server/javascript">
        var someVar = 0;
        var arr = ["1", "2", "3"];
        </script>
        <div data-for-num="arr">number: $expression $expression2</div>
    </body>
</html>""")
        assertHtmlEquals(
                res,
                """
<html>
    <head></head>
    <body>
        <div>number: 1 0</div>
        <div>number: 2 0</div>
        <div>number: 3 0</div>
    </body>
</html>""")
    }
})

fun assertHtmlEquals(value: String, expecting: String) {
    onelineHtml(value) shouldBe onelineHtml(expecting)
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