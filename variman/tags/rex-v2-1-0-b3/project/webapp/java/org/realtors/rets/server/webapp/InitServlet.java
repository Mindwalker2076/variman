/*
 * Rex RETS Server
 *
 * Author: Dave Dribin
 * Copyright (c) 2004, The National Association of REALTORS
 * Distributed under a BSD-style license.  See LICENSE.TXT for details.
 */

/*
 */
package org.realtors.rets.server.webapp;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.cfg.Configuration;
import org.realtors.rets.server.IOUtils;
import org.realtors.rets.server.PasswordMethod;
import org.realtors.rets.server.RetsServer;
import org.realtors.rets.server.RetsServerException;
import org.realtors.rets.server.config.GroupRules;
import org.realtors.rets.server.config.RetsConfig;
import org.realtors.rets.server.metadata.MClass;
import org.realtors.rets.server.metadata.MSystem;
import org.realtors.rets.server.metadata.MetadataLoader;
import org.realtors.rets.server.metadata.MetadataManager;
import org.realtors.rets.server.metadata.Resource;
import org.realtors.rets.server.protocol.TableGroupFilter;
import org.realtors.rets.server.webapp.auth.NonceReaper;
import org.realtors.rets.server.webapp.auth.NonceTable;

/**
 * @web.servlet name="init-servlet"
 *   load-on-startup="1"
 */
public class InitServlet extends RetsServlet
{
    public void init() throws ServletException
    {
        initLog4J();
        try
        {
            LOG.info("Running init servlet");
            WebApp.setServletContext(getServletContext());
            PasswordMethod.setDefaultMethod(PasswordMethod.DIGEST_A1,
                                            PasswordMethod.RETS_REALM);
            initRetsConfiguration();
            initHibernate();
            initMetadata();
            initNonceTable();
            initGroupFilter();
            LOG.info("Init servlet completed successfully");
        }
        catch (ServletException e)
        {
            LOG.fatal("Caught", e);
            Throwable rootCause = e.getRootCause();
            if (rootCause != null)
            {
                LOG.fatal("Caused by", rootCause);
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            LOG.fatal("Caught", e);
            throw e;
        }
        catch (Error e)
        {
            LOG.fatal("Caught", e);
            throw e;
        }
    }

    private String getContextInitParameter(String name, String defaultValue)
    {
        String value = getServletContext().getInitParameter(name);
        if (value == null)
        {
            value = defaultValue;
        }
        return value;
    }

    private String resolveFromConextRoot(String file)
    {
        return IOUtils.resolve(getServletContext().getRealPath("/"), file);
    }

    /**
     * Initialize log4j. First, the application's context is checked for the
     * file name, and then the servlet context is checked.
     */
    private void initLog4J()
    {
        initRexLogHome();
        String log4jInitFile =
            getContextInitParameter("log4j-init-file",
                                    "WEB-INF/classes/rex-webapp-log4j.xml");
        log4jInitFile = resolveFromConextRoot(log4jInitFile);
        WebApp.setLog4jFile(log4jInitFile);
        WebApp.loadLog4j();
    }

    /**
     * Sets the <code>rex.log.home</code> system property so it can be used in
     * the log4j config file.
     */
    private void initRexLogHome()
    {
        String rexLogHome =
            getServletContext().getInitParameter("rex-log4j-home");
        if (rexLogHome == null)
        {
            rexLogHome = System.getProperty("rex.log.home");
            if (rexLogHome == null)
            {
                rexLogHome = System.getProperty("rex.home");
                if (rexLogHome == null)
                {
                    rexLogHome = System.getProperty("user.dir");
                }
                else
                {
                    rexLogHome = rexLogHome + File.separator + "logs";
                }
            }
        }
        System.setProperty("rex.log.home", rexLogHome);
    }

    private void initRetsConfiguration() throws ServletException
    {
        try
        {
            String configFile =
                getContextInitParameter("rets-config-file",
                                        "WEB-INF/rex/rets-config.xml");
            configFile = resolveFromConextRoot(configFile);
            mRetsConfig = RetsConfig.initFromXml(new FileReader(configFile));
            LOG.debug(mRetsConfig);

            ServletContext context = getServletContext();
            String getObjectRoot =
                mRetsConfig.getGetObjectRoot(context.getRealPath("/"));
            WebApp.setGetObjectRoot(getObjectRoot);
            LOG.debug("GetObject root: " + getObjectRoot);

            String getObjectPattern =
                mRetsConfig.getGetObjectPattern("pictures/%k-%i.jpg");
            WebApp.setGetObjectPattern(getObjectPattern);
            LOG.debug("GetObject pattern: " + getObjectPattern);
        }
        catch (IOException e)
        {
            throw new ServletException(e);
        }
        catch (RetsServerException e)
        {
            throw new ServletException(e);
        }
    }

    private void initHibernate() throws ServletException
    {
        try
        {
            LOG.debug("Initializing hibernate");
            Configuration cfg = new Configuration();
            cfg.addJar("rex-hbm-xml.jar");
            cfg.setProperties(mRetsConfig.createHibernateProperties());
            RetsServer.setSessions(cfg.buildSessionFactory());
        }
        catch (HibernateException e)
        {
            throw new ServletException("Could not initialize hibernate", e);
        }
    }

    private void initMetadata() throws ServletException
    {
        LOG.debug("Initializing metadata");
        MSystem system = findSystem();
        LOG.debug("Creating metadata manager");
        MetadataManager manager = new MetadataManager();
        manager.addRecursive(system);
        LOG.debug("Adding metadata to servlet context");
        WebApp.setMetadataManager(manager);
    }

    private MSystem findSystem() throws ServletException
    {
        try
        {
            String metadataDir = mRetsConfig.getMetadataDir();
            metadataDir = resolveFromConextRoot(metadataDir);
            LOG.info("Reading metadata from: " + metadataDir);
            MetadataLoader loader = new MetadataLoader();
            return loader.parseMetadataDirectory(metadataDir);
        }
        catch (RetsServerException e)
        {
            throw new ServletException(e);
        }
    }

    private void initNonceTable()
    {
        NonceTable nonceTable = new NonceTable();
        int initialTimeout = mRetsConfig.getNonceInitialTimeout();
        if (initialTimeout != -1)
        {
            nonceTable.setInitialTimeout(
                initialTimeout * DateUtils.MILLIS_IN_MINUTE);
            LOG.debug("Set initial nonce timeout to " + initialTimeout +
                      " minutes");
        }

        int successTimeout = mRetsConfig.getNonceSuccessTimeout();
        if (successTimeout != -1)
        {
            nonceTable.setSuccessTimeout(
                successTimeout * DateUtils.MILLIS_IN_MINUTE);
            LOG.debug("Set success nonce timeout to " + successTimeout +
                      " minutes");
        }
        WebApp.setNonceTable(nonceTable);
        WebApp.setNonceReaper(new NonceReaper(nonceTable));
    }

    private void initGroupFilter()
    {
        LOG.debug("Initializing group filter");
        TableGroupFilter groupFilter = new TableGroupFilter();
        MetadataManager metadataManager = WebApp.getMetadataManager();
        MSystem system =
            (MSystem) metadataManager.findUnique(MSystem.TABLE, "");
        Set resources = system.getResources();
        for (Iterator i = resources.iterator(); i.hasNext();)
        {
            Resource resource = (Resource) i.next();
            String resourceID = resource.getResourceID();
            Set classes = resource.getClasses();
            for (Iterator j = classes.iterator(); j.hasNext();)
            {
                MClass aClass = (MClass) j.next();
                String className = aClass.getClassName();
                LOG.debug("Setting tables for " + resourceID + ":" + className);
                groupFilter.setTables(resourceID, className,
                                      aClass.getTables());
            }
        }
        List securityConstraints = mRetsConfig.getAllGroupRules();
        for (int i = 0; i < securityConstraints.size(); i++)
        {
            GroupRules rules = (GroupRules) securityConstraints.get(i);
            LOG.debug("Adding rules for " + rules.getGroupName());
            groupFilter.addRules(rules);
        }

        RetsServer.setTableGroupFilter(groupFilter);
    }

    public void destroy()
    {
        WebApp.getReaper().stop();
    }

    private static final Logger LOG =
        Logger.getLogger(InitServlet.class);
    private RetsConfig mRetsConfig;
}
