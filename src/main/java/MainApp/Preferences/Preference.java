package MainApp.Preferences;

import sun.security.krb5.internal.PAData;
import utility.Utility;

import java.util.prefs.Preferences;

/**
 * Created by reiner on 3/5/2016.
 */
public class Preference {

    private static String DrctoryKeyPath = "directoryDBPath";
    private static String DrctoryPath = "";



    private static String DBPath = "DBPATH";
    private static String dbPath = "";

    static  Preferences preferences = Utility.createPreference();

    public static String getDirectoryDBpath(){
        return preferences.get(DrctoryKeyPath, DrctoryPath);
    }

    public static void  setDirectoryDBpath(String path){
        preferences.put(DrctoryKeyPath, path );
    }

    public static void  setDbPath(String path){
        preferences.put(DBPath, path );
    }
    public static String  getDBPath(){
      return   preferences.get(DBPath, dbPath );
    }


}
