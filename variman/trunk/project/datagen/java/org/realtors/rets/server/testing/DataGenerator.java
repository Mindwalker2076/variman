package org.realtors.rets.server.testing;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.realtors.rets.server.data.RetsData;
import org.realtors.rets.server.data.RetsDataElement;
import org.realtors.rets.server.metadata.MClass;
import org.realtors.rets.server.metadata.MSystem;
import org.realtors.rets.server.metadata.Resource;
import org.realtors.rets.server.metadata.Table;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import net.sf.hibernate.cfg.Configuration;

public class DataGenerator
{
    public DataGenerator() throws HibernateException
    {
        initHibernate();
        
        mClasses = new HashMap();
        mTables = new HashMap();
        mRandom = new Random(System.currentTimeMillis());
        mRandom.nextInt();

        mECount = 0;
        mESchools = new int[] { 306, 300, 42, 304, 303 };
        mACount = 0;
        mAgents = new String[] { "P345", "P123", "M123" };
        mBCount = 0;
        mBrokers = new String[] { "Laffalot Realty", "Tex Mex Real Estate",
            "Yellow Armadillo Realty", "Retzilla Realty" };

        mDCount = 0;
        mDate = new Date[4];
        Calendar cal = Calendar.getInstance();
        cal.set(2003,6,10);
        mDate[mDCount++] = cal.getTime();
        cal.set(2003,6,22);
        mDate[mDCount++] = cal.getTime();
        cal.set(2002,11,5);
        mDate[mDCount++] = cal.getTime();
        mDate[mDCount++] = new Date();
    }

    private void createData() throws HibernateException
    {
        createData(10);
    }

    private void createData(int props) throws HibernateException
    {
        Session session = null;
        Transaction tx = null;
        try
        {
            session = mSessions.openSession();
            tx = session.beginTransaction();

            for (int i = 0; i < props; i++)
            {
                RetsData retsData = new RetsData();
                MClass clazz = (MClass) mClasses.get("Property:RES");
                retsData.setClazz(clazz);
                session.save(retsData);
                
                Map dataElements = new HashMap();
                
                RetsDataElement dataElement =
                createDataElement("Property:RES:LP", mRandom.nextInt(1000000));
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);
                
                dataElement =
                    createDataElement("Property:RES:BROKER", getNextBroker());
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);
                
                dataElement =
                    createDataElement("Property:RES:AGENT_ID", getNextAgent());
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);
                
                String tmp = Long.toHexString(mRandom.nextInt(100000));
                dataElement = createDataElement("Property:RES:LN", tmp);
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);
                
                dataElement = createDataElement("Property:RES:ZIP_CODE",
                mRandom.nextInt(99999));
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);
                
                dataElement = createDataElement("Property:RES:LD",
                                                getNextDate());
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);
                
                dataElement = createDataElement("Property:RES:SQFT", 
                                                mRandom.nextInt(7000));
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);
                
                dataElement = createDataElement("Property:RES:E_SCHOOL",
                                                getNextSchool());
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);

                dataElement = createDataElement("Property:RES:M_SCHOOL",
                                                getNextSchool());
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);

                dataElement = createDataElement("Property:RES:H_SCHOOL",
                                                getNextSchool());
                dataElements.put(dataElement.getKey(), dataElement);
                session.save(dataElement);

                retsData.setDataElements(dataElements);
                session.saveOrUpdate(retsData);
            }
            tx.commit();
            session.close();
        }
        catch (HibernateException e)
        {
            if (tx != null)
            {
                tx.rollback();
            }
            if (session != null)
            {
                session.close();
            }
            throw e;
        }
    }

    /**
     * Creates a RetsDataElement
     * 
     * @param path The path to the table
     * @param value the value to store
     * @return an initialized RetsDataElement
     */
    private RetsDataElement createDataElement(String path, Date value)
    {
        RetsDataElement rde = new RetsDataElement();
        Table key = (Table) mTables.get(path);
        rde.setKey(key);
        rde.setDateValue(value);
        return rde;
    }

    /**
     * Creates a RetsDataElement
     * 
     * @param path The path to the table
     * @param value the value to store
     * @return an initialized RetsDataElement
     */
    private RetsDataElement createDataElement(String path, long value)
    {
        RetsDataElement rde = new RetsDataElement();
        Table key = (Table) mTables.get(path);
        rde.setKey(key);
        rde.setIntValue(new Long(value));
        return rde;
    }

    /**
     * Creates a RetsDataElement
     * 
     * @param path The path to the Table
     * @param value the value to store
     * @return an initialized RetsDataElement
     */
    private RetsDataElement createDataElement(String path, String value)
    {
        RetsDataElement rde = new RetsDataElement();
        Table key = (Table) mTables.get(path);
        rde.setKey(key);
        rde.setCharacterValue(value);
        return rde;
    }

    public void getData() throws HibernateException
    {
        Session session = mSessions.openSession();
        Transaction tx = session.beginTransaction();

        Query q =
            session.createQuery(
                "SELECT data"
                    + "  FROM org.realtors.rets.server.data.RetsData data");

        Iterator i = q.iterate();
        while (i.hasNext())
        {
            RetsData element = (RetsData) i.next();
            Map elements = element.getDataElements();
            RetsDataElement rde =
                (RetsDataElement) elements.get(
                    (Table) mTables.get("Property:RES:LP"));
        }

        tx.commit();
        session.close();
    }

    /**
     * @return
     */
    private String getNextAgent()
    {
        return mAgents[mACount++ % mAgents.length];
    }

    /**
     * @return
     */
    private String getNextBroker()
    {
        return mBrokers[mBCount++ % mBrokers.length];
    }

    /**
     * @return
     */
    private Date getNextDate()
    {
        return mDate[mDCount++ % mDate.length];
    }

    /**
     * @return ant int of the next school
     */
    private int getNextSchool()
    {
        return mESchools[mECount++ % mESchools.length];
    }
    
    private void initHibernate() throws HibernateException
    {
        Configuration cfg = new Configuration();
        cfg.addJar("retsdb2-hbm-xml.jar");
        mSessions = cfg.buildSessionFactory();
    }

    public void loadMetadata() throws HibernateException
    {
        Session session = mSessions.openSession();
        Transaction tx = session.beginTransaction();
        Query query = session.createQuery(
                "SELECT system" +
                "  FROM org.realtors.rets.server.metadata.MSystem system");
        Iterator i = query.iterate();
        while (i.hasNext())
        {
            MSystem system = (MSystem) i.next();
            System.out.println("Got system" + system.getId());
            Iterator j = system.getResources().iterator();
            while (j.hasNext())
            {
                Resource res = (Resource) j.next();
                Iterator k = res.getClasses().iterator();
                while (k.hasNext())
                {
                    MClass clazz = (MClass) k.next();
                    mClasses.put(clazz.getPath(), clazz);
                    Iterator l = clazz.getTables().iterator();
                    while (l.hasNext())
                    {
                        Table table = (Table) l.next();
                        mTables.put(table.getPath(), table);
                    }
                }
            }
        }
        tx.commit();
        session.close();
    }

    //  TODO: Add method to preload classes

    public static void main(String[] args) throws HibernateException
    {

        DataGenerator dg = new DataGenerator();
        long before = System.currentTimeMillis();
        dg.loadMetadata();
        long after = System.currentTimeMillis();
        System.out.print("Time to loadMetadata:");
        System.out.print(after - before);
        System.out.println("ms");

        String tmp = System.getProperty("add.data");
        if (tmp != null && tmp.equalsIgnoreCase("yes"))
        {
            before = System.currentTimeMillis();
            tmp = System.getProperty("prop.count");
            if (tmp == null)
            {
                dg.createData();
            }
            else
            {
                dg.createData(Integer.parseInt(tmp));
            }
            after = System.currentTimeMillis();
            System.out.print("Time to create data:");
            System.out.print(after - before);
            System.out.println("ms");
        }

        for (int i = 0; i < 10; i++)
        {
            before = System.currentTimeMillis();
            dg.getData();
            after = System.currentTimeMillis();
            System.out.print("Time to read data:");
            System.out.print(after - before);
            System.out.println("ms");
        }
    }

    private int mACount;
    private String[] mAgents;
    private int mBCount;
    private String[] mBrokers;
    private Map mClasses;
    private Date[] mDate;
    private int mDCount;
    private int mECount;
    private int[] mESchools;
    private int mHCount; 
    private int[] mHSchools;
    private Random mRandom;
    private SessionFactory mSessions;
    private Map mTables;
}
