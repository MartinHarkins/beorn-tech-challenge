package com.beorntech

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class SSRServlet : HttpServlet() {
    @Throws(ServletException::class, IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "text/html"
        response.setStatus(HttpServletResponse.SC_OK)
        response.writer.println("<h1>Returning Anything</h1>")
        response.writer.println("session=" + request.getSession(true).id)
    }
}