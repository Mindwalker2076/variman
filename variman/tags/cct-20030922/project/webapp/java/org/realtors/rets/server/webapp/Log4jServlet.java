/*
 */
package org.realtors.rets.server.webapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @web.servlet name="log4j-servlet"
 * @web.servlet-mapping url-pattern="/log4j"
 */
public class Log4jServlet extends RetsServlet
{
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        WebApp.loadLog4j();
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Done");
    }
}
