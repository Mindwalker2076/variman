/*
 */
package org.realtors.rets.server.webapp.cct;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.commons.lang.exception.NestableRuntimeException;

public abstract class BaseServletHandler implements ServletHandler
{
    public BaseServletHandler()
    {
        mExpectedHeaders = new HashMap();
    }

    public void reset()
    {
        mExpectedHeaders.clear();
        mDoGetInvokeCount = 0;
        mInvokeCount = InvokeCount.ANY;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        mDoGetInvokeCount++;
        mRequest = request;
        LOG.info(getName() + " doGet invoked: " + mDoGetInvokeCount);
    }

    public void setGetInvokeCount(InvokeCount invokeCount)
    {
        mInvokeCount = invokeCount;
    }

    public ValidationResults validate()
    {
        ValidationResults result = new ValidationResults();
        if (mInvokeCount.equals(InvokeCount.ONE) && !(mDoGetInvokeCount == 1))
        {
            result.addFailure(getName() + " get invoke count was " +
                              mDoGetInvokeCount +
                              ", expected " + mInvokeCount.getName());
        }

        if (mInvokeCount.equals(InvokeCount.ZERO_OR_ONE) &&
            !((mDoGetInvokeCount == 0) || (mDoGetInvokeCount == 1)))
        {
            // Todo: LoginHandler.validate: log error
        }

        validateHeaders(result);
        return result;
    }

    private void validateHeaders(ValidationResults result)
    {
        if (mRequest == null)
        {
            return;
        }

        Set names = mExpectedHeaders.keySet();
        for (Iterator iterator = names.iterator(); iterator.hasNext();)
        {
            String name = (String) iterator.next();
            String header = mRequest.getHeader(name);
            String regexp = (String) mExpectedHeaders.get(name);
            if ((header == null) || !matches(header, regexp))
            {
                result.addFailure(getName() + " HTTP header [" + name +
                                  "] was " + header + ", expected " + regexp);
            }
        }
    }

    public void addRequiredHeader(String header, String pattern)
    {
        mExpectedHeaders.put(header, pattern);
    }

    public static boolean matches(String string, String regexp)
    {
        try
        {
            Perl5Compiler compiler = new Perl5Compiler();
            Pattern pattern = compiler.compile(regexp);
            Perl5Matcher matcher = new Perl5Matcher();
            return matcher.contains(string, pattern);
        }
        catch (MalformedPatternException e)
        {
            // Convert to runtime exception
            throw new NestableRuntimeException(e);
        }
    }

    private static final Logger LOG =
        Logger.getLogger(BaseServletHandler.class);
    private Map mExpectedHeaders;
    private int mDoGetInvokeCount;
    private InvokeCount mInvokeCount;
    private HttpServletRequest mRequest;
}