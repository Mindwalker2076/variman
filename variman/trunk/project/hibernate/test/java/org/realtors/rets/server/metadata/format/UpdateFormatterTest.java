/*
 */
package org.realtors.rets.server.metadata.format;

import org.realtors.rets.server.metadata.Update;

public class UpdateFormatterTest extends FormatterTestCase
{
    protected void setUp()
    {
        Update update = new Update();
        update.setUpdateName("Add");
        update.setDescription("Add a new Residential Listing");
        update.setKeyField("key");
        mUpdates = new Update[] {update};
    }

    private UpdateFormatter getFormatter(int format)
    {
        UpdateFormatter formatter = UpdateFormatter.getInstance(format);
        formatter.setVersion("1.00.001", getDate());
        formatter.setClassName("RES");
        formatter.setResourceName("Property");
        return formatter;
    }

    public void testCompactFormatUpdate()
    {
        UpdateFormatter formatter = getFormatter(MetadataFormatter.COMPACT);
        String formatted = formatter.format(mUpdates);
        assertEquals(
            "<METADATA-UPDATE Resource=\"Property\" Class=\"RES\" " +
            "Version=\"" + VERSION + "\" Date=\"" + DATE + "\">\n" +

            "<COLUMNS>\tUpdateName\tDescription\tKeyField\tVersion\tDate\t" +
            "</COLUMNS>\n" +

            "<DATA>\tAdd\tAdd a new Residential Listing\tkey" + VERSION_DATE +
            "\t</DATA>\n" +

            "</METADATA-UPDATE>\n",
            formatted);
    }

    private Update[] mUpdates;
}