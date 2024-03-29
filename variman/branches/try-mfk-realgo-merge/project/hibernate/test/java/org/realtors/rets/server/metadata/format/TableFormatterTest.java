/*
 */
package org.realtors.rets.server.metadata.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.realtors.rets.server.metadata.AlignmentEnum;
import org.realtors.rets.server.metadata.DataTypeEnum;
import org.realtors.rets.server.metadata.EditMask;
import org.realtors.rets.server.metadata.InterpretationEnum;
import org.realtors.rets.server.metadata.Lookup;
import org.realtors.rets.server.metadata.Table;
import org.realtors.rets.server.metadata.TableStandardName;
import org.realtors.rets.server.metadata.UnitEnum;
import org.realtors.rets.server.Group;
import org.realtors.rets.server.protocol.TableGroupFilter;
import org.realtors.rets.server.config.FilterRule;
import org.realtors.rets.server.config.FilterRuleImpl;
import org.realtors.rets.server.config.GroupRules;
import org.realtors.rets.server.config.GroupRulesImpl;

public class TableFormatterTest extends FormatterTestCase
{
    public TableFormatterTest()
    {
        HashSet allTables = new HashSet();

        mSchool = new Table(1);
        mSchool.setSystemName("E_SCHOOL");
        mSchool.setStandardName(new TableStandardName("ElementarySchool"));
        mSchool.setLongName("Elementary School");
        mSchool.setShortName("ElemSchool");
        mSchool.setDbName("E_SCHOOL");
        mSchool.setMaximumLength(4);
        mSchool.setDataType(DataTypeEnum.INT);
        mSchool.setPrecision(0);
        mSchool.setSearchable(true);
        mSchool.setInterpretation(InterpretationEnum.LOOKUP);
        mSchool.setAlignment(AlignmentEnum.LEFT);

        EditMask em1 = new EditMask(1);
        em1.setEditMaskID("EM1");
        EditMask em2 = new EditMask(2);
        em2.setEditMaskID("EM2");
        Set editMasks = new HashSet();
        editMasks.add(em1);
        editMasks.add(em2);
        mSchool.setEditMasks(editMasks);

        mSchool.setUseSeparator(false);
        Lookup lookup = new Lookup();
        lookup.setLookupName("E_SCHOOL");
        mSchool.setLookup(lookup);

        mSchool.setMaxSelect(1);
        mSchool.setUnits(UnitEnum.FEET);
        mSchool.setIndex(2);
        mSchool.setMinimum(3);
        mSchool.setMaximum(4);
        mSchool.setDefault(5);
        mSchool.setRequired(6);
        mSchool.setUnique(false);
        allTables.add(mSchool);

        mAgent = new Table(2);
        mAgent.setSystemName("AGENT_ID");
        mAgent.setStandardName(new TableStandardName("ListAgentAgentID"));
        mAgent.setLongName("Listing Agent ID");
        mAgent.setShortName("AgentID");
        mAgent.setDbName("AGENT_ID");
        mAgent.setMaximum(6);
        mAgent.setDataType(DataTypeEnum.CHARACTER);
        mAgent.setPrecision(0);
        mAgent.setSearchable(true);
        mAgent.setAlignment(AlignmentEnum.LEFT);
        mAgent.setMaxSelect(0);
        mAgent.setIndex(0);
        mAgent.setMinimum(0);
        mAgent.setMaximum(0);
        mAgent.setDefault(5);
        mAgent.setRequired(0);
        mAgent.setUnique(false);
        allTables.add(mAgent);

        mListingPrice = new Table(3);
        mListingPrice.setSystemName("LISTING_PRICE");
        mListingPrice.setStandardName(new TableStandardName("ListingPrice"));
        mListingPrice.setLongName("Listing Price");
        mListingPrice.setShortName("ListingPrice");
        mListingPrice.setDbName("LP");
        mListingPrice.setMaximum(6);
        mListingPrice.setDataType(DataTypeEnum.INT);
        mListingPrice.setPrecision(0);
        mListingPrice.setSearchable(true);
        mListingPrice.setAlignment(AlignmentEnum.LEFT);
        mListingPrice.setMaxSelect(0);
        mListingPrice.setIndex(0);
        mListingPrice.setMinimum(0);
        mListingPrice.setMaximum(0);
        mListingPrice.setDefault(5);
        mListingPrice.setRequired(0);
        mListingPrice.setUnique(false);
        allTables.add(mListingPrice);

        mGroupFilter = new TableGroupFilter();
        mGroupFilter.setTables("Property", "MOB", allTables);

        mNewspapers = new Group("Newspapers");
        mGroups = new HashSet();
        mGroups.add(mNewspapers);

        FilterRule filterRule = new FilterRuleImpl(FilterRule.Type.EXCLUDE);
        filterRule.setResourceID("Property");
        filterRule.setRetsClassName("MOB");
        filterRule.addSystemName("LISTING_PRICE");
        GroupRules rules = new GroupRulesImpl(mNewspapers);
        rules.addFilterRule(filterRule);
        mGroupFilter.addRules(rules);
    }

    protected List getData()
    {
        List tables = new ArrayList();
        tables.add(mSchool);
        tables.add(mAgent);
        tables.add(mListingPrice);
        return tables;
    }

    protected TableGroupFilter getGroupFilter()
    {
        return mGroupFilter;
    }

    protected Set getGroups()
    {
        return mGroups;
    }

    protected String[] getLevels()
    {
        return new String[] {"Property", "MOB"};
    }

    protected MetadataFormatter getCompactFormatter()
    {
        return new CompactTableFormatter();
    }

    protected MetadataFormatter getStandardFormatter()
    {
        return new StandardTableFormatter();
    }

    protected String getExpectedCompact()
    {
        return
            "<METADATA-TABLE Resource=\"Property\" Class=\"MOB\" " +
            "Version=\"" + VERSION + "\" Date=\"" + DATE + "\">\n" +

            "<COLUMNS>\t" +
            "SystemName\tStandardName\tLongName\tDBName\t" +
            "ShortName\tMaximumLength\tDataType\tPrecision\tSearchable\t" +
            "Interpretation\tAlignment\tUseSeparator\tEditMaskID\t" +
            "LookupName\tMaxSelect\tUnits\tIndex\tMinimum\tMaximum\tDefault\t" +
            "Required\tSearchHelpID\tUnique\t" +
            "</COLUMNS>\n" +

            "<DATA>\t" +
            "E_SCHOOL\tElementarySchool\tElementary School\t" +
            "E_SCHOOL\tElemSchool\t4\tInt\t0\t1\tLookup\tLeft\t0\tEM1,EM2\t" +
            "E_SCHOOL\t1\tFeet\t2\t3\t4\t5\t6\t\t0\t" +
            "</DATA>\n" +

            "<DATA>\t" +
            "AGENT_ID\tListAgentAgentID\tListing Agent ID\tAGENT_ID\t" +
            "AgentID\t0\tCharacter\t0\t1\t\tLeft\t0\t\t\t0\t\t0\t0\t0\t5" +
            "\t0\t\t0\t" +
            "</DATA>\n" +

            "</METADATA-TABLE>\n";
    }

    protected String getExpectedCompactRecursive()
    {
        return getExpectedCompact();
    }

    protected String getExpectedStandard()
    {
        return
            "<METADATA-TABLE Resource=\"Property\" Class=\"MOB\" " +
            "Version=\"" + VERSION + "\" Date=\"" + DATE + "\">" + EOL +
            "<Field>" + EOL +
            "<SystemName>E_SCHOOL</SystemName>" + EOL +
            "<StandardName>ElementarySchool</StandardName>" + EOL +
            "<LongName>Elementary School</LongName>" + EOL +
            "<DBName>E_SCHOOL</DBName>" + EOL +
            "<ShortName>ElemSchool</ShortName>" + EOL +
            "<MaximumLength>4</MaximumLength>" + EOL +
            "<DataType>Int</DataType>" + EOL +
            // Line 10
            "<Precision>0</Precision>" + EOL +
            "<Searchable>1</Searchable>" + EOL +
            "<Interpretation>Lookup</Interpretation>" + EOL +
            "<Alignment>Left</Alignment>" + EOL +
            "<UseSeparator>0</UseSeparator>" + EOL +
            "<EditMaskID>EM1,EM2</EditMaskID>" + EOL +
            "<LookupName>E_SCHOOL</LookupName>" + EOL +
            "<MaxSelect>1</MaxSelect>" + EOL +
            "<Units>Feet</Units>" + EOL +
            "<Index>2</Index>" + EOL +
            // Line 20
            "<Minimum>3</Minimum>" + EOL +
            "<Maximum>4</Maximum>" + EOL +
            "<Default>5</Default>" + EOL +
            "<Required>6</Required>" + EOL +
            "<SearchHelpID></SearchHelpID>" + EOL +
            "<Unique>0</Unique>" + EOL +
            "</Field>" + EOL +
            "<Field>" + EOL +
            "<SystemName>AGENT_ID</SystemName>" + EOL +
            "<StandardName>ListAgentAgentID</StandardName>" + EOL +
            //Line 30
            "<LongName>Listing Agent ID</LongName>" + EOL +
            "<DBName>AGENT_ID</DBName>" + EOL +
            "<ShortName>AgentID</ShortName>" + EOL +
            "<MaximumLength>0</MaximumLength>" + EOL +
            "<DataType>Character</DataType>" + EOL +
            "<Precision>0</Precision>" + EOL +
            "<Searchable>1</Searchable>" + EOL +
            "<Interpretation></Interpretation>" + EOL +
            "<Alignment>Left</Alignment>" + EOL +
            "<UseSeparator>0</UseSeparator>" + EOL +
            // Line 40
            "<EditMaskID></EditMaskID>" + EOL +
            "<LookupName></LookupName>" + EOL +
            "<MaxSelect>0</MaxSelect>" + EOL +
            "<Units></Units>" + EOL +
            "<Index>0</Index>" + EOL +
            "<Minimum>0</Minimum>" + EOL +
            "<Maximum>0</Maximum>" + EOL +
            "<Default>5</Default>" + EOL +
            "<Required>0</Required>" + EOL +
            "<SearchHelpID></SearchHelpID>" + EOL +
            // Line 50
            "<Unique>0</Unique>" + EOL +
            "</Field>" + EOL +
            "</METADATA-TABLE>" + EOL;
    }

    protected String getExpectedStandardRecursive()
    {
        return getExpectedStandard();
    }

    public void testCompactFormatIsEmptyIfAllTablesFiltered()
    {
        ArrayList data = new ArrayList();
        data.add(mListingPrice);
        String formatted = format(getCompactFormatter(), data, 
                                  getLevels(), FormatterContext.NOT_RECURSIVE);
        assertLinesEqual("", formatted);
    }

    private Table mSchool;
    private Table mAgent;
    private Table mListingPrice;
    private Group mNewspapers;
    private HashSet mGroups;
    private TableGroupFilter mGroupFilter;
}
