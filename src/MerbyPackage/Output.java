package MerbyPackage;

import static MerbyPackage.ClassManager.getTodaysDate;
import static MerbyPackage.GUI_Window.output;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class Output 
{    
    protected enum EnumErrors {WRITE_ACCESS, SERVICE_STARTED, SERVICE_NOT_INSTALLED, TABLE_EXPORT_FAILED, LOGFILE_FAILURE, TABLE_CONTAINS_DATA;}
    private static PrintWriter logging;
    private static final List<EnumErrors> ERRORS = new ArrayList<>();
    
    public static void startLog()
    {        
        try
        {
            logging = new PrintWriter(new FileOutputStream(new File("Merby.log"),true));
            logging.print(getTodaysDate(2) + " " + "-----New Session-----" + "\r\n");
            logging.flush();
        }
        catch (IOException IOExcept)
        {
            logging = null;
        }
    }
    
    public static void outputToLogFile(String logMessage)
    {
        if(logging != null)
        {
            logging.print(getTodaysDate(2) + " " + logMessage.replace("\n", "\r\n"));
            logging.flush();
        }
    }
    
    public static void distributeLog(String logMessage, char charColor)
    {
        outputToLogFile(logMessage);
        
        int lineSize = 100;
        
        if (charColor != 'C')
        {
            output(getTodaysDate(1) + "\t", 'C');
        }
        else
        {
            output("\t" + getTodaysDate(2) + " ", 'C');
        }
        
        if (logMessage.length() > lineSize)
        {
            String outputString = logMessage.replace("\n", "");
            int lineTotal = outputString.length() / lineSize;
            int remainder = outputString.length() % lineSize;
            int startPos = 0;
            int endPos = 0;

            for(int i=1; i<=lineTotal; i++)
            {
                startPos = (i-1) * lineSize;
                endPos = i * lineSize;
                
                if (i == 1)
                {
                    output(outputString.substring(startPos,endPos), charColor);
                    output("\n", charColor);
                }
                else if (i > 1)
                {
                    output("\t" + outputString.substring(startPos,endPos), charColor);
                    output("\n", charColor);
                }
            }

            if (remainder > 0)
            {
                output("\t" + outputString.substring(endPos, endPos + remainder), charColor);
                output("\n", charColor);
            }
        }
        else
        {
            output(logMessage, charColor);
        }
    }
    
    public static void addErrors(EnumErrors error)
    {
        ERRORS.add(error);
    }
    
    public static void displayErrors(Component centerScreen)
    {            
        String output;
        
        for(EnumErrors error : ERRORS)
        {
            switch (error)
            {
                case WRITE_ACCESS:
                {
                    output = "Merby does NOT have access to Mirth Directory. Please run as Administrator!!!\n" +
                "        (To do this, run Batch file 'StartMerbyAsAdmin.bat' as an Admin)";
                    break;
                }
                case SERVICE_STARTED:
                {
                    output = "Mirth service is still running - Merby may not be able to access Derby database!";
                    break;
                }
                case SERVICE_NOT_INSTALLED:
                {
                    output = "Mirth service is not installed! There may not be a Database to connect to!";
                    break;
                }
                case TABLE_EXPORT_FAILED:
                {
                    output = "One or more table exorts failed! Check log for details.";
                    break;
                }
                case LOGFILE_FAILURE:
                {
                    output = "Unable to create log file! Ensure you unzipped Merby!";
                    break;
                }
                case TABLE_CONTAINS_DATA:
                {
                    output = "Import may have failed due to tables already containing data!";
                    break;
                }
                default:
                {
                    output = "UNKNOWN ERROR";
                }
            }
            JOptionPane.showMessageDialog(centerScreen, output, "Merb says...", JOptionPane.ERROR_MESSAGE);
        }

        ERRORS.clear();
    }
    
    public static void outputProgramStatus()
    {
        if (logging == null)
        {
            distributeLog("Unable to create/write Merby.log! Merby may not be running with write permissions!\n", 'R');
            addErrors(Output.EnumErrors.LOGFILE_FAILURE);
        }
        
        distributeLog("Welcome to Merby! Please enter a password.\n", 'B');
    }
    
    public static boolean errorsArePresent()
    {
        return !ERRORS.isEmpty();
    }
}
