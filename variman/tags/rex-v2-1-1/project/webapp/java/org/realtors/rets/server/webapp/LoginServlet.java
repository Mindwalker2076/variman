/*
 * Rex RETS Server
 *
 * Author: Dave Dribin
 * Copyright (c) 2004, The National Association of REALTORS
 * Distributed under a BSD-style license.  See LICENSE.TXT for details.
 */

package org.realtors.rets.server.webapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpSession;

import org.realtors.rets.server.AccountingStatistics;
import org.realtors.rets.server.RetsServerException;
import org.realtors.rets.server.User;
import org.realtors.rets.server.RetsVersion;
import org.realtors.rets.server.RetsUtils;

import org.apache.log4j.Logger;

/**
 * Performs all necessary one-time initializations for the web
 * application.
 *
 * @web.servlet name="login-servlet"
 * @web.servlet-mapping url-pattern="/rets/login"
 */
public class LoginServlet extends RetsServlet
{
    protected void doRets(RetsServletRequest request,
                          RetsServletResponse response)
        throws RetsServerException, IOException
    {
        StringBuffer contextPath = ServletUtils.getContextPath(request);
        LOG.debug("context=" + contextPath);

        HttpSession session = request.getSession();
        SessionFilter.validateSession(session);
        AccountingStatistics statitics = getStatistics(session);
        statitics.startSession();
        User user = getUser(session);

        PrintWriter out = response.getXmlWriter();
        RetsUtils.printOpenRetsSuccess(out);
        if (request.getRetsVersion() != RetsVersion.RETS_1_0)
        {
            RetsUtils.printOpenRetsResponse(out);
        }
        out.println("Broker = " + user.getBrokerCode());
        out.println("MemberName = " + user.getName());
        out.println("MetadataVersion = 1.0.000");
        out.println("MinMetadataVersion = 1.00.000");
        out.println("User = " + user.getUsername() + ",NULL,NULL,NULL");
        out.println("Login = " + contextPath + Paths.LOGIN);
        out.println("Logout = " + contextPath + Paths.LOGOUT);
        out.println("Search = " + contextPath + Paths.SEARCH);
        out.println("GetMetadata = " + contextPath + Paths.GET_METADATA);
        out.println("ChangePassword = " + contextPath + Paths.CHANGE_PASSWORD);
        out.println("GetObject = " + contextPath + Paths.GET_OBJECT);
        out.println("Balance = " + statitics.getTotalBalanceFormatted());
        out.println("TimeoutSeconds = " + session.getMaxInactiveInterval());
        if (request.getRetsVersion() != RetsVersion.RETS_1_0)
        {
            RetsUtils.printCloseRetsResponse(out);
        }
        RetsUtils.printCloseRets(out);
    }

    private static final Logger LOG =
        Logger.getLogger(LoginServlet.class);
}
