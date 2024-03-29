package org.realtors.rets.server.importer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;

import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.MappingException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import net.sf.hibernate.cfg.Configuration;

import org.realtors.rets.client.Metadata;
import org.realtors.rets.client.MetadataTable;
import org.realtors.rets.client.RetsException;
import org.realtors.rets.client.RetsSession;

import org.realtors.rets.server.SessionHelper;
import org.realtors.rets.server.metadata.AlignmentEnum;
import org.realtors.rets.server.metadata.ClassStandardNameEnum;
import org.realtors.rets.server.metadata.DataTypeEnum;
import org.realtors.rets.server.metadata.EditMask;
import org.realtors.rets.server.metadata.ForeignKey;
import org.realtors.rets.server.metadata.InterpretationEnum;
import org.realtors.rets.server.metadata.Lookup;
import org.realtors.rets.server.metadata.LookupType;
import org.realtors.rets.server.metadata.MClass;
import org.realtors.rets.server.metadata.MObject;
import org.realtors.rets.server.metadata.MSystem;
import org.realtors.rets.server.metadata.ObjectTypeEnum;
import org.realtors.rets.server.metadata.Resource;
import org.realtors.rets.server.metadata.ResourceStandardNameEnum;
import org.realtors.rets.server.metadata.SearchHelp;
import org.realtors.rets.server.metadata.Table;
import org.realtors.rets.server.metadata.TableStandardName;
import org.realtors.rets.server.metadata.UnitEnum;
import org.realtors.rets.server.metadata.Update;
import org.realtors.rets.server.metadata.UpdateHelp;
import org.realtors.rets.server.metadata.UpdateType;
import org.realtors.rets.server.metadata.UpdateTypeAttributeEnum;
import org.realtors.rets.server.metadata.ValidationExpression;
import org.realtors.rets.server.metadata.ValidationExpressionTypeEnum;
import org.realtors.rets.server.metadata.ValidationExternal;
import org.realtors.rets.server.metadata.ValidationExternalType;
import org.realtors.rets.server.metadata.ValidationLookup;
import org.realtors.rets.server.metadata.ValidationLookupType;


public class MetadataImporter extends MetadataHelpers
{
    /**
     * Creates a new <code>MetadataImporter</code> instance.
     *
     */
    public MetadataImporter()
        throws Exception
    {
        super();
        initHibernate();
        mFilename = null;
    }

    private boolean boolValue(String bString)
    {
        return bString.equalsIgnoreCase("true") || 
               bString.equalsIgnoreCase("1");
    }
    
    public void closeRetsSession()
        throws RetsException
    {
        mRetsSession.logout();
    }

    private void doClasses(Session hSession)
        throws HibernateException
    {
        MetadataTable tClass = getMetadataTable(MetadataTable.CLASS);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hClasses = new HashSet();
            List classes = tClass.getDataRows(resource.getPath());
            Iterator j = classes.iterator();
            while (j.hasNext())
            {
                Metadata md = (Metadata) j.next();
                MClass hClass = new MClass();

                hClass.setResource(resource);
                String className = md.getAttribute("ClassName");
                hClass.setClassName(className);
                hClass.setStandardName(ClassStandardNameEnum.fromString(
                                           md.getAttribute("StandardName")));
                hClass.setVisibleName(md.getAttribute("VisibleName"));
                hClass.setDescription(md.getAttribute("Description"));
                
                StringBuffer tmp = new StringBuffer("rets_");
                tmp.append(resource.getResourceID()).append("_");
                tmp.append(hClass.getClassName());
                hClass.setDbTable(tmp.toString().toLowerCase());

                hClass.updateLevel();

                hSession.save(hClass);
                hClasses.add(hClass);
                mClasses.put(hClass.getPath(), hClass);
            }

            resource.setClasses(hClasses);
            hSession.saveOrUpdate(resource);
        }
    }
    
    private void doEditMask(Session hSession)
        throws HibernateException
    {
        MetadataTable tEditMask = getMetadataTable(MetadataTable.EDITMASK);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hEditMasks = new HashSet();
            List editMasks = tEditMask.getDataRows(resource.getPath());
            if (editMasks != null)
            {
                Iterator j = editMasks.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    EditMask hEditMask = new EditMask();

                    hEditMask.setResource(resource);
                    hEditMask.setEditMaskID(md.getAttribute("EditMaskID"));
                    hEditMask.setValue(md.getAttribute("Value"));

                    hEditMask.updateLevel();

                    hSession.save(hEditMask);
                    hEditMasks.add(hEditMask);
                    mEditMasks.put(hEditMask.getPath(), hEditMask);
                }
            }
            resource.setEditMasks(hEditMasks);
            hSession.saveOrUpdate(resource);
        }
    }

    private void doForeignKey(Session hSession, MSystem hSystem)
        throws HibernateException
    {
        MetadataTable tForeignKeys =
            getMetadataTable(MetadataTable.FOREIGN_KEYS);

        Set hForeignKeys = new HashSet();
        List foreignKeys = tForeignKeys.getDataRows("");
        if (foreignKeys != null)
        {
            Iterator i = foreignKeys.iterator();
            while (i.hasNext())
            {
                Metadata md = (Metadata) i.next();
                ForeignKey hFk = new ForeignKey();

                hFk.setSystem(hSystem);

                hFk.setForeignKeyID(md.getAttribute("ForeignKeyID"));
                String path[] = new String[3];
                path[0] = md.getAttribute("ParentResourceID");
                path[1] = md.getAttribute("ParentClassID");
                path[2] = md.getAttribute("ParentSystemName");
                String tablePath = StringUtils.join(path, ":");
                Table table = (Table) mTables.get(tablePath);
                hFk.setParentTable(table);

                path[0] = md.getAttribute("ChildResourceID");
                path[1] = md.getAttribute("ChildClassID");
                path[2] = md.getAttribute("ChildSystemName");
                tablePath = StringUtils.join(path, ":");
                table = (Table) mTables.get(tablePath);
                hFk.setChildTable(table);
                if (table == null)
                {
                    LOG.error("table is null for path: " + tablePath);
                }

                hSession.save(hFk);
                hForeignKeys.add(hFk);
            }
        }
        hSystem.setForeignKeys(hForeignKeys);
        hSession.saveOrUpdate(hSystem);
    }
    
    public void doIt()
        throws Exception
    {
        if (mFilename == null)
        {
            getRetsSession();
        }
        else
        {
            InputStream in = new FileInputStream(mFilename);
            loadMetadataTables(in);
        }
        
        parseMetadata();

        if (mFilename == null)
        {
            closeRetsSession();
        }
    }

    private void doLookup(Session hSession)
        throws HibernateException
    {
        MetadataTable tLookup = getMetadataTable(MetadataTable.LOOKUP);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hLookups = new HashSet();
            List lookups = tLookup.getDataRows(resource.getPath());
            if (lookups != null)
            {
                Iterator j = lookups.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    Lookup hLookup = new Lookup();

                    hLookup.setResource(resource);

                    String lookupName = md.getAttribute("LookupName");
                    hLookup.setLookupName(lookupName);

                    hLookup.setVisibleName(md.getAttribute("VisibleName"));

                    hLookup.updateLevel();
                    hSession.save(hLookup);
                    // doLookupTypes(hLookup, rSession, hSession);
                    hLookups.add(hLookup);
                    mLookups.put(hLookup.getPath(), hLookup);
                }
            }
            resource.setLookups(hLookups);
            hSession.saveOrUpdate(resource);
        }
    }

    /**
     * Meant to be called only from do Lookup.
     *
     * @param hSession the hibernate session
     * @exception HibernateException if an error occurs
     */
    private void doLookupTypes(Session hSession)
        throws HibernateException
    {
        MetadataTable tLookupTypes =
            getMetadataTable(MetadataTable.LOOKUP_TYPE);

        Iterator i = mLookups.values().iterator();
        while (i.hasNext())
        {
            Lookup lookup = (Lookup) i.next();
            List lookupTypes = tLookupTypes.getDataRows(lookup.getPath());
            Set hLookupTypes = new HashSet();
            if (lookupTypes != null)
            {
                Iterator j = lookupTypes.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    LookupType hLookupType = new LookupType();

                    hLookupType.setLookup(lookup);

                    hLookupType.setLongValue(md.getAttribute("LongValue"));
                    hLookupType.setShortValue(md.getAttribute("ShortValue"));
                    hLookupType.setValue(md.getAttribute("Value"));

                    hLookupType.updateLevel();

                    hSession.save(hLookupType);
                    hLookupTypes.add(hLookupType);
                }
            }
            lookup.setLookupTypes(hLookupTypes);
            hSession.saveOrUpdate(lookup);
        }
    }

    private void doObjects(Session hSession)
        throws HibernateException
    {
        MetadataTable tObject = getMetadataTable(MetadataTable.OBJECT);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hObjects = new HashSet();
            List objects = tObject.getDataRows(resource.getPath());
            if (objects != null)
            {
                Iterator j = objects.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    MObject hObject = new MObject();

                    hObject.setResource(resource);

                    hObject.setObjectType(ObjectTypeEnum.fromString(
                                              md.getAttribute("ObjectType")));
                    hObject.setMimeType(md.getAttribute("MimeType"));
                    hObject.setVisibleName(md.getAttribute("VisibleName"));
                    hObject.setDescription(
                        StringUtils.substring(
                            md.getAttribute("Description"), 0, 64));

                    // Should we have an updateLevel?

                    hSession.save(hObject);
                    hObjects.add(hObject);
                }
            }

            resource.setObjects(hObjects);
            hSession.saveOrUpdate(resource);
        }
    }

    private void doResource(Session hSession, MSystem hSystem)
        throws HibernateException
    {
        MetadataTable tResource = getMetadataTable(MetadataTable.RESOURCE);

        Set hResources = new HashSet();
        List resources = tResource.getDataRows("");
        Iterator i = resources.iterator();
        while (i.hasNext())
        {
            Metadata md = (Metadata) i.next();
            Resource hResource = new Resource();

            hResource.setSystem(hSystem);
            String resourceID = md.getAttribute("ResourceID");
            hResource.setResourceID(resourceID);
            hResource.setStandardName(ResourceStandardNameEnum.fromString(
                                          md.getAttribute("StandardName")));
            hResource.setVisibleName(md.getAttribute("VisibleName"));
            hResource.setDescription(md.getAttribute("Description"));
            hResource.setKeyField(md.getAttribute("KeyField"));

            hResource.updateLevel();

            hSession.save(hResource);
            hResources.add(hResource);
            mResources.put(hResource.getPath(), hResource);
        }

        hSystem.setResources(hResources);
        hSession.saveOrUpdate(hSystem);
    }

    private void doSearchHelp(Session hSession)
        throws HibernateException
    {
        MetadataTable tSearchHelp =
            getMetadataTable(MetadataTable.SEARCH_HELP);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hSearchHelps = new HashSet();
            List searchHelps =
                tSearchHelp.getDataRows(resource.getPath());
            if (searchHelps != null)
            {
                Iterator j = searchHelps.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    SearchHelp hSearchHelp = new SearchHelp();

                    hSearchHelp.setResource(resource);
                    hSearchHelp.setSearchHelpID(
                        md.getAttribute("SearchHelpID"));
                    hSearchHelp.setValue(md.getAttribute("Value"));

                    hSearchHelp.updateLevel();

                    hSession.save(hSearchHelp);
                    hSearchHelps.add(hSearchHelp);
                    mSearchHelps.put(hSearchHelp.getPath(), hSearchHelp);
                }
            }

            resource.setSearchHelps(hSearchHelps);
            hSession.saveOrUpdate(resource);
        }
    }

    private MSystem doSystem(Session hSession)
        throws HibernateException
    {
        MSystem hSystem = new MSystem();
        hSystem.setVersion(101001);
        hSystem.setDate(Calendar.getInstance().getTime());
        hSession.save(hSystem);
        return hSystem;
    }

    private void doTable(Session hSession)
        throws HibernateException
    {
        MetadataTable tTables = getMetadataTable(MetadataTable.TABLE);

        Iterator i = mClasses.values().iterator();
        while (i.hasNext())
        {
            MClass hClass = (MClass) i.next();
            Set hTables = new HashSet();
            List tables = tTables.getDataRows(hClass.getPath());
            if (tables != null)
            {
                Iterator j = tables.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    Table hTable = new Table();

                    hTable.setMClass(hClass);

                    hTable.setSystemName(md.getAttribute("SystemName"));

                    String standardName = md.getAttribute("StandardName");
                    TableStandardName tsn = 
                        lookupTableStandardName(standardName);
                    if (tsn == null)
                    {
                        if (!standardName.equals(""))
                        {
                            tsn = new TableStandardName(standardName);
                            mTableStandardNames.put(standardName, tsn);
                            hSession.save(tsn);
                        }
                    }
                    hTable.setStandardName(tsn);
                        
                    hTable.setLongName(md.getAttribute("LongName"));

                    String tmp = md.getAttribute("DbName").toLowerCase();
                    if (tmp.startsWith("r_"))
                    {
                        hTable.setDbName(StringUtils.substring(tmp, 0, 10));
                    }
                    else
                    {
                        hTable.setDbName(
                            StringUtils.substring("r_" + tmp, 0, 10));
                    }                        
                        
                    hTable.setShortName(md.getAttribute("ShortName"));
                    hTable.setMaximumLength(
                        Integer.parseInt(md.getAttribute("MaximumLength")));
                    hTable.setDataType(
                        DataTypeEnum.fromString(md.getAttribute("DataType")));
                    hTable.setPrecision(
                        Integer.parseInt(md.getAttribute("Precision")));
                    hTable.setSearchable(
                        boolValue(md.getAttribute("Searchable")));
                    hTable.setInterpretation(
                        InterpretationEnum.fromString(
                            md.getAttribute("Interpretation")));
                    hTable.setAlignment(
                        AlignmentEnum.fromString(
                            md.getAttribute("Alignment")));
                    hTable.setUseSeparator(
                        boolValue(md.getAttribute("UseSeparator")));

                    String editMasksJoined = md.getAttribute("EditMaskID");
                    String resourcePath =  hClass.getResource().getPath();
                    String path = null;
                    Set hEditMasks = new HashSet();
                    if (editMasksJoined != null)
                    {
                        String editMasks[] =
                            StringUtils.split(editMasksJoined, ",");

                        for (int c = 0; c < editMasks.length; c++)
                        {
                            path = resourcePath + ":" +
                                StringUtils.trimToEmpty(editMasks[c]);
                            EditMask em = (EditMask) mEditMasks.get(path);
                            hEditMasks.add(em);
                            if (em == null)
                            {
                                LOG.error("edit mask null for path: " + path);
                            }
                        }
                    }
                    hTable.setEditMasks(hEditMasks);

                    String lookupName = md.getAttribute("LookupName");
                    path = resourcePath + ":" + lookupName;
                    Lookup lookup = (Lookup) mLookups.get(path);
                    hTable.setLookup(lookup);

                    hTable.setMaxSelect(
                        Integer.parseInt(md.getAttribute("MaxSelect")));

                    hTable.setUnits(
                        UnitEnum.fromString(md.getAttribute("Units")));

                    hTable.setIndex(
                        Integer.parseInt(md.getAttribute("Index")));

                    hTable.setMinimum(
                        Integer.parseInt(md.getAttribute("Minimum")));

                    hTable.setMaximum(
                        Integer.parseInt(md.getAttribute("Maximum")));

                    hTable.setDefault(
                        Integer.parseInt(md.getAttribute("Default")));

                    hTable.setRequired(
                        Integer.parseInt(md.getAttribute("Required")));

                    String searchHelpID = md.getAttribute("SearchHelpID");
                    path = resourcePath + ":" + searchHelpID;
                    SearchHelp searchHelp =
                        (SearchHelp) mSearchHelps.get(path);
                    hTable.setSearchHelp(searchHelp);

                    // String = md.getAttribute("unique");
                    hTable.setUnique(boolValue(md.getAttribute("Unique")));

                    hTable.updateLevel();

                    hSession.save(hTable);
                    hTables.add(hTable);
                    mTables.put(hTable.getPath(), hTable);
                }
                hClass.setTables(hTables);
                hSession.saveOrUpdate(hClass);
            }
        }
    }

    private void doUpdate(Session hSession)
        throws HibernateException
    {
        MetadataTable tUpdates = getMetadataTable(MetadataTable.UPDATE);

        Iterator i = mClasses.values().iterator();
        while (i.hasNext())
        {
            MClass clazz = (MClass) i.next();
            Set hUpdates = new HashSet();
            List updates = tUpdates.getDataRows(clazz.getPath());
            if (updates != null)
            {
                Iterator j = updates.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    Update hUpdate = new Update();

                    hUpdate.setMClass(clazz);

                    hUpdate.setUpdateName(md.getAttribute("UpdateName"));
                    hUpdate.setDescription(md.getAttribute("Description"));
                    hUpdate.setKeyField(md.getAttribute("KeyField"));

                    hUpdate.updateLevel();

                    hSession.save(hUpdate);
                    hUpdates.add(hUpdate);
                    mUpdates.put(hUpdate.getPath(), hUpdate);
                }
            }
            clazz.setUpdates(hUpdates);
            hSession.saveOrUpdate(clazz);
        }
    }

    private void doUpdateHelp(Session hSession)
        throws HibernateException
    {
        MetadataTable tUpdateHelps =
            getMetadataTable(MetadataTable.UPDATE_HELP);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hUpdateHelps = new HashSet();
            List updateHelps =
                tUpdateHelps.getDataRows(resource.getPath());
            if (updateHelps != null)
            {
                Iterator j = updateHelps.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    UpdateHelp hUpdateHelp = new UpdateHelp();

                    hUpdateHelp.setResource(resource);
                    hUpdateHelp.setUpdateHelpID(
                        md.getAttribute("UpdateHelpID"));
                    hUpdateHelp.setValue(md.getAttribute("Value"));

                    hUpdateHelp.updateLevel();

                    hSession.save(hUpdateHelp);
                    hUpdateHelps.add(hUpdateHelp);
                    mUpdateHelps.put(hUpdateHelp.getPath(), hUpdateHelp);
                }
            }
            resource.setUpdateHelps(hUpdateHelps);
            hSession.saveOrUpdate(resource);
        }
    }

    public void doUpdateType(Session hSession)
        throws HibernateException
    {
        MetadataTable tUpdateTypes =
            getMetadataTable(MetadataTable.UPDATE_TYPE);

        Iterator i = mUpdates.values().iterator();
        while (i.hasNext())
        {
            Update update = (Update) i.next();
            Set hUpdateTypes = new HashSet();
            List updateTypes = tUpdateTypes.getDataRows(update.getPath());
            if (updateTypes != null)
            {
                Iterator j = updateTypes.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    UpdateType updateType = new UpdateType();

                    updateType.setUpdate(update);
                    String level = update.getLevel();
                    String systemName = md.getAttribute("SystemName");
                    String tablePath = level + ":" + systemName;
                    Table table = (Table) mTables.get(tablePath);
                    updateType.setTable(table);
                    // Hack to get around metadata bug
                    if (table == null)
                    {
                        LOG.error("null table for path: " + tablePath);
                        System.exit(1);
                    }

                    updateType.setSequence(
                        Integer.parseInt(md.getAttribute("Sequence")));

                    String joinedAttributes = md.getAttribute("Attributes");
                    String attributes[] =
                        StringUtils.split(joinedAttributes, ",");
                    Set attributeSet = new HashSet();
                    for (int c = 0; c < attributes.length; c++)
                    {
                        attributeSet.add(UpdateTypeAttributeEnum.fromInt(
                                             Integer.parseInt(attributes[c])));
                    }
                    updateType.setAttributes(attributeSet);

                    updateType.setDefault(md.getAttribute("Default"));

                    String valExp[] = StringUtils.split(
                        md.getAttribute("ValidationExpressionID"), ",");
                    Set valExpSet = new HashSet();
                    String resourcePath =
                        update.getMClass().getResource().getPath();
                    for (int c = 0; c < valExp.length; c++)
                    {
                        String vePath = resourcePath + ":" + valExp[c];
                        ValidationExpression ve = (ValidationExpression)
                            mValidationExpressions.get(vePath);
                        valExpSet.add(ve);
                    }
                    updateType.setValidationExpressions(valExpSet);

                    String updateHelpPath = resourcePath + ":" +
                        md.getAttribute("UpdateHelpID");
                    updateType.setUpdateHelp(
                        (UpdateHelp) mUpdateHelps.get(updateHelpPath));

                    String vlPath = resourcePath + ":" +
                        md.getAttribute("ValdiationLookupName");
                    updateType.setValidationLookup(
                        (ValidationLookup) mValidationLookups.get(vlPath));

                    String vePath = resourcePath + ":" +
                        md.getAttribute("ValdationExternalName");
                    updateType.setValidationExternal(
                        (ValidationExternal) mValidationExternals.get(vePath));

                    hSession.save(updateType);
                    hUpdateTypes.add(updateType);
                }
            }
            update.setUpdateTypes(hUpdateTypes);
            hSession.saveOrUpdate(update);
        }
    }

    private void doValidationExpression(Session hSession)
        throws HibernateException
    {
        MetadataTable tValidationExpressions =
            getMetadataTable(MetadataTable.VALIDATION_EXPRESSION);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hValidationExpressions = new HashSet();
            List validationExpressions =
                tValidationExpressions.getDataRows(resource.getPath());
            if (validationExpressions != null)
            {
                Iterator j = validationExpressions.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    ValidationExpression ve = new ValidationExpression();

                    ve.setResource(resource);
                    ve.setValidationExpressionID(
                        md.getAttribute("ValidationExpressionID"));
                    ve.setValidationExpressionType(
                        ValidationExpressionTypeEnum.fromString(
                            md.getAttribute("ValidationExpressionType")));
                    ve.setValue(md.getAttribute("value"));

                    ve.updateLevel();

                    hSession.save(ve);
                    hValidationExpressions.add(ve);
                    mValidationExpressions.put(ve.getPath(), ve);
                }
            }
            resource.setValidationExpressions(hValidationExpressions);
            hSession.saveOrUpdate(resource);
        }
    }

    private void doValidationExternal(Session hSession)
        throws HibernateException
    {
        MetadataTable tValidationExternals =
            getMetadataTable(MetadataTable.VALIDATION_EXTERNAL);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hValidationExternals = new HashSet();
            List validationExternals =
                tValidationExternals.getDataRows(resource.getPath());
            if (validationExternals != null)
            {
                Iterator j = validationExternals.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    ValidationExternal hValidationExternal =
                        new ValidationExternal();

                    hValidationExternal.setResource(resource);

                    hValidationExternal.setValidationExternalName(
                        md.getAttribute("ValidationExternalName"));

                    // get the search class
                    String path = md.getAttribute("SearchResource") + ":" +
                        md.getAttribute("SearchClass");
                    MClass clazz = (MClass) mClasses.get(path);
                    hValidationExternal.setSearchClass(clazz);

                    hValidationExternal.updateLevel();

                    hSession.save(hValidationExternal);
                    hValidationExternals.add(hValidationExternal);
                    mValidationExternals.put(hValidationExternal.getPath(),
                                             hValidationExternal);
                }
            }
            resource.setValidationExternals(hValidationExternals);
            hSession.saveOrUpdate(resource);
        }
    }


    /**
     * Intended to be called by doValidationExternal.
     *
     * @param hSession the hibernate session
     * @exception HibernateException if an error occurs
     */
    private void doValidationExternalType(Session hSession)
        throws HibernateException
    {
        MetadataTable tValidationExternalTypes =
            getMetadataTable(MetadataTable.VALIDATION_EXTERNAL_TYPE);

        Iterator i = mValidationExternals.values().iterator();
        while (i.hasNext())
        {
            ValidationExternal ve = (ValidationExternal) i.next();
            Set hValdationExternalTypes = new HashSet();
            List validationExternalTypes =
                tValidationExternalTypes.getDataRows(ve.getPath());
            if (validationExternalTypes != null)
            {
                Iterator j = validationExternalTypes.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    ValidationExternalType vet = new ValidationExternalType();

                    vet.setValidationExternal(ve);

                    String joinedSearchField = md.getAttribute("SearchField");
                    String searchField[] =
                        StringUtils.split(joinedSearchField, ",");
                    Set searchFieldSet = new HashSet();
                    for (int c = 0; c < searchField.length; c++)
                    {
                        searchFieldSet.add(
                            StringUtils.trimToEmpty(searchField[c]));
                    }
                    vet.setSearchField(searchFieldSet);

                    String joinedDisplayField =
                        md.getAttribute("DisplayField");
                    String displayFields[] =
                        StringUtils.split(joinedDisplayField, ",");
                    Set displayFieldSet = new HashSet();
                    for (int c = 0; c < displayFields.length; c++)
                    {
                        displayFieldSet.add(
                            StringUtils.trimToEmpty(displayFields[c]));
                    }
                    vet.setDisplayField(displayFieldSet);

                    String joinedResultField =
                        md.getAttribute("ResultFields");
                    String resultFields[] =
                        StringUtils.split(joinedResultField, ",");
                    Map resultFieldMap = new HashMap();
                    for (int c = 0; c < resultFields.length; c++)
                    {
                        String split[] =
                            StringUtils.split(resultFields[c], "=", 2);
                        resultFieldMap.put(StringUtils.trimToEmpty(split[0]),
                                           StringUtils.trimToEmpty(split[1]));
                    }
                    vet.setResultFields(resultFieldMap);

                    vet.updateLevel();
                    hSession.save(vet);
                }
                ve.setValidationExternalTypes(hValdationExternalTypes);
            }
        }
    }

    /**
     * Requires that Table to be done.
     *
     * @param hSession the hibernate session
     * @exception HibernateException if an error occurs
     */
    private void doValidationLookup(Session hSession)
        throws HibernateException
    {
        MetadataTable tValidationLookups =
            getMetadataTable(MetadataTable.VALIDATION_LOOKUP);

        Iterator i = mResources.values().iterator();
        while (i.hasNext())
        {
            Resource resource = (Resource) i.next();
            Set hValidationLookups = new HashSet();
            String resourcePath = resource.getPath();
            List validationLookups =
                tValidationLookups.getDataRows(resourcePath);
            if (validationLookups != null)
            {
                Iterator j = validationLookups.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    ValidationLookup hvl = new ValidationLookup();

                    hvl.setResource(resource);

                    hvl.setValidationLookupName(
                        md.getAttribute("ValidationLookupName"));

                    hvl.setParent1Field(md.getAttribute("Parent1Field"));
                    hvl.setParent2Field(md.getAttribute("Parent2Field"));

                    hvl.updateLevel();

                    hSession.save(hvl);
                    hValidationLookups.add(hvl);
                    mValidationLookups.put(hvl.getPath(), hvl);
                }
                resource.setValidationLookups(hValidationLookups);
                hSession.saveOrUpdate(resource);
            }
        }
    }

    private void doValidationLookupType(Session hSession)
        throws HibernateException
    {
        MetadataTable tValidationLookupTypes =
            getMetadataTable(MetadataTable.VALIDATION_LOOKUP_TYPE);

        Iterator i = mValidationLookups.values().iterator();
        while (i.hasNext())
        {
            ValidationLookup vl = (ValidationLookup) i.next();
            Set hValdationLookupTypes = new HashSet();
            List validationLookupTypes =
                tValidationLookupTypes.getDataRows(vl.getPath());
            if (validationLookupTypes != null)
            {
                Iterator j = validationLookupTypes.iterator();
                while (j.hasNext())
                {
                    Metadata md = (Metadata) j.next();
                    ValidationLookupType vlt = new ValidationLookupType();

                    vlt.setValidationLookup(vl);
                    vlt.setValidText(md.getAttribute("ValidText"));
                    vlt.setParent1Value(md.getAttribute("Parent1Value"));
                    vlt.setParent2Value(md.getAttribute("Parent2Value"));

                    vlt.updateLevel();

                    hSession.save(vlt);
                    hValdationLookupTypes.add(vlt);
                }
            }
            vl.setValidationLookupTypes(hValdationLookupTypes);
            hSession.saveOrUpdate(vl);
        }
    }

    public void getRetsSession()
        throws RetsException
    {
        mRetsSession = new RetsSession(mConnectionURL);
        mRetsSession.login(mUsername, mPassword);
    }

    private MetadataTable getMetadataTable(String tableName)
    {
        if (mFilename == null)
        {
            return mRetsSession.getMetadataTable(tableName);
        }
        else
        {
            return mMetadataTables.getTable(tableName);
        }
    }

    private void initHibernate()
        throws MappingException, HibernateException
    {
        Configuration cfg = new Configuration();
        cfg.addJar("retsdb2-hbm-xml.jar");
        mSessions = cfg.buildSessionFactory();
    }
    
    protected TableStandardName lookupTableStandardName(String standardName)
    {
        TableStandardName name = super.lookupTableStandardName(standardName);
        if (name == null)
        {            
            SessionHelper helper = new SessionHelper(mSessions);
            try
            {
                Session session = helper.beginTransaction();
                List results = session.find(
                    "SELECT name" +
                    "  FROM TableStandardName name" +
                    " WHERE name.name = ?",
                    standardName, Hibernate.STRING);
                if (results.size() == 1)
                {
                    name = (TableStandardName) results.get(0);
                }
            }
            catch (HibernateException e)
            {
                helper.rollback(System.err);
            }
            finally
            {
                helper.close(System.err);
            }
            if (name != null)
            {
                mTableStandardNames.put(standardName, name);
            }
        }
        return name;
    }

    private void parseMetadata()
        throws Exception
    {
        Session hSession = null;
        Transaction tx = null;

        try
        {
            hSession = mSessions.openSession();
            tx = hSession.beginTransaction();
            deleteExistingMetadata();

            MSystem hSystem = doSystem(hSession);
            doResource(hSession, hSystem);
            doClasses(hSession);
            doObjects(hSession);
            doSearchHelp(hSession);
            doEditMask(hSession);
            doLookup(hSession);
            doLookupTypes(hSession);
            doUpdateHelp(hSession);
            doValidationExternal(hSession);
            doUpdate(hSession);
            doTable(hSession);
            doValidationLookup(hSession);
            doValidationLookupType(hSession);
            doValidationExternalType(hSession);
            doValidationExpression(hSession);
            doUpdateType(hSession);
            doForeignKey(hSession, hSystem);

            tx.commit();
            hSession.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                tx.rollback();
                hSession.close();
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
            }
        }
    }

    private void deleteExistingMetadata()
        throws HibernateException, SQLException
    {
        // The ordering of the tables is crucial. If deletion is done in the
        // wrong order, database constraints will be violated. This is
        // basically walks the metadata tree from the bottom up.
        String[] tables = new String[] {
            "rets_metadata_foreignkey",
            "rets_metadata_lookuptype",
            "rets_metadata_object",
            "rets_metadata_table_editmasks",
            "rets_metadata_table_standard_name",
            "rets_metadata_updatetype_attributes",
            "rets_metadata_updatetype_validationexpressions",
            "rets_metadata_validationexpression",
            "rets_metadata_validationexternaltype_displayfield",
            "rets_metadata_validationexternaltype_resultfields",
            "rets_metadata_validationexternaltype_searchfield",
            "rets_metadata_validationlookuptype",
            "rets_metadata_editmask",
            "rets_metadata_updatetype",
            "rets_metadata_validationexternaltype",
            "rets_metadata_validationlookup",
            "rets_metadata_table",
            "rets_metadata_update",
            "rets_metadata_updatehelp",
            "rets_metadata_validationexternal",
            "rets_metadata_class",
            "rets_metadata_lookup",
            "rets_metadata_searchhelp",
            "rets_metadata_resource",
            "rets_metadata_system",
        };

        Session session = null;
        Statement statement = null;
        Connection connection = null;
        try
        {
            session = mSessions.openSession();
            connection = session.connection();
            statement = connection.createStatement();
            for (int i = 0; i < tables.length; i++)
            {
                String table = tables[i];
                System.out.println("Deleting " + table);
                statement.executeUpdate("DELETE FROM " + table);
                statement.close();
            }
            connection.commit();
        }
        catch (SQLException e)
        {
            if (connection != null)
            {
                connection.rollback();
            }
        }
        finally
        {
            close(statement);
            close(session);
        }
    }

    private void close(Session session)
    {
        try
        {
            if (session != null)
            {
                session.close();
            }
        }
        catch (HibernateException e)
        {
            LOG.error("Caught", e);
        }
    }

    private void close(Statement statement)
    {
        try
        {
            if (statement != null)
            {
                statement.close();
            }
        }
        catch (SQLException e)
        {
            LOG.error("Caught", e);
        }
    }

    /**
     * 
     * @param string
     */
    public void setConnectionURL(String string)
    {
        mConnectionURL = string;
    }

    /**
     * 
     * @param filename
     */
    private void setFile(String filename)
    {
        mFilename = filename;
    }

    /**
     * 
     * @param string
     */
    public void setPassword(String string)
    {
        mPassword = string;
    }

    /**
     * 
     * @param string
     */
    public void setUsername(String string)
    {
        mUsername = string;
    }

    /**
     * 
     * @return
     */
    private static Options getOptions()
    {
        Options ops = new Options();
        ops.addOption("c", "url", true, "The URL to connect with");
        ops.addOption("u", "username", true, "username to connect with");
        ops.addOption("p", "password", true, "password to connect with");
        ops.addOption("f", "file", true, "get metadata from this file");
        return ops;
    }

    public static final void main(String args[])
        throws Exception
    {
        Parser parser = new GnuParser();
        Options opts = getOptions();
        CommandLine cmdl = null;
        try
        {
            cmdl = parser.parse(opts, args);
        }
        catch (Exception e)
        {
            printHelp(opts);
            System.exit(1);
        }
        
        MetadataImporter mi = new MetadataImporter();
        mi.setConnectionURL(
            cmdl.getOptionValue('c',
                                "http://demo.crt.realtors.org:6103/login"));
        mi.setUsername(cmdl.getOptionValue('u', "Joe"));
        mi.setPassword(cmdl.getOptionValue('p', "Schmoe"));
        if (cmdl.hasOption('f'))
        {
            mi.setFile(cmdl.getOptionValue('f'));
        }
        mi.doIt();
    }

    /**
     * 
     * @param opt
     */
    private static void printHelp(Options opt)
    {
        HelpFormatter fs = new HelpFormatter();
        fs.printHelp("MetadataImporter [options]", opt);
    }

    private String mConnectionURL;
    private String mFilename;
    private String mPassword;
    private RetsSession mRetsSession;
    private SessionFactory mSessions;
    private String mUsername;
    static final String CVSID =
        "$Id: MetadataImporter.java,v 1.38 2003/12/04 20:44:58 dribin Exp $";

    private static final Logger LOG = Logger.getLogger(MetadataImporter.class);

}
