package MainApp.DataBase;

import MainApp.Preferences.Preference;
import ToolKit.MessageBox;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.io.File;
import java.io.IOException;

/**
 * Created by Didoy on 2/25/2016.
 */
public class Database {

    private static final String host = "jdbc:h2:file:~/UrbanPoor/pdsss/database/pdss;Mode=MySQL;MVCC=TRUE";
    private static final String user = "admin";
    private static final String pass = "admin";

    private static JdbcConnectionPool connectionPool  = JdbcConnectionPool.create(host, user, pass);

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

        String path = Preference.getDBpath();
            if (path.equals("")){
                path = MessageBox.showImportDatabaseDialog();
                Preference.setDBpath(path);
            }
            else {
                File source = new File(path);
                        if (!source.exists()){
                            Utility.showMessageBox("Database is no longer existing in the file path", Alert.AlertType.ERROR);
                            Preference.setDBpath("");
                            SynchDB();
                        }else {
                            File dstination = new File(path + "copy");
                            CreateCopyOfDB(source, dstination);
                        }
            }

        return  isSave;
    }
    private static  void CreateCopyOfDB(File source, File dstination){

        try {
            FileUtils.copyDirectoryToDirectory(source, dstination);
        } catch (IOException e) {
            Utility.showConfirmationMessage("Unable to synch Database contact your database administrator", Alert.AlertType.INFORMATION);
            e.printStackTrace();
        }
    }

}
