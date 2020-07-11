package com.beorntech

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Throws(IllegalArgumentException::class)
fun parseHtml(htmlContent: String): List<String> {
    val doc: Document = Jsoup.parse(htmlContent)

    val scriptsToInterpret = doc.select("script[type=server/javascript]")
    return scriptsToInterpret.map { e -> e.data() }
}
