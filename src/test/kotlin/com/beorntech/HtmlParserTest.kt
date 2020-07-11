package com.beorntech

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HtmlParserTest {
    @Test
    @DisplayName("Should find the server/javascript script tags")
    fun shouldFindTheScriptTags() {
        val script1 = "script1";
        val script2 = "script2";
        val htmlBase = """
                <script type="server/javascript">$script1</script>
                <script type="server/javascript">$script2</script>
            """".trimIndent()
        val res = parseHtml(htmlBase);
        assertThat(res.size).isEqualTo(2);
        assertThat(res[0]).isEqualTo(script1);
        assertThat(res[1]).isEqualTo(script2);


    }
}

