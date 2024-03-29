/*
 */
package org.realtors.rets.server.webapp.auth;

import org.realtors.rets.server.DigestUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class DigestAuthorizationRequest
{
    public DigestAuthorizationRequest()
    {
        mUsername = null;
        mUri = null;
        mNonce = null;
        mQop = null;
        mMethod = null;
        mAlgorithm = "MD5";
        mNonceCount = "";
        mCnonce = "";
        mResponse = "";
        mOpaque = "";
    }

    /**
     * Initialize a request from an HTTP "Authorization" header line.
     *
     * @param header HTTP "Authorization" header
     * @param method HTTP method, i.e. GET, POST, etc.
     * @param requestUri HTTP Request-URI
     * @throws IllegalArgumentException if the header does not adhere to RFC
     * 2617
     */
    public DigestAuthorizationRequest(String header, String method,
                                      String requestUri)
    {
        this();
        mMethod = method;
        LOG.debug("Authorization header: <" + header + ">");
        // Valid header, but verify should always fail
        if (header == null)
        {
            return;
        }
        if (!header.startsWith(PREFIX))
        {
            throw new IllegalArgumentException("Incorrect prefix <" + header +
                                               ">");
        }
        String authorization = header.substring(PREFIX.length());

        String[] tokens = StringUtils.split(authorization, ",");
        for (int i = 0; i < tokens.length; i++)
        {
            String token = tokens[i];
            String[] keyValue = StringUtils.split(token, "=", 2);
            if (keyValue.length != 2)
            {
                throw new IllegalArgumentException(
                    "Invalid key/value pair: <" + token + "> <" + header + ">");
            }
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            if (key.equals("username"))
            {
                mUsername = removeQuotes(value);
            }
            else if (key.equals("realm"))
            {
                mRealm = removeQuotes(value);
            }
            else if (key.equals("nonce"))
            {
                mNonce = removeQuotes(value);
            }
            else if (key.equals("uri"))
            {
                mUri = removeQuotes(value);
            }
            else if (key.equals("qop"))
            {
                mQop = removeQuotes(value);
            }
            else if (key.equals("algorithm"))
            {
                mAlgorithm = removeQuotes(value);
            }
            else if (key.equals("nc"))
            {
                mNonceCount = value;
            }
            else if (key.equals("cnonce"))
            {
                mCnonce = removeQuotes(value);
            }
            else if (key.equals("response"))
            {
                mResponse = removeQuotes(value);
            }
            else if (key.equals("opaque"))
            {
                mOpaque = removeQuotes(value);
            }
        }

        if ((mUsername == null) || (mRealm == null) || (mNonce == null) ||
            (mUri == null) || (mResponse == null))
        {
            throw new IllegalArgumentException("Required fields not set");
        }

        if (!mUri.equals(requestUri))
        {
            throw new IllegalArgumentException(
                "URI from header <" + mUri + "> does not match Request-URI <" +
                requestUri + ">");
        }
    }

    private String removeQuotes(String string)
    {
        int start = 0;
        if (string.startsWith("\""))
        {
            start++;
        }

        int end = string.length();
        if (string.endsWith("\""))
        {
            end--;
        }

        return string.substring(start, end);
    }

    public void setUsername(String username)
    {
        mUsername = username;
    }

    public String getUsername()
    {
        return mUsername;
    }

    public void setUri(String uri)
    {
        mUri = uri;
    }

    public String getUri()
    {
        return mUri;
    }

    public void setNonce(String nonce)
    {
        mNonce = nonce;
    }

    public String getNonce()
    {
        return mNonce;
    }

    public void setQop(String qop)
    {
        mQop = qop;
    }

    public String getQop()
    {
        return mQop;
    }

    public String getAlgorithm()
    {
        return mAlgorithm;
    }

    public void setNonceCount(String nonceCount)
    {
        mNonceCount = nonceCount;
    }

    public String getNonceCount()
    {
        return mNonceCount;
    }

    public String getCnonce()
    {
        return mCnonce;
    }

    public void setResponse(String response)
    {
        mResponse = response;
    }

    public String getResponse()
    {
        return mResponse;
    }

    public void setOpaque(String opaque)
    {
        mOpaque = opaque;
    }

    public String getOpaque()
    {
        return mOpaque;
    }

    public void setCnonce(String cnonce)
    {
        mCnonce = cnonce;
    }

    public void setRealm(String realm)
    {
        mRealm = realm;
    }

    public String getRealm()
    {
        return mRealm;
    }

    public void setMethod(String method)
    {
        mMethod = method;
    }

    public String getMethod()
    {
        return mMethod;
    }

    public boolean verifyResponse(String password)
    {
        return verifyResponse(password, false);
    }

    public boolean verifyResponse(String password, boolean passwordIsA1)
    {
        // Check for absolute failures, i.e. cases that should fail no matter
        // what the digest turns out to be.
        if ((password == null) || (mUsername == null))
        {
            return false;
        }
        String expectedResponse = calculateRequestDigest(password,
                                                         passwordIsA1);
        LOG.debug("Expected response: " + expectedResponse);
        LOG.debug("Actual response:   " + mResponse);
        return (expectedResponse.equals(mResponse));
    }

    /**
     * Calculates the request digest according to Section 3.2.2.1 of RFC
     * 2617. This has default (package) scope to allow for testing, though it
     * could be private.
     *
     * @param password Plain text password
     * @param passwordIsA1 <code>true</code> if the password is A1
     * @return The expected digest response
     */
    String calculateRequestDigest(String password, boolean passwordIsA1)
    {
        String a1;
        if (passwordIsA1)
        {
            a1 = password;
        }
        else
        {
            a1 = DigestUtils.md5Hex(mUsername + ":" + mRealm + ":" + password);
        }
        String a2 = DigestUtils.md5Hex(mMethod + ":" + mUri);

        if (mQop == null)
        {
            return DigestUtils.md5Hex(a1 + ":" + mNonce + ":" + a2);
        }
        else
        {
            return DigestUtils.md5Hex(a1 + ":" + mNonce + ":" + mNonceCount +
                                      ":" + mCnonce + ":" + mQop + ":" + a2);
        }
    }

    private static final String PREFIX = "Digest ";
    private String mUsername;
    private String mUri;
    private String mNonce;
    private String mQop;
    private String mAlgorithm;
    private String mNonceCount;
    private String mCnonce;
    private String mResponse;
    private String mOpaque;
    private String mRealm;
    private String mMethod;
    private static final Logger LOG =
        Logger.getLogger(DigestAuthorizationRequest.class);
}
