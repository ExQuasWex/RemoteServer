package MainApp.Preferences;

import sun.security.krb5.internal.PAData;
import utility.Utility;

import java.util.prefs.Preferences;

/**
 * Created by reiner on 3/5/2016.
 */
public class Preference {

    private static String KEYPATH = "DBPATH";
    private static String dbPath = "";

    static  Preferences preferences = Utility.createPreference();

    public static String getDBpath(){
        return preferences.get(KEYPATH, dbPath);
    }

    public static void  setDBpath(String path){
        preferences.put(KEYPATH, path );
    }
}
