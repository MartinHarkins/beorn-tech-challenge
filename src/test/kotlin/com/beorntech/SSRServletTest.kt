package com.beorntech

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SSRServletTest : StringSpec({
    "should let valid html file names through" {
        val validFilenames = listOf("test.html", "test2.html")

        validFilenames.forEach { filename -> isHtmlFilename(filename) shouldBe true }
    }

    "should NOT let invalid html file names through" {
        val invalidFilenames = listOf("t/est.html", "test^2.html", "test3.html3")

        invalidFilenames.forEach { filename -> isHtmlFilename(filename) shouldBe false }
    }
})