package com.beorntech

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HtmlParserTest : StringSpec ({
    "Should find script tags with our server javascript handle" {
        val script1 = "script1";
        val script2 = "script2";
        val htmlBase = """
                <script type="server/javascript">$script1</script>
                <div/>
                <script type="server/javascript">$script2</script>
            """".trimIndent()
        val res = parseHtml(htmlBase);
        res.size shouldBe 2;
        res[0] shouldBe script1;
        res[1] shouldBe script2;
    }
})

