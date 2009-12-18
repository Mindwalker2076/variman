/*
 * Variman RETS Server
 *
 * Author: Dave Dribin
 * Copyright (c) 2004, The National Association of REALTORS
 * Distributed under a BSD-style license.  See LICENSE.TXT for details.
 */

/*
 */
package org.realtors.rets.server.protocol;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

import org.realtors.rets.common.metadata.types.MTable;
import org.realtors.rets.common.util.TagBuilder;
import org.realtors.rets.server.RetsServerException;
import org.realtors.rets.server.dmql.DmqlParserMetadata;
import org.realtors.rets.server.metadata.MetadataUtils;
import org.realtors.rets.server.metadata.UnitEnum;

public class ResidentialPropertyFormatter implements SearchResultsFormatter
{

    public void formatResults(SearchFormatterContext context)
        throws RetsServerException
    {
        try
        {
            PrintWriter out = context.getWriter();
            TagBuilder reData = new TagBuilder(out, "REData")
                .beginContentOnNewLine();
            TagBuilder reProperties = new TagBuilder(out, "REProperties")
                .beginContentOnNewLine();
            while (context.hasNext())
            {
                formatRow(context);
            }
            reProperties.close();
            reData.close();
        }
        catch (SQLException e)
        {
            throw new RetsServerException(e);
        }
    }

    private void formatRow(SearchFormatterContext context)
        throws SQLException
    {
    	// FIXME: This is a shell and needs to be fully implemented for 1.7.2
        PrintWriter out = context.getWriter();
        DmqlParserMetadata metadata = context.getMetadata();
        TagBuilder residentialProperty =
            new TagBuilder(out, "ResidentialProperty")
            .beginContentOnNewLine();
        TagBuilder listing = new TagBuilder(out, "Listing")
            .beginContentOnNewLine();
        new TagBuilder(out, "StreetAddress")
            .beginContentOnNewLine()
            .emptyTag("StreetNumber")
            .simpleTag("StreetName", context.getResultString("StreetName"))
            .simpleTag("PostalCode", context.getResultString("PostalCode"))
            .close();
        listing.close();
        TagBuilder livingArea = new TagBuilder(out, "LivingArea")
            .beginContentOnNewLine();
        TagBuilder area = new TagBuilder(out, "Area");
        MTable livingAreaTable = metadata.getTable("LivingArea");
        String units = getUnits(livingAreaTable);
        if (units != null)
        {
            area.appendAttribute("Units", units);
        }
        area.beginContent()
            .print(context.getResultString("LivingArea"))
            .close();
        livingArea.close();
        residentialProperty.close();
    }

    private String getUnits(MTable table)
    {
        if (table == null)
        {
            return null;
        }
        UnitEnum units = MetadataUtils.getUnitEnum(table);
        return UNITS_MAP.get(units);
    }

    private static final Map<UnitEnum, String> UNITS_MAP;

    static
    {
        UNITS_MAP = new HashMap<UnitEnum, String>();
        UNITS_MAP.put(UnitEnum.SQFT, "SqFeet");
        UNITS_MAP.put(UnitEnum.SQMETERS, "SqMeters");
        UNITS_MAP.put(UnitEnum.ACRES, "Acres");
        UNITS_MAP.put(UnitEnum.HECTARES, "Hectares");
    }

}
