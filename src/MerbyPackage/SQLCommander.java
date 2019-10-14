package MerbyPackage;

import static MerbyPackage.Output.distributeLog;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SQLCommander 
{
    private int XmlColumn;
    private String chanPath;
    private String minRow;
    private String overideDir = "C:\\MerbyBackups\\MirthConfigurations\\";
    private String tableName;
    private String version;
    private final String [] credentials = new String[2];
    private final Connection conn;
    private Statement stmt;
    private Statement verStmt;
    private Statement compressStmt;
    private Statement CredentialsStmt;
    private Statement queryTableStmt;
    private ResultSet results;
    private ResultSet versionResults;
    private ResultSet compressResults;
    private ResultSet CredentialsResults;
    private ResultSet queryTableResults;
    private ResultSetMetaData rsmd;
    private List<String> uncompressedTables;

    public SQLCommander(String subFolder, String Date, String Dir)
    {
        conn = ClassManager.getConnection();
        
        if(Dir.length() > 0)
        {
            overideDir = Dir;
        }
        chanPath = overideDir + Date + "\\" + subFolder;
    }
    
    public SQLCommander()
    {
        conn = ClassManager.getConnection();
    }
    
    public void setData(String table)
    {
        tableName = table;
        
        try
        {
            stmt = conn.createStatement();
            results = stmt.executeQuery("select * from " + tableName);
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
    }
    
    public boolean exportTable(String tableName)
    {
        String exportCommand;
        
        if(tableName.equals("CONFIGURATION"))
        {
            exportCommand = "CALL SYSCS_UTIL.SYSCS_EXPORT_QUERY ('SELECT * FROM CONFIGURATION WHERE NAME = ''channelMetadata''', '" + chanPath + "\\" + tableName + "_MirVer_" + version + ".merby', null, null, null)";
        }
        else
        {
            exportCommand = "CALL SYSCS_UTIL.SYSCS_EXPORT_TABLE ('APP','" + tableName.toUpperCase() + "','" + chanPath + "\\" + tableName + "_MirVer_" + version + ".merby',null,null,null)";
        }
        
        try
        {
            stmt = conn.createStatement();
            stmt.execute(exportCommand);
            distributeLog("Backed up File: " + chanPath + "\\" + tableName + "_MirVer_" + version + ".merby\n", 'G');
            return true;
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(tableName.toUpperCase() + " table export wasn't performed! See reason below!\n", 'R');
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'B');
            return false;
        }
    }
    
    public void importTable(String tableName, String importPath)
    {
        try
        {
            stmt = conn.createStatement();
            stmt.execute("CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE ('APP','" + tableName.toUpperCase() + "','" + importPath + "',null,null,null,0)");
            distributeLog("Imported file: '" + importPath + "', into table: " + tableName.toUpperCase() + "\n", 'G');
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(tableName.toUpperCase() + " table import wasn't performed! See reason below!\n", 'R');
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
    }
    
    public String [] getCredentials()
    {
        try
        {
            CredentialsStmt = conn.createStatement();
            CredentialsResults = CredentialsStmt.executeQuery(""
                    + "SELECT min("
                    + " CASE "
                    + "  WHEN (SELECT 1 FROM PERSON WHERE USERNAME = 'admin') = 1 THEN (SELECT ID FROM PERSON WHERE USERNAME = 'admin') "
                    + "  ELSE ID "
                    + " END) "
                    + "FROM person");
            CredentialsResults.next();
            minRow = CredentialsResults.getString(1);            
            
            CredentialsResults = CredentialsStmt.executeQuery("select username from person where ID = " + minRow);
            CredentialsResults.next();
            credentials[0] = CredentialsResults.getString(1);

            if(getTableStatus("person_password"))
            {
                CredentialsResults = CredentialsStmt.executeQuery("select password from person_password where person_id = " + minRow);
                CredentialsResults.next();
                credentials[1] = CredentialsResults.getString(1);
            }
            else
            {
                CredentialsResults = CredentialsStmt.executeQuery("select password, salt from person where id = " + minRow);
                CredentialsResults.next();
                credentials[1] = CredentialsResults.getString(1) + "[MD5_Salt:]" + CredentialsResults.getString(2);
            }
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }

        if (credentials[0] == null)
        {
            credentials[0] = "NULL USERNAME";
        }

        if (credentials[1] == null)
        {
            credentials[1] = "NULL PASSWORD";
        }
        
        return credentials;
    }
    
    public void resetCredentials()
    {
        try
        {
            int rowsModifiedUser = 0, rowsModifiedPW = 0;
            
            distributeLog("Old Credentials: " + Arrays.toString(getCredentials()) + "\n", 'B');
            stmt = conn.createStatement();
            rowsModifiedUser = stmt.executeUpdate("UPDATE PERSON SET USERNAME = 'admin' WHERE ID = " + minRow);
            
            if (rowsModifiedUser > 0)
            {
                distributeLog("Successfully reset Username! Primary Username is now 'admin'.\n", 'G');
            }
            else
            {
                distributeLog("Username reset failed! Zero rows modified!\n", 'R');
            }
            
            if(rowsModifiedUser > 0 && getTableStatus("person_password"))
            {
                rowsModifiedPW = stmt.executeUpdate("UPDATE PERSON_PASSWORD SET PASSWORD = 'YzKZIAnbQ5m+3llggrZvNtf5fg69yX7pAplfYg0Dngn/fESH93OktQ==' WHERE PERSON_ID = " + minRow);
            }
            else if (rowsModifiedUser > 0)
            {
                rowsModifiedPW = stmt.executeUpdate("UPDATE PERSON SET PASSWORD = 'T3D14obEzfut9AEC8jagxJO6Wjw=' WHERE ID = " + minRow);
                rowsModifiedPW = rowsModifiedPW + stmt.executeUpdate("UPDATE PERSON SET SALT = 'U+nfwsn9e3w=' WHERE ID = " + minRow);
            }
            else
            {
                distributeLog("Password not reset due to Username not being reset.\n", 'B');
            }
            
            if (rowsModifiedPW > 0)
            {
                distributeLog("Successfully reset Password! Primary Password is now 'admin'.\n", 'G');
                distributeLog("New Credentials: " + Arrays.toString(getCredentials()) + "\n", 'B');
            }
            else
            {
                distributeLog("Username reset failed! Zero rows modified!\n", 'R');
            }
        }
        catch (SQLException sqlExcept)
        {
            distributeLog("Username and/or Password was not reset!!!\n", 'R');
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');

        }
    }
    
    public List<String> getUncompressedTables()
    {
        uncompressedTables = new ArrayList<>();
        
        if(getTableStatus("message"))
        {
            uncompressedTables.add("message");
        }
        else
        {
            try
            {
                compressStmt = conn.createStatement();
                compressResults = compressStmt.executeQuery("select tablename " +
                        "from sys.systables " +
                        "where tabletype = 'T' " +
                        "and tablename like 'D_MC%' " +
                        "and tablename not like 'D_MCM%' " +
                        "order by tablename");

                while (compressResults.next())
                {
                    uncompressedTables.add(compressResults.getString(1));
                }
            }
            catch (SQLException sqlExcept)
            {
                distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
            }
        }
        
        return uncompressedTables;
    }
    
    public void compressDB(String tableToCompress, int tableNum)
    {
        try
        {
            compressStmt = conn.createStatement();
            compressStmt.execute("call SYSCS_UTIL.SYSCS_COMPRESS_TABLE('APP','" + tableToCompress.toUpperCase() + "', 1)");
            distributeLog("Compressed Table" + tableNum + ": '" + tableToCompress + "'\n", 'G');
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
        
    }
    
    public void setVersion()
    {
        try
        {
            verStmt = conn.createStatement();
            versionResults = verStmt.executeQuery("select version from schema_info");
            versionResults.next();
            version = versionResults.getString(1);
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
        
    }
    
    public void setXmlColumn()
    {
        try
        {
            XmlColumn = 0;
            rsmd = results.getMetaData();
            int numberCols = rsmd.getColumnCount();
            String columnName;
            
            for (int i=1; i<=numberCols; i++)
            {
                columnName = rsmd.getColumnLabel(i);
                if (tableName.equalsIgnoreCase("channel"))
                {
                    if (columnName.equalsIgnoreCase("channel"))
                    {
                        XmlColumn = i;
                    }
                }
                else if(tableName.equalsIgnoreCase("code_template"))
                {
                    if (columnName.equalsIgnoreCase("code") || columnName.equalsIgnoreCase("code_template"))
                    {
                        XmlColumn = i;
                    }
                }
            }
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
    }
    
    public String getVersion()
    {
        return version;
    }
    
    public String getPath()
    {
        return chanPath;
    }
    
    public ResultSet getResults()
    {
        return results;
    }
    
    public int getXmlColumn()
    {
        return XmlColumn;
    }

    public boolean getTableStatus(String tableName)
    {
        boolean tExists = false;

        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName.toUpperCase(), null))
        {
            while (rs.next())
            { 
                String tName = rs.getString("TABLE_NAME");
                if (tName != null && tName.equals(tableName.toUpperCase()))
                {
                    tExists = true;
                    break;
                }
            }
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
        
        return tExists;
    }
    
    public boolean getTableContainsData(String tableName)
    {        
        Boolean tableContainsData;
        
        try
        {
            queryTableStmt = conn.createStatement();
            
            if(tableName.equals("CONFIGURATION"))
            {
                queryTableResults = queryTableStmt.executeQuery("SELECT EXISTS(SELECT * FROM CONFIGURATION WHERE NAME = 'channelMetadata') FROM SYSIBM.SYSDUMMY1");
            }
            else
            {
                queryTableResults = queryTableStmt.executeQuery("SELECT EXISTS(SELECT * FROM " + tableName + ") from SYSIBM.SYSDUMMY1");
            }
            
            queryTableResults.next();
            tableContainsData = Boolean.parseBoolean(queryTableResults.getString(1));
        }
        catch (SQLException sqlExcept)
        {
            tableContainsData = true; //In case of SQL Exception, just return true, so there's no risk of not being able to backup data.
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
        
        return tableContainsData;
    }
    
    public void closeConnections()
    {
        try
        {
            if (stmt != null)
            {
                stmt.close();
            }
            if (verStmt != null)
            {
                verStmt.close();
            }
            if (CredentialsStmt != null)
            {
                CredentialsStmt.close();
            }
            if (compressStmt != null)
            {
                compressStmt.close();
            }
            if (queryTableStmt != null)
            {
                queryTableStmt.close();
            }
        }
        catch (SQLException sqlExcept)
        {
            distributeLog(String.format("%1.298s", sqlExcept.toString()) + "\n", 'R');
        }
    }
}
