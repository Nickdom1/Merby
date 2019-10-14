package MerbyPackage;

import static MerbyPackage.Output.*;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClassManager
{
    private static int loginAttemptsLeft = 3;
    private static Connection conn = null;
    private static String dbURL = "jdbc:derby:C:\\Program Files\\Mirth Connect\\appdata\\mirthdb;";
    private static String servicePath;
    private static String overideMirthDB = "";
    private static String overideBackup = "";
    private static String actualPath = "";
    private static final String MERBPASS = (Integer.parseInt(getTodaysDate(3)) + 5) + "PICKLES";
    private static boolean connected = false; 
    
    public static void resetLoginAttempts()
    {
        loginAttemptsLeft = 3;
    }
    
    public static boolean getPasswordStatus(char pressedKey, char[] userInput)
    {
        boolean passwordMatches = false;
                
        if(pressedKey == '\n')
        {            
            if(String.valueOf(userInput).equalsIgnoreCase(MERBPASS))
            {
                passwordMatches = true;
                distributeLog("Password is correct, enabling buttons (click password field to lock Merby).\n", 'B');
            }
            else if (loginAttemptsLeft != 0)
            {
                distributeLog("Password is incorrect! " + loginAttemptsLeft + " attempts left!\n", 'R');
                loginAttemptsLeft = loginAttemptsLeft - 1;
            }
            else
            {
                distributeLog("Password is incorrect! " + loginAttemptsLeft + " attempts left! Shutting down program!\n", 'R');
                System.exit(0);
            }
        }
        
        return passwordMatches;
    }
    
    public static void startExport(Component centerScreen)
    {
        distributeLog("--- Export Initiated\n",'C');    
        
        if(createConnection(0))
        {
            Export.exportEverything(conn, getTodaysDate(0), overideBackup);
            shutdownConnection();
        }
        else
        {
            checkServiceStatus();
            distributeLog("--- Export Cancelled!\n",'C');
        }
        
        Output.displayErrors(centerScreen);
    }
    
    public static void startImport(String filePath, Component centerScreen)
    {
        distributeLog("--- Import Initiated\n",'C');
                
        if(filePath.length() > 0 && createConnection(0))
        {
            Import dataImporter = new Import(filePath);
            dataImporter.importTable();
            shutdownConnection();
        }
        else
        {
            checkServiceStatus();
            distributeLog("--- Import Cancelled!\n",'C');
        }
        
        checkWritePermissions(overideMirthDB + servicePath);
        displayErrors(centerScreen);
    }
    
    public static void repairDB(InputStream logFolder, Component centerScreen)
    {
        distributeLog("--- Repair Initiated\n",'C');

        servicePath = "";
                
        if (overideMirthDB.length() == 0)
        {
            setServicePath();
            actualPath = servicePath;
        }
        else
        {
            actualPath = overideMirthDB;
        }
        
        ArrayList<String> dbFolders = new ArrayList<>();
        
        distributeLog("Testing possible Mirth DB subfolders...\n",'B');
        
        if(directoryExists(actualPath + "appdata\\mirthdb"))
        {
            distributeLog("Found Mirth DB subfolder: " + actualPath + "appdata\\mirthdb" + "\n",'B');
            dbFolders.add(actualPath + "appdata\\mirthdb");
        }
        
        if(directoryExists(actualPath + "mirthdb"))
        {
            distributeLog("Found Mirth DB subfolder: " + actualPath + "mirthdb" + "\n",'B');
            dbFolders.add(actualPath + "mirthdb");
        }

        if (dbFolders.size() > 0)
        {
            for(String dbFolder : dbFolders)
            {
                if(directoryExists(dbFolder + "\\log"))
                {
                    distributeLog("Found Derby log folder: " + dbFolder + "\\log" + "\n",'B');
                    distributeLog("Attempting to rename folder...\n",'B');
                    renameDirectory(dbFolder + "\\log");
                }

                Export.createDirectory(dbFolder + "\\log");

                distributeLog("Attempting to create fresh database files...\n",'B');
                
                File file = new File("");
                
                try
                {
                    String [] logFiles = {"log.ctrl", "log1.dat", "logmirror.ctrl", "README_DO_NOT_TOUCH_FILES.txt"};
                    
                    for (String logFile : logFiles) 
                    {
                        file = new File(dbFolder + "\\log\\" + logFile);
                        Files.copy(logFolder.getClass().getResourceAsStream("/log/" + logFile), file.getAbsoluteFile().toPath());
                        distributeLog("Successfully saved file: " + file.getAbsoluteFile().toString() + "\n",'G');
                    }
                } 
                catch (IOException ex) 
                {
                    distributeLog("Failed to create file: " + file.getAbsoluteFile().toString() + "\n",'R');
                    distributeLog(ex.toString() + "\n",'R');
                }
            }
        }
        else
        {
            distributeLog("Cannot find Mirth DB subfolders!\n",'R');
        }

        checkWritePermissions(actualPath);
        checkServiceStatus();
        
        if(Output.errorsArePresent())
        {
            distributeLog("Repair finished but with some potential errors.\n",'B');
        }
        else
        {
            distributeLog("Repair finished.\n",'B');
        }
        
        displayErrors(centerScreen);
    }
    
    private static void checkWritePermissions(String pathToTest)
    {   
        File testAccess = new File(pathToTest);        

        if (!Files.isWritable(testAccess.toPath()))
        {
            Output.addErrors(Output.EnumErrors.WRITE_ACCESS);
        }
    }
    
    public static void checkServiceStatus()
    {
        String serviceStatus = "";
        
        try
        {    
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec("sc query \"mirth connect service\"");
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            
            do
            {
                serviceStatus=input.readLine();
            }
            while(!serviceStatus.contains("STATE") && !serviceStatus.contains("FAILED"));
        }
        catch (IOException runtimeError)
        {
            distributeLog(runtimeError.getMessage(), 'R');
        }
        
        if (!serviceStatus.contains("STOPPED"))
        {
            if (serviceStatus.contains("FAILED"))
            {
                Output.addErrors(Output.EnumErrors.SERVICE_NOT_INSTALLED);
            }
            else
            {
                Output.addErrors(Output.EnumErrors.SERVICE_STARTED);
            }            
        }
    }
    
    private static boolean directoryExists(String chanPath)
    {
        File file = new File(chanPath);
        return file.exists();
    }
    
    private static void renameDirectory(String logFolder)
    {
        String newlogFolder = logFolder + "_BACKUP_" + getTodaysDate(4);
        
        File dir = new File(logFolder);
        File newDir = new File(newlogFolder);
        
        try
        {
            Files.move(dir.toPath(), newDir.toPath());
            distributeLog("Renamed Directory: '" + logFolder + "' to: '" + newlogFolder + "'\n", 'G');
        }
        catch (IOException ex)
        {
            distributeLog("Failed to rename folder: " + ex.toString() + "\n",'R');
        }
    }
    
    public static void compressDB(Component centerScreen)
    {
        distributeLog("--- Compress Initiated\n",'C');
        
        if(createConnection(0))
        {
            SQLCommander compressor = new SQLCommander();
            List<String> uncompressedTables = compressor.getUncompressedTables();
            distributeLog("Found " + uncompressedTables.size() + " tables: " + uncompressedTables + "\n",'B');
            
            for(int i = 0; i<uncompressedTables.size(); i++)
            {
                compressor.compressDB(uncompressedTables.get(i), i+1);
            }
            
            shutdownConnection();
        }
        else
        {
            checkServiceStatus();
            distributeLog("--- Compress Cancelled!\n",'C');
        }
        
        checkWritePermissions(overideMirthDB + servicePath);
        displayErrors(centerScreen);
    }
    
    public static String [] getUserName(Component centerScreen)
    {
        distributeLog("--- Username Query Initiated\n",'C');
        
        String [] credStrings = new String[2];
        
        if(createConnection(0))
        {
            distributeLog("Querying Derby Database for Mirth Username...\n", 'B');
            SQLCommander cred = new SQLCommander();
            credStrings = cred.getCredentials();
            
            if(credStrings[0].length() > 0)
            {
                distributeLog("Found Username: '" + credStrings[0] + "'\n", 'G');
            }
            else
            {
                distributeLog("No User Found!\n", 'R');
            }
            
            if(credStrings[1].length() > 0)
            {
                distributeLog("Found (MD5) Password: '" + credStrings[1] + "'\n", 'G');
            }
            else
            {
                distributeLog("No Password Found!\n", 'R');
            }
            
            cred.closeConnections();
            shutdownConnection();
        }
        else
        {
            checkServiceStatus();
            displayErrors(centerScreen);
            credStrings[0] = "No User Found";
            credStrings[1] = "No Password Found";
        }
        
        return credStrings;
    }
    
    public static void resetPassword(Component centerScreen)
    {
        distributeLog("--- Password Reset Initiated\n",'C');
        
        if(createConnection(0))
        {
            distributeLog("Running Reset command against Derby Database...\n", 'B');
            SQLCommander reset = new SQLCommander();
            reset.resetCredentials();
            reset.closeConnections();
            shutdownConnection();
        }
        else
        {
            checkServiceStatus();
            distributeLog("--- Password Reset Cancelled!\n",'C');
        }
        
        checkWritePermissions(overideMirthDB + servicePath);
        displayErrors(centerScreen);
    }
    
    public static String getTodaysDate(int form)
    {
        Date dateNow = new Date();
        
        switch (form)
        {
            case 0:
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat ("MM-dd-yyyy");
                return dateFormat.format(dateNow);
            }
            case 1:
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat ("mm:ss.SSS");
                return dateFormat.format(dateNow);
            }
            case 2:
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat ("MM-dd-yyyy|HH:mm|ss.SSS");
                return dateFormat.format(dateNow);
            }
            case 3:
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat ("dd");
                return dateFormat.format(dateNow);
            }
            case 4:
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat ("MM-dd-yyyy-HH-mm-ss-SSS");
                return dateFormat.format(dateNow);
            }
            default:
            {
                return "BAD_DATE";
            }
        }
    }
    
    private static void setServicePath()
    {
        try
        {    
            //The purpose of this section is to find the correct path for the Derby database using command prompt and the path for mcservice.exe
            Runtime rt = Runtime.getRuntime();
            //Actual command sent to command line.
            Process pr = rt.exec("sc qc \"Mirth Connect Service\"");
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            //BPN is the actual path to the service file. I'm using this to find the installed Mirth Directory.
            //There's several lines in the output from command line - so the only way to find the BPN is traverse each line.
            do
            {
                servicePath=input.readLine();
            }
            while(!servicePath.contains("BINARY_PATH_NAME") && !servicePath.contains("FAILED"));
        }
        catch (IOException runtimeError)
        {
            distributeLog(runtimeError.getMessage(), 'R');
        }
        
        if (servicePath.contains("BINARY_PATH_NAME"))
        {
            //Using following character positions to cut out all of the non-path information/the executable name.
            int pos1 = servicePath.indexOf(":\\") - 1;
            int pos2 = servicePath.indexOf("\\mirthc") + 1;
            
            if (pos2 < 1)
            {
                pos2 = servicePath.indexOf("\\mcserv") + 1;
            }
            
            if(pos1 > 0 && pos2 > 0)
            {
                servicePath = servicePath.substring(pos1,pos2);
            }
            else
            {
                distributeLog("Error parsing path: '" + servicePath + "' for Mirth Service! Service exists but has unexpected path!\n", 'R');
                servicePath = "";
            }            
        }
        else
        {
            distributeLog("Cannot find Mirth Connect Service! Mirth may not be installed!\n", 'R');
            servicePath = "";
        }
    }
    
    private static boolean createConnection(int connect)
    {
        String subFolder;
        
        servicePath = "";
        //Using following if/else to change the subfolder we're looking for. Mirth moves the database around version to version.
        if (connect == 0)
        {
            subFolder = "appdata\\mirthdb;";
        }
        else
        {
            subFolder = "mirthdb;";
        }
        
        if (overideMirthDB.length() == 0)
        {
            setServicePath();
        }
        
        if (servicePath.length() > 0 || overideMirthDB.length() > 0)
        {
            if (servicePath.length() > 0)
            {
                dbURL = "jdbc:derby:" + servicePath + subFolder;
            }
            else
            {
                dbURL = "jdbc:derby:" + overideMirthDB + subFolder;
            }

            try
            {
                distributeLog("Connecting to Derby Database... [" + dbURL + "]\n", 'B');

                Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
                conn = DriverManager.getConnection(dbURL); 

                distributeLog("Connection Successful! Connected to: [" + dbURL + "]\n", 'G');

                connected = true;

            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException except)
            {
                if (connect < 1)
                {
                    distributeLog("Connection failed! Attempting another subfolder...\n", 'R');
                    
                    Output.outputToLogFile(except.toString() + "\n");
                    createConnection(connect + 1);
                }
                else
                {
                    Output.outputToLogFile(except.toString() + "\n");
                    
                    distributeLog("Connection failed!\n", 'R');
                    distributeLog("Unable to connect to Derby Database. Check to see if service is still running.\n", 'B');
                    connected = false;
                }
            }
        }
        return connected;
    }
    
    public static void overideMirthDB(String inputPath)
    {
        overideMirthDB = inputPath;
    }
    
    public static void overideBackup(String inputPath)
    {
        overideBackup = inputPath;
    }
    
    public static Connection getConnection()
    {
        return conn;
    }
    
    public static void shutdownConnection()
    {
        distributeLog("Shutting down connection...\n", 'B');
        
        try
        {
            if (conn != null)
            {
                DriverManager.getConnection(dbURL + ";shutdown=true");
                //This will always throw a SQL error. Derby is really weird this way, it is NORMAL behavior to throw a SQL error on close - it's by design.
            }
        }
        catch (SQLException sqlExcept)
        {
            try
            {
                //Close the connection
                conn.close();
                
                //Display connection is shut down
                distributeLog("Connection is shut down.\n", 'G');
                distributeLog(String.format("%85s", "").replace(' ', '~') + "\n", 'G');
            }
            catch (SQLException sqlExcept2)
            {
                distributeLog("Shut down FAILED!\n", 'R');
                distributeLog(String.format("%85s", "").replace(' ', '~') + "\n", 'R');
            }
        }
    }
}
