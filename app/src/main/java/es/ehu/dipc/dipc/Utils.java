package es.ehu.dipc.dipc;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This class is used as a function library, to be able to call the same methods from different activities
 */
public class Utils {

    // The public static function which can be called from other classes
    public static void darkenStatusBar(Activity activity, int color) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            activity.getWindow().setStatusBarColor(
                    darkenColor(
                            ContextCompat.getColor(activity, color)));
        }

    }

    // Code to darken the color supplied (mostly color of toolbar)
    public static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    // Execute given command in the server and return output
    public static String execCommand(Session secondSession, String command) {
        Channel channel;
        StringBuilder outputBuffer = new StringBuilder();

        try {
            // Open exec channel and set command to the given one
            channel = secondSession.openChannel("exec");

            ((ChannelExec)channel).setCommand(command);

            channel.setInputStream(null);

            try {
                //System.out.println("0.1:" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) );
                InputStream commandOutput = channel.getInputStream();
                channel.connect();
                //System.out.println("0.2:" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) );
                    // A possible way of reading command output.
                    /*int readByte = commandOutput.read();

                    while(readByte != 0xffffffff)
                    {
                        outputBuffer.append((char)readByte);
                        readByte = commandOutput.read();
                    }*/
                // Improved way of reading command output
                try{
                    InputStreamReader inputReader = new InputStreamReader(commandOutput);
                    BufferedReader bufferedReader = new BufferedReader(inputReader);
                    String line;

                    while((line = bufferedReader.readLine()) != null){
                        outputBuffer.append(line);
                    }
                    bufferedReader.close();
                    inputReader.close();
                }catch(IOException ex){
                    ex.printStackTrace();
                }
                //System.out.println("0.3:" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) );
                channel.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
                channel.disconnect();
            }

            return outputBuffer.toString();

        } catch (JSchException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Execute 'showq' command in the server
    public static List<String> execQueueCommand(Session secondSession, String cluster) {
        Channel channel;
        List<String> output = new ArrayList<>();

        try {
            channel = secondSession.openChannel("exec");

            // Command isn't the same in all servers (Environment variables are not properly loaded using JSch connections)
            if (cluster.equals("ATLAS")){
                ((ChannelExec) channel).setCommand("export LD_LIBRARY_PATH=/usr/local/lib ; /usr/local/maui/bin/showq -u " + secondSession.getUserName());
            }else {
                ((ChannelExec) channel).setCommand("/software/maui/bin/showq -u " + secondSession.getUserName());
            }

            channel.setInputStream(null);

            try {
                //System.out.println("0.1:" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) );
                InputStream commandOutput = channel.getInputStream();
                channel.connect();
                //System.out.println("0.2:" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) );
                try{
                    InputStreamReader inputReader = new InputStreamReader(commandOutput);
                    try (BufferedReader bufferedReader = new BufferedReader(inputReader)) {
                        String line;

                        while ((line = bufferedReader.readLine()) != null) {
                            output.add(line);
                        }
                        bufferedReader.close();
                    }
                    inputReader.close();
                }catch(IOException ex){
                    ex.printStackTrace();
                }
                //System.out.println("0.3:" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) );
                channel.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
                channel.disconnect();
            }

            return output;

        } catch (JSchException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Get a list of active jobs in ATLAS
    public static String getJobIds(Session secondSession, String command) {
        Channel channel;
        StringBuilder outputBuffer = new StringBuilder();

        try {
            channel = secondSession.openChannel("exec");

            ((ChannelExec)channel).setCommand(command);

            channel.setInputStream(null);

            try {
                InputStream commandOutput = channel.getInputStream();
                channel.connect();
                try{
                    InputStreamReader inputReader = new InputStreamReader(commandOutput);
                    BufferedReader bufferedReader = new BufferedReader(inputReader);
                    String line;
                    while((line = bufferedReader.readLine()) != null){
                        outputBuffer.append(line);
                        outputBuffer.append(" ");
                    }
                    bufferedReader.close();
                    inputReader.close();
                }catch(IOException ex){
                    ex.printStackTrace();
                }
                channel.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
                channel.disconnect();
            }

            return outputBuffer.toString();

        } catch (JSchException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Get HTML DCRAB report of specified Job ID (Not properly tested due to time and resource limitations, should work)
    public static boolean getReport(Session secondSession, String jobID) {
        Channel channel;

        String path = execCommand(secondSession, "qstat -f " + jobID + " | grep Output_Path");
        path = path.substring(path.indexOf("/"), path.lastIndexOf("/")+1);
        try {
            channel = secondSession.openChannel("sftp");
            try {
                channel.connect();
                ChannelSftp sftp = (ChannelSftp) channel;
                try{
                    sftp.get(path + "dcrab_report_" + jobID + "/dcrab_report.html","/" + jobID + "_dcrab_report.html");
                }catch(SftpException ex){
                    ex.printStackTrace();
                    return false;
                }
                channel.disconnect();
            } catch (JSchException e) {
                e.printStackTrace();
                channel.disconnect();
                return false;
            }
        } catch (JSchException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Get Remora style DCRAB output (This functionality is not implemented in ATLAS yet and it's been tested with a handmade file)
    public static String[] getDcrabTextOutput(Session secondSession, String jobID) {
        String[] output;
        String filename = "dcrab_report.txt"; //Change to DCRAB report name

        String path = execCommand(secondSession, "qstat -f " + jobID + " | grep Output_Path");
        path = path.substring(path.indexOf("/"), path.lastIndexOf("/")+1);

        //Only use next line until Remora style DCRAB output is implemented or change it to the testing file path
        path = "/scratch/urkole/";

        String filecontents = execCommand(secondSession, "cat " + path + filename);

        output = filecontents.split(";");

        return output;
    }

}