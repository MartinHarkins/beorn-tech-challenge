package com.beorntech

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.stream.Collectors
import javax.servlet.ServletException
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


val htmlFileNameRegex = """/(\w|\d)+\.html$""".toRegex();

fun isHtmlFilename(fileName: String): Boolean {
    return htmlFileNameRegex.matches(fileName);
}


class SSRServlet : HttpServlet() {
    @Throws(ServletException::class, IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        if (!htmlFileNameRegex.matches(request.pathInfo)) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE)
            return
        }

        // todo needs more security
        FileReader("src/main/resources" + request.pathInfo).use { fileReader ->
            BufferedReader(fileReader).use { reader ->
                val contents = reader.lines()
                        .collect(Collectors.joining(System.lineSeparator()))

                RhinoJSInterpreter { jsInterpreter ->
                    val res = parseHtml(jsInterpreter.inject(Pair("request", request)), contents)
                    response.writer.println(res)
                }

                response.contentType = "text/html"
                response.setStatus(HttpServletResponse.SC_OK)
            }
        }

    }
}