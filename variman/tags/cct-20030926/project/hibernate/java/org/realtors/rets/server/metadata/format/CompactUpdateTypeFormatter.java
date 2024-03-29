/*
 */
package org.realtors.rets.server.metadata.format;

import java.io.PrintWriter;
import java.util.List;

import org.realtors.rets.server.metadata.UpdateType;

public class CompactUpdateTypeFormatter extends UpdateTypeFormatter
{
    public void format(PrintWriter out, List updateTypes)
    {
        if (updateTypes.size() == 0)
        {
            return;
        }
        TagBuilder tag = new TagBuilder(out);
        tag.begin("METADATA-UPDATE_TYPE");
        tag.appendAttribute("Resource", mResourceName);
        tag.appendAttribute("Class", mClassName);
        tag.appendAttribute("Update", mUpdateName);
        tag.appendAttribute("Version", mVersion);
        tag.appendAttribute("Date", mDate);
        tag.endAttributes();
        tag.appendColumns(COLUMNS);
        for (int i = 0; i < updateTypes.size(); i++)
        {
            UpdateType updateType = (UpdateType) updateTypes.get(i);
            apppendDataRow(out, updateType);
        }
        tag.end();
    }

    private void apppendDataRow(PrintWriter out, UpdateType updateType)
    {
        DataRowBuilder row = new DataRowBuilder(out);
        row.begin();
        row.append(updateType.getTable().getSystemName());
        row.append(updateType.getSequence());
        row.append(updateType.getAttributes());
        row.append(updateType.getDefault());
        row.append(updateType.getValidationExpressions());
        row.append(updateType.getUpdateHelp());
        row.append(updateType.getValidationLookup());
        row.append(updateType.getValidationExternal());
        row.end();
    }

    private static final String[] COLUMNS = new String[] {
        "SystemName", "Sequence", "Attributes", "Default",
        "ValidationExpressionID", "UpdateHelpID", "ValidationLookupName",
        "ValidationExternalName",
    };
}
