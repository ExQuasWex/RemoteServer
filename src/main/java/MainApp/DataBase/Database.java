package MainApp.DataBase;

import MainApp.Preferences.Preference;
import ToolKit.MessageBox;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import org.zeroturnaround.zip.ZipUtil;
import utility.Utility;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Didoy on 2/25/2016.
 */
public class Database {
    private static String DBprefix = "jdbc:h2:file:";
    private static String DBsuffix = "/pdsss/database/pdss;Mode=MySQL;MVCC=TRUE";

    private static  String host = "";
    private static final String user = "admin";
    private static final String pass = "admin";

    private static JdbcConnectionPool connectionPool;

    public static JdbcConnectionPool getConnectionPool (){
        return connectionPool;
    }

    public static void  setMaxConnection(int MaxConnection){
        connectionPool.setMaxConnections(MaxConnection);
    }

    //  method is just used for development purpose

    public static void monitorActiveConnection(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(5002);
                        System.out.println("Active connectionss : "+connectionPool.getActiveConnections());

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();

    }

    public static boolean SynchDB(){
        boolean isSave = false;

        String path = Preference.getDirectoryDBpath();

            if (path.equals("")){
                path = MessageBox.showImportDatabaseDialog();
                Preference.setDirectoryDBpath(path);

                final String  AbsoluteDBPath = DBprefix + path + DBsuffix;
                System.out.println(AbsoluteDBPath);
                Preference.setDbPath(AbsoluteDBPath);
                host = AbsoluteDBPath;
                connectionPool  = JdbcConnectionPool.create(host, user, pass);
                isSave =true;
            }
            else {
                File source = new File(path);
                        if (!source.exists()){
                            Utility.showMessageBox("Database is no longer existing in the file path", Alert.AlertType.ERROR);
                            Preference.setDirectoryDBpath("");
                            Preference.setDbPath("");
                            SynchDB();
                        }else {

                            File dstination = new File(path + "copy");
                            boolean isCopied = CreateCopyOfDB(source, dstination);
                            if (isCopied){
                                dstination = new File(path + "copy.zip");
                                ZipUtil.pack(source, dstination);

                            }

                            // ZipUtil.unpack(source , new File("D:\\"));
                            // ZipUtil.unexplode(dstination);

                            path = Preference.getDBPath();
                            System.out.println(path);
                            host = path;
                            connectionPool  = JdbcConnectionPool.create(host, user, pass);
                            isSave = true;
                        }
            }

        return  isSave;
    }
    private static  boolean CreateCopyOfDB(File source, File dstination){
        boolean isCopied = false;
        try {
            FileUtils.copyDirectoryToDirectory(source, dstination);
            isCopied = true;
        } catch (IOException e) {
            Utility.showConfirmationMessage("Unable to synch Database contact your database administrator", Alert.AlertType.INFORMATION);
            e.printStackTrace();
        }
        return isCopied;
    }


}
