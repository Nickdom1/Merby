package MerbyPackage;

import static MerbyPackage.Output.distributeLog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class Export 
{
    public static void exportEverything(Connection conn, String TodaysDate, String Dir)
    {
        String tableStatus = "";
        ArrayList<String> tables = new ArrayList<>();
        
        tables.add("CHANNEL");
        tables.add("CODE_TEMPLATE");
        tables.add("CODE_TEMPLATE_LIBRARY");
        tables.add("CONFIGURATION");
        
        //Each object here will be used to communicate to/from the database tables.
        SQLCommander chan = new SQLCommander("ChannelBackup", TodaysDate, Dir);
        SQLCommander codeTemp = new SQLCommander("CodeTemplateBackup", TodaysDate, Dir);
        SQLCommander tableExport = new SQLCommander("MerbyTableBackup", TodaysDate, Dir);
        
        tableExport.setVersion();
        
        distributeLog("Checking if tables exist and have data...\n", 'B');

        for(int i=0; i<tables.size(); i++)
        {
            if(tableExport.getTableStatus(tables.get(i)))
            {                
                if(tableExport.getTableContainsData(tables.get(i)))
                {
                    tableStatus = tableStatus + tables.get(i) + " table exists and contains data! ... ";
                }
                else
                {
                    tableStatus = tableStatus + tables.get(i) + " table exists but DOES NOT contain data! ... ";
                    tables.set(i, "");
                }
            }
            else
            {
                tableStatus = tableStatus + tables.get(i) + " table DOES NOT exist! ... ";
                tables.set(i, "");
            }
        }
        
        tables.removeAll(Arrays.asList(""));
        
        distributeLog(tableStatus, 'B');
        
        //Checks to see if table exists before exporting channel files.
        if(tables.contains("CHANNEL"))
        {
            chan.setData("CHANNEL");
            chan.setXmlColumn();
            
            if (chan.getXmlColumn() > 0)
            {
                createDirectory(chan.getPath());
                distributeLog("Backing up Channels...\n", 'B');
                exportXML(chan.getResults(), chan.getXmlColumn(), chan.getPath());
            }
            else
            {
                distributeLog("This version of Mirth is incompatible with Merby Channel Export!!! Use MerbyTableBackup (below) to recover channels.\n", 'R');
            }
        }
        
        //Checks to see if table exists before exporting code template files.
        if(tables.contains("CODE_TEMPLATE"))
        {   
            codeTemp.setData("CODE_TEMPLATE");
            
            createDirectory(codeTemp.getPath());
            codeTemp.setXmlColumn();

            //Display backing up code templates
            distributeLog("Backing up Code Templates...\n", 'B');
            exportXML(codeTemp.getResults(), codeTemp.getXmlColumn(), codeTemp.getPath());
        }
                
        boolean exportTable = true;
        
        if(tables.size() > 0)
        {
            distributeLog("Exporting Merby Table Backups...\n", 'B');
            createDirectory(tableExport.getPath());

            for (String table : tables) 
            {
                if(!tableExport.exportTable(table))
                {
                    exportTable = false;
                }             
            }
        }
        else
        {
            exportTable = false;
            distributeLog("No tables found to backup! Mirth database appears to be empty!\n", 'R');
        }

        if(!exportTable)
        {
            ClassManager.checkServiceStatus();
            Output.addErrors(Output.EnumErrors.TABLE_EXPORT_FAILED);
        }
        
        tableExport.closeConnections();
        chan.closeConnections();
        codeTemp.closeConnections();
    }
    
    //exportXML exports channels/code templates as XML files.
    private static void exportXML(ResultSet res, int clobCol, String writePath)
    {
        String fileName;
        String XMLdata;
        try
        {
            //Traverses rows in the object copy of the channel/code template table using pointer.
            while(res.next())
                {
                    //The name of the channel/code template. As far as I can tell it's almost always column 2.
                    fileName = res.getString(2);
                    //Added following code after finding there are cases where Column 2 is the XML data and not the name.
                    if (clobCol == 2 || fileName.length() > 100)
                    {
                        //Uses the character position of the name tag in the XML data to isolate the name.
                        int pos1 = fileName.indexOf("<name>") + 6;
                        int pos2 = fileName.indexOf("</name>");
                        fileName = fileName.substring(pos1, pos2);
                    }
                    //Found that Mirth allows you to enter values that Windows does not allow in file names - have to account for this or it causes issues writing the file.
                    //I'm replacing the value with something obvious we/the user should be able to find.
                    fileName = fileName.replace("/", "-FW_SLASH-").replace("\\", "-BK_SLASH-").replace(":", "-COLON-").replace("*", "-ASTERISK-").replace("?", "-QUESTION_MARK-").replace("\"","-QUOT_MARK-").replace("<","-LESS_THAN-").replace(">","-GREATER_THAN-").replace("|","-VERTICAL_BAR-");
                    //Used to turn the clob into a clob object and parse that into a string, but figured out it can be converted straight into a string.
                    XMLdata = res.getString(clobCol);

                    try(PrintWriter XMLout = new PrintWriter(writePath + "\\" + fileName + ".xml"))
                    {
                        XMLout.println(XMLdata);
                        XMLout.close();
                        
                        distributeLog("Backed up File: " + writePath + "\\" + fileName + ".xml\n", 'G');
                    }
                    catch (FileNotFoundException fileExcept2)
                    {
                        distributeLog("Unable to write File!\n", 'R');
                    }
                }
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
    }
    public static void createDirectory(String chanPath)
    {
        try
        {
            distributeLog("Checking if Directory exists...\n", 'B');
            File file = new File(chanPath);
            
            if (file.exists()) 
            {
                distributeLog("Found directory: " + chanPath + "\n", 'B');
            }
            else
            {
                distributeLog("Directory does not exist. Attempting to create Directory...\n", 'B');
                if (file.mkdirs()) 
                {
                    distributeLog("Created Directory: " + chanPath +"\n", 'G');
                } 
                else 
                {
                    distributeLog("Failed to create Directory\n", 'R');
                }
            }
        }
        catch (Exception fileError)
        {
            distributeLog(String.format("%1.298s", fileError.toString()) + "\n", 'R');
        }
    }
}
