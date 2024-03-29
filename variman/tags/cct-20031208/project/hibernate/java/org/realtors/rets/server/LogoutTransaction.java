/*
 */
package org.realtors.rets.server;

import java.io.PrintWriter;

import org.apache.log4j.Logger;

public class LogoutTransaction
{
    public void execute(PrintWriter out, AccountingStatistics stats)
    {
        LOG.debug("Duration: " + stats.getSessionDuration());

        RetsUtils.printOpenRetsSuccess(out);
        RetsUtils.printOpenRetsResponse(out);
        out.println("ConnectTime = " + stats.getSessionTime());
        out.println("Billing = " + stats.getSessionBalanceFormatted());
        out.println("SignOffMessage = Goodbye");
        RetsUtils.printCloseRetsResponse(out);
        RetsUtils.printCloseRets(out);
    }

    private static final Logger LOG =
        Logger.getLogger(LogoutTransaction.class);
}
