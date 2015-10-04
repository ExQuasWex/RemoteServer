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
    private Object lock4;
    private Object usernameLock;
    private Object lock5;


    protected ClientDB() throws RemoteException{

        lock1 = new Object();
        lock2 = new Object();
        lock3 = new Object();
        lock4 = new Object();
        lock5 = new Object();
        usernameLock = new Object();

    }

    // this is used directly by the client e.g checking database is up
    public  boolean checkConnectDB(){

        try {
            connection = DriverManager.getConnection(host,user,pass);

            return  true;
        } catch (SQLException e) {
            System.out.println("jdbc communication link failure");
            return false;
        }
    }

    // this is used for server
    public  boolean connect(){

        try {
            connection = DriverManager.getConnection(host,user,pass);
            return  true;
        } catch (SQLException e) {
            System.out.println("jdbc communication link failure");
            return false;
        }

    }

    // use checkConnectDB() here
    @Override
    public  boolean checkDatabase() throws RemoteException, SQLException {

        synchronized (lock4){
            return checkConnectDB();
        }
    }


    //METHODS THAT ARE NEED TO BE SYNCRONIZED asdasd

    @Override
    public boolean Login(String user, String pass) throws RemoteException {
    boolean isTrue = false;
        synchronized (lock3){
            connect();
            String loginSql = "SELECT User, password from account where User = ? and password = ?";
            String updateStatus = "UPDATE account SET status = ? WHERE User = ? and password = ?";

            try {
                PreparedStatement ps = connection.prepareStatement(loginSql);
                ps.setString(1,user);
                ps.setString(2, pass);

                PreparedStatement updatePS = connection.prepareStatement(updateStatus);
                updatePS.setString(1,"Online");
                updatePS.setString(2,user);
                updatePS.setString(3,pass);

                ResultSet rs = ps.executeQuery();
                int affectedRow = updatePS.executeUpdate();

                if (rs.next()){
                        String username = rs.getString("User");
                        String password = rs.getString("password");

                        if (username.equals(user) && password.equals(pass) && affectedRow == 1){
                            isTrue = true;
                        }else {
                            isTrue = false;
                        }

                }else {
                    isTrue = false;
                }

            } catch (SQLException e) {
                isTrue = false;
                e.printStackTrace();
            }

        }
    return isTrue;
    }



    @Override
    public boolean getAdminKeyCode(String keycode) throws RemoteException {

        boolean bool = false;

        synchronized (lock1){
            connect();
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
            connect();
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

    @Override
    public boolean getUsername(String username) throws RemoteException {
        boolean bool = false;

        synchronized (usernameLock){
            connect();
            String usernameSQL = "SELECT User FROM account WHERE User = ?";

            try {
                PreparedStatement ps = connection.prepareStatement(usernameSQL);
                ps.setString(1,username);

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
}

