package MainApp.DataBase;

import org.h2.jdbcx.JdbcConnectionPool;

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

}
