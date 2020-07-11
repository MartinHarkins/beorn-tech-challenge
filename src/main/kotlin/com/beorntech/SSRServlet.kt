package com.beorntech

import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolver
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

val htmlFileNameRegex = """(\w|\d)+\.html$""".toRegex();

fun isHtmlFilename(fileName: String): Boolean {
    return htmlFileNameRegex.matches(fileName);
}


class SSRServlet : HttpServlet() {
    @Throws(ServletException::class, IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "text/html"
        response.setStatus(HttpServletResponse.SC_OK)
        response.writer.println("<h1>Returning Anything</h1>")
        response.writer.println("session=" + request.getSession(true).id)
    }
}