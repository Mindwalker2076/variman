/*
 * Variman RETS Server
 *
 * Author: Dave Dribin
 * Copyright (c) 2004, The National Association of REALTORS
 * Distributed under a BSD-style license.  See LICENSE.TXT for details.
 */

/*
 */
package org.realtors.rets.server.admin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.cfg.Configuration;
import org.realtors.rets.server.PasswordMethod;
import org.realtors.rets.server.RetsServer;
import org.realtors.rets.server.RetsServerException;
import org.realtors.rets.server.SessionHelper;
import org.realtors.rets.server.config.DatabaseConfig;
import org.realtors.rets.server.config.DatabaseType;
import org.realtors.rets.server.config.RetsConfig;
import org.realtors.rets.server.config.RetsConfigImpl;
import org.realtors.rets.server.config.SecurityConstraints;
import org.realtors.rets.server.config.XmlRetsConfigUtils;

public class AdminUtils
{

    public static void initConfig() throws RetsServerException
    {
        initAdminProperties();
        File configFile = new File(Admin.getHomeDirectory() +
                                   "/webapp/WEB-INF/rets/rets-config.xml");
        Admin.setConfigFile(configFile.getAbsolutePath());
        if (configFile.exists())
        {
            Admin.setRetsConfig(
                XmlRetsConfigUtils.initFromXmlFile(Admin.getConfigFile()));
            Admin.setRetsConfigChanged(false);
        }
        else
        {
            RetsConfig retsConfig = new RetsConfigImpl();
            String defaultDirectory = SystemUtils.USER_DIR + File.separator;
            retsConfig.setMetadataDir(defaultDirectory + "metadata");
            retsConfig.setGetObjectRoot(defaultDirectory + "pictures");
            retsConfig.setPhotoPattern("%k-%i.jpg");
            DatabaseConfig databaseConfig = new DatabaseConfig();
            databaseConfig.setDatabaseType(DatabaseType.POSTGRESQL);
            databaseConfig.setHostName("localhost");
            retsConfig.setDatabase(databaseConfig);
            retsConfig.setSecurityConstraints(new SecurityConstraints());
            Admin.setRetsConfig(retsConfig);
            Admin.setRetsConfigChanged(true);
        }

        File logConfigFile =
            new File(Admin.getHomeDirectory() +
                     "/webapp/WEB-INF/rets/rets-logging.properties");
        Admin.setLogConfigFile(logConfigFile.getAbsolutePath());
    }

    public static void initAdminProperties() throws RetsServerException
    {
        try
        {
            Admin.initProperties();
            LOG.info(Admin.ADMIN_NAME + " version " + Admin.getVersion());
            LOG.info("Build date " + Admin.getBuildDate());
            LOG.info("Java version " + SystemUtils.JAVA_VERSION);
            LOG.info(SystemUtils.JAVA_RUNTIME_NAME + ", version " +
                     SystemUtils.JAVA_RUNTIME_VERSION);
            LOG.info(SystemUtils.JAVA_VM_NAME + ", version " +
                     SystemUtils.JAVA_VM_VERSION);
            LOG.info(SystemUtils.OS_NAME + ", version " +
                     SystemUtils.OS_VERSION);
        }
        catch (IOException e)
        {
            throw new RetsServerException(e);
        }
    }

    public static void initDatabase()
        throws HibernateException, RetsServerException
    {
        LOG.debug("Initializing Hibernate configuration");
        RetsConfig retsConfig = Admin.getRetsConfig();
        LOG.info("JDBC URL: " + retsConfig.getDatabase().getUrl());
        Configuration config = new Configuration()
            .addJar(Admin.PROJECT_NAME + "-hbm-xml.jar")
            .setProperties(retsConfig.getDatabase().createHibernateProperties());
        SessionFactory sessionFactory =
            config.buildSessionFactory();
        PasswordMethod.setDefaultMethod(PasswordMethod.DIGEST_A1,
                                        PasswordMethod.RETS_REALM);
        LOG.debug("Hibernate initialized");
        Admin.setHibernateConfiguration(config);
        RetsServer.setSessions(sessionFactory);
        logDatabaseInfo();
    }

    private static void logDatabaseInfo() throws RetsServerException
    {
        SessionHelper helper = RetsServer.createHelper();
        try
        {
            Session session = helper.beginSession();
            Connection connection = session.connection();
            DatabaseMetaData metaData = connection.getMetaData();
            LOG.info("JDBC Driver info: " + metaData.getDriverName() +
                     " version " + metaData.getDriverVersion());
            LOG.info("JDBC DB info: " + metaData.getDatabaseProductName() +
                     " version " + metaData.getDatabaseProductVersion());
        }
        catch (SQLException e)
        {
            throw new RetsServerException("Caught", e);
        }
        catch (HibernateException e)
        {
            throw new RetsServerException("Caught", e);
        }
        finally
        {
            helper.close(LOG);
        }

    }

    private static final Logger LOG =
        Logger.getLogger(AdminUtils.class);
}
