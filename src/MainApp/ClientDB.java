package MainApp;

import RMI.RemoteMethods;
import clientModel.StaffRegister;

import javax.naming.ldap.PagedResultsControl;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.Objects;

/**
 * Created by Didoy on 8/24/2015.
 */
public class ClientDB extends UnicastRemoteObject implements RemoteMethods  {

    private boolean isRegistered;

    private Connection connection;
    private final String host = "jdbc:mysql://localhost/pdss";
    private final String user = "admin";
    private final String pass = "admin";

    private Object lock1;
    private Object lock2;
    private Object lock3;


    protected ClientDB() throws RemoteException{

        lock1 = new Object();
        lock2 = new Object();

    }


    public  boolean checkConnectDB(){

        try {
            connection = DriverManager.getConnection(host,user,pass);
            return  true;
        } catch (SQLException e) {
            System.out.println("jdbc communication link failure");
            return false;
        }

    }


    //METHODS THAT ARE NEED TO BE SYNCRONIZED

    @Override
    public boolean Login(String user, String pass) throws RemoteException {

                if (user.equals("wew") && pass.equals("wew")){
                    System.out.println("weeew");
                    return true;
                }else {
                    return false;
                }
    }

    @Override
    public synchronized boolean checkDatabase() throws RemoteException, SQLException {
        return checkConnectDB();
    }

    @Override
    public boolean getAdminKeyCode(String keycode) throws RemoteException {
        checkConnectDB();
        boolean bool = false;

        synchronized (lock1){
            String Sql = "Select KeyCode from keycode where keycode = ?";
            try {
                PreparedStatement ps = connection.prepareStatement(Sql);
                ps.setString(1, keycode);

                ResultSet rs = ps.executeQuery();

                if (rs.next()){
                    bool = true;
                }else {
                    bool = false;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return bool;
    }

    @Override
    public boolean register(StaffRegister staffRegister) throws RemoteException {

        synchronized (lock2){
            checkConnectDB();
            boolean isRegistered = false;
            int accountID = 0;
            int secretInfoID = 0;

            /// insert account of the encode here

            String insertAccount = "INSERT INTO account (User,password,Status) VALUES (?,?,?)";
            try {
                PreparedStatement ps = connection.prepareStatement(insertAccount, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1,staffRegister.getUsername());
                ps.setString(2,staffRegister.getPassword());
                ps.setString(3,"offline");

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();

                if (rs != null && rs.next()){
                    accountID = rs.getInt(1);
                }

            } catch (SQLException e) {
                isRegistered = false;
                e.printStackTrace();
            }

            // insert the encoders security question

            String insertSecurity = "INSERT INTO secretinfo (SecretQuestionID, SecretAnswer) VALUES (?,?)";

            try {
                PreparedStatement ps = connection.prepareStatement(insertSecurity, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1,staffRegister.getSecretID());
                ps.setString(2,staffRegister.getSecretAnswer());

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()){
                    secretInfoID = rs.getInt(1);
                }

            } catch (SQLException e) {
                isRegistered = false;
                e.printStackTrace();
            }


            // insert encoder personal information here

            String infoSQL = "INSERT INTO client (Name, Address, ContactNo, Gender, AccountID, SecretInfoID) VALUES (?,?,?,?,?,?)";

            try {
                PreparedStatement ps = connection.prepareStatement(infoSQL);
                ps.setString(1,staffRegister.getName());
                ps.setString(2,staffRegister.getAddress());
                ps.setString(3,staffRegister.getContact());
                ps.setString(4,staffRegister.getGender());
                ps.setInt(5,accountID);
                ps.setInt(6,secretInfoID);

                ps.executeUpdate();

                isRegistered = true;
            } catch (SQLException e) {
                isRegistered = false;
                e.printStackTrace();
            }

        }

        return  isRegistered;
    }
}

