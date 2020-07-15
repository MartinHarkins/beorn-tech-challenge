package com.beorntech

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable

class RhinoJSInterpreterTest : StringSpec({
    "should read simple expression" {
        RhinoJSInterpreter { jsInterpreter ->
            jsInterpreter.evalAsString(stripTemplating("\${5}")) shouldBe "5"
        }
    }

    "should do something with a script" {
        val script = """
            obj = {a:1, b:['x','y']}
        """.trimIndent()

        val rhinoInterpreter = RhinoJSInterpreter();
        rhinoInterpreter.eval(script)

        val obj: Scriptable = rhinoInterpreter.scope.get("obj", rhinoInterpreter.scope) as Scriptable
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

        val rhinoJSInterpreter = RhinoJSInterpreter()
                .inject(Pair("dummy", dummyInstance)) as RhinoJSInterpreter

        rhinoJSInterpreter.eval(script)

        val scope = rhinoJSInterpreter.scope

        val initialName: NativeJavaObject = scope.get("initialName", scope) as NativeJavaObject
        initialName.unwrap() shouldBe "you"
        rhinoJSInterpreter.evalAsString("initialName") shouldBe "you"
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
})