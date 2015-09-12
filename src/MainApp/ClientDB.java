package MainApp;

import RMI.RemoteMethods;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Didoy on 8/24/2015.
 */
public class ClientDB extends UnicastRemoteObject implements RemoteMethods {

    private Connection connection;
    private final String host = "jdbc:mysql://localhost/pdss";
    private final String user = "client";
    private final String pass = "client";


    protected ClientDB() throws RemoteException {

    }


    public  boolean checkConnectDB(){

        try {
            connection = DriverManager.getConnection(host,user,pass);
            return  true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }


    @Override
    public boolean Login(String user, String pass) throws RemoteException {

                if (user.equals("wew") && pass.equals("wew")){
                    return true;
                }else {
                    return false;
                }
    }

    @Override
    public boolean checkDatabase() throws RemoteException {
        return checkConnectDB();
    }
}

