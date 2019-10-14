package MerbyPackage;

import static MerbyPackage.Output.distributeLog;

public class Import 
{
    private boolean pathExists;
    private boolean tableExists;
    private int removeFolder;
    private int endOfTableName;
    private String tableToImport;
    private final String filePath;
    private SQLCommander tableImport;
    
    public Import(String path)
    {
        filePath = path;
    }
    
    public void importTable()
    {
        if(testPath())
        {
            tableImport = new SQLCommander();

            tableImport.setVersion();
            distributeLog("Checking if table exists. . .\n", 'B');
            
            if(testTable())
            {
                distributeLog("Attempting to import table. . .\n", 'B');
                tableImport.importTable(tableToImport, filePath);
            }

            tableImport.closeConnections();
        }
    }
    
    private boolean testPath()
    {
        pathExists = false;
        removeFolder = filePath.lastIndexOf("\\");
        endOfTableName = filePath.lastIndexOf("_MirVer_");
        
        if (removeFolder > 0 && endOfTableName > 0 && filePath.contains(".merby"))
        {
            distributeLog("Found valid Merby backup. . .\n",'B'); 

            tableToImport = filePath.substring(removeFolder + 1, endOfTableName);

            if (tableToImport.toUpperCase().equals("CHANNEL")
                || tableToImport.toUpperCase().equals("CODE_TEMPLATE")
                || tableToImport.toUpperCase().equals("CODE_TEMPLATE_LIBRARY")
                || tableToImport.toUpperCase().equals("CONFIGURATION"))
            {
                pathExists = true;
            }
            else
            {
                distributeLog("Unsupported tablename: '" + tableToImport + "' found!!! Cancelling backup. . .\n",'R');
            }
        }
        else
        {
            distributeLog("Invalid backup - file isn't named properly or isn't a backup!\n",'R');
            distributeLog("Filename should consist of: [tablename]\"_MirVer_\"[MirthVersionNumber]\".merby\"\n",'R');
        }
        
        return pathExists;
    }
    
    private boolean testTable()
    {
        tableExists = false;
        
        if(tableImport.getTableStatus(tableToImport))
        {
            distributeLog(tableToImport.toUpperCase() + " table exists!\n", 'G');

            if(filePath.contains("_MirVer_" + tableImport.getVersion()))
            {
                tableExists = true;
                
                if(tableImport.getTableContainsData(tableToImport))
                {
                    Output.addErrors(Output.EnumErrors.TABLE_CONTAINS_DATA);
                }
                
                tableImport.closeConnections();
            }
            else
            {
                distributeLog("Mirth version doesn't match! Expecting version: '" + tableImport.getVersion() + "'\n", 'R');
            }
        }
        else
        {
            distributeLog(tableToImport.toUpperCase() + " table doesn't exist!\n", 'R');
        }
        
        return tableExists;
    }
}
