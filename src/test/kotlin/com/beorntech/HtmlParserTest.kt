package com.beorntech

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable


class HtmlParserTest : StringSpec({
    "should find script tags with our server javascript handle" {
        val script1 = "script1"
        val script2 = "script2"
        val htmlBase = """
                <script type="server/javascript">$script1</script>
                <div/>
                <script type="server/javascript">$script2</script>
            """".trimIndent()
        val res = parseHtml(htmlBase)
        res.size shouldBe 2
        res[0] shouldBe script1
        res[1] shouldBe script2
    }

    "should do something with a script" {
        val script = """
            obj = {a:1, b:['x','y']}
        """.trimIndent()

        parseScript(script, "testScript", null
        ) { ctx, scope ->
            val obj: Scriptable = scope.get("obj", scope) as Scriptable

            obj.get("a", obj) as Double shouldBe 1
        }
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

        parseScript(script, "testScript",
                hashMapOf(Pair("dummy", dummyInstance)))
        { ctx, scope ->
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
        }
    }

    "should evaluate valid expressions" {
        shouldEvaluate("\${person.test}") shouldBe true
        shouldEvaluate("\${someVar}") shouldBe true
        shouldEvaluate("\${someFunc()}") shouldBe true
        shouldEvaluate("\${person.someFunc()}") shouldBe true
        shouldEvaluate("\${person.test}suffix") shouldBe false
        shouldEvaluate("prefix\${person.test}") shouldBe false
        shouldEvaluate("\${}") shouldBe false

        // sample of incomplete tests
//        shouldEvaluate("\${()}") shouldBe false
//        shouldEvaluate("\${2var}") shouldBe false
    }

    "should read simple expression" {
        initializeContext { context, scope ->
            read(context, scope, "\${5}") shouldBe "5"
        }
    }
})