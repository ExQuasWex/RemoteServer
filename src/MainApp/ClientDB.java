package MainApp;

import RMI.RemoteMethods;
import clientModel.Family;
import clientModel.FamilyPoverty;
import clientModel.StaffInfo;
import clientModel.StaffRegister;
import org.h2.jdbcx.JdbcConnectionPool;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;

/**
 * Created by Didoy on 8/24/2015.
 */
public class ClientDB extends UnicastRemoteObject implements RemoteMethods  {

    private boolean isRegistered;

    private Connection connection;
    private final String host = "jdbc:h2:file:c:/pdsss/database/pdss;Mode=MySQL;LOCK_MODE=1";
    private final String user = "admin";
    private final String pass = "admin";

    private Object lock1;
    private Object lock2;
    private Object lock3;
    private Object lock4;
    private Object usernameLock;
    private Object logoutLock;
    private Object FamilyLock;


    private static JdbcConnectionPool cp;

    protected ClientDB() throws RemoteException{

        lock1 = new Object();
        lock2 = new Object();
        lock3 = new Object();
        lock4 = new Object();
        logoutLock = new Object();
        usernameLock = new Object();
        FamilyLock = new Object();
        cp = JdbcConnectionPool.create(host, user, pass);

    }

    // this is used directly by the client e.g checking database is up asd
    public  boolean checkConnectDB(){
        boolean isconnected = false;

        try {
            Connection connection = cp.getConnection();

            if (connection.isValid(5000)){
                isconnected = true;
            }else{
                isconnected = false;
            }

            connection.close();
        } catch (SQLException e) {
            isconnected = false;
            e.printStackTrace();
        }
        return isconnected;
    }

    // this is used for server


    // use checkConnectDB() here
    @Override
    public  boolean checkDatabase() throws RemoteException, SQLException {

        synchronized (lock4){
            return checkConnectDB();
        }
    }


    //METHODS THAT ARE NEED TO BE SYNCRONIZED

    @Override
    public StaffInfo Login(String user, String pass) throws RemoteException {

    boolean isTrue = false;
    StaffInfo staffInfo = null;

        synchronized (lock3){
            int accountID = 0;
            String status = null;
            String loginSql = "SELECT id, User, password, status from account where User = ? and password = ?";
            String updateStatus = "UPDATE account SET status = ? WHERE User = ? and password = ?";

            try {
                connection = cp.getConnection();
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
                    accountID = rs.getInt("id");
                    status = rs.getString("status");

                    if (username.equals(user) && password.equals(pass) && affectedRow == 1){
                        isTrue = true;
                    }else{
                        isTrue = false;
                    }

                }else {
                    isTrue = false;
                }

                if (isTrue){

                    String getInfoSql = "SELECT name, address, contactno, totalentries from client WHERE accountid = ?";

                    PreparedStatement  getPS =  connection.prepareStatement(getInfoSql);
                    getPS.setInt(1,accountID);

                    ResultSet getRS = getPS.executeQuery();

                    if (getRS.next()){
                        String name = getRS.getString("name");
                        String address = getRS.getString("address");
                        String contacno = getRS.getString("contactno");
                        int totalentries = getRS.getInt("totalentries");
                        staffInfo = new StaffInfo(true,accountID,status,name,user,pass,address,contacno,totalentries);
                    }

                }else {
                    isTrue = false;
                    staffInfo = new StaffInfo(false,0,null,null,null,null,null,null,0);
                }


                ps.close();
                connection.close();


            } catch (SQLException e) {
                isTrue = false;
                staffInfo = new StaffInfo(false,0,null,null,null,null,null,null,0);
                e.printStackTrace();
            }

        }

    return staffInfo;
    }



    @Override
    public boolean getAdminKeyCode(String keycode) throws RemoteException {

        boolean bool = false;

        synchronized (lock1){

            String Sql = "Select KeyCode from keycode where keycode = ?";
            try {
                connection = cp.getConnection();
                PreparedStatement ps = connection.prepareStatement(Sql);
                ps.setString(1, keycode);

                ResultSet rs = ps.executeQuery();

                if (rs.next()){
                    bool = true;
                }else {
                    bool = false;
                }
                ps.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return bool;
    }

    @Override
    public boolean register(StaffRegister staffRegister) throws RemoteException {

        synchronized (lock2){

            int accountID = 0;
            int secretInfoID = 0;

            /// insert account of the encode here

            String insertAccount = "INSERT INTO account (User,password,Status) VALUES (?,?,?)";
            try {
                connection = cp.getConnection();
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

                ps.close();
                connection.close();
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

            String usernameSQL = "SELECT User FROM account WHERE User = ?";

            try {
                connection = cp.getConnection();
                PreparedStatement ps = connection.prepareStatement(usernameSQL);
                ps.setString(1,username);

                ResultSet rs = ps.executeQuery();

                if (rs.next()){
                    bool = true;
                }else {
                    bool = false;
                }
                ps.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        return bool;
    }

    @Override
    public void Logout(int accountID) throws RemoteException {

        String logout = "Update account set status = ? where id = ?";

        synchronized (logoutLock){
            try {
                connection = cp.getConnection();

                PreparedStatement ps = connection.prepareStatement(logout);
                ps.setString(1,"offline");
                ps.setInt(2,accountID);

                ps.executeUpdate();

                ps.close();
                connection.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean addFamilyInfo(Family family) throws RemoteException {
        boolean bool= false;
        String checkRecord = "Select name from family where name = ?";

        synchronized (FamilyLock) {
            try {
                connection = cp.getConnection();

                PreparedStatement ps = connection.prepareStatement(checkRecord);
                ps.setString(1,family.getFamilyinfo().getName());

                ResultSet rs = ps.executeQuery();

                if (rs.next()){
                    //Update
                }else{
                 bool =  addToBarangay(family);
                }

            } catch (SQLException e) {
                bool = false;
                e.printStackTrace();
            }
        }

        return bool;
    }


    private boolean addToBarangay(Family family){
    boolean isValid = false;

        String insertbarangay = "Insert INTO barangay (name,date,unresolvepopulation,resolvepopulation) VALUES " +
                "(?,?,?,?)";
        String chckBarangay = "Select id from barangay where name = ? and date = ?";
        String updateBarangay = "Update barangay  SET Unresolvepopulation = Unresolvepopulation + 1 where id = ?";

        int barangayID = 0;

            try {
                connection = cp.getConnection();

                // check if barangay is already existing
                PreparedStatement chckPs = connection.prepareStatement(chckBarangay,Statement.RETURN_GENERATED_KEYS);
                chckPs.setString(1,family.getFamilyinfo().getBarangay());
                chckPs.setInt(2,family.getFamilyinfo().getSurveyedYr());

                ResultSet chckRs = chckPs.executeQuery();


                if (chckRs.next()){
                    barangayID = chckRs.getInt("id");

                    PreparedStatement updatePs = connection.prepareStatement(updateBarangay,Statement.RETURN_GENERATED_KEYS);
                    updatePs.setInt(1,barangayID);
                    int row = updatePs.executeUpdate();
                    //update

                }else {
                    // insert to barangay
                    PreparedStatement barangayPS = connection.prepareStatement(insertbarangay, Statement.RETURN_GENERATED_KEYS);
                    barangayPS.setString(1,family.getFamilyinfo().getBarangay());
                    barangayPS.setInt(2, family.getFamilyinfo().getSurveyedYr());
                    barangayPS.setInt(3,1);
                    barangayPS.setInt(4,0);

                    int row = barangayPS.executeUpdate();
                    ResultSet barangayRs = barangayPS.getGeneratedKeys();

                    if (row == 1&& barangayRs.next()) {
                        barangayID = barangayRs.getInt(1);
                    }
                }

                isValid = addFamily(family,barangayID,connection);


            } catch (SQLException e) {
                isValid = false;
                e.printStackTrace();
            }
    return isValid;
    }

    private boolean addFamily(Family family, int barangayID, Connection connection){
        boolean isSucess = false;
        int familyID = 0;

        String addFamilySql = "Insert INTO family (date,name,maritalstatus,age,spouse,address,childrenno,gender,yrresidency,yrissued,clientid)" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try {

            int numofChildren = family.getFamilyinfo().getNumofChildren();
            PreparedStatement ps = connection.prepareStatement(addFamilySql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,family.getFamilyinfo().getInputDate());
            ps.setString(2,family.getFamilyinfo().getName());
            ps.setString(3,family.getFamilyinfo().getMaritalStatus());
            ps.setString(4,family.getFamilyinfo().getAge());
            ps.setString(5,family.getFamilyinfo().getSpouseName());
            ps.setString(6,family.getFamilyinfo().getAddress());
            ps.setInt(7,numofChildren);
            ps.setString(8,family.getFamilyinfo().getGender());
            ps.setInt(9, family.getFamilyinfo().getResidencyYr());
            ps.setInt(10, family.getFamilyinfo().getSurveyedYr());
            ps.setInt(11,family.getFamilyinfo().getClientID());

            int row = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (row == 1 && rs.next()){
                familyID = rs.getInt(1);
               isSucess = addPovertyFactors(family.getFamilypoverty(),familyID,connection);

            }else{
                isSucess = false;
            }

        } catch (SQLException e) {
            isSucess = false;
            e.printStackTrace();
        }


        return isSucess;
    }

    public boolean addPovertyFactors(FamilyPoverty familyPoverty, int familyId, Connection connection){
        boolean isAdded = false;

        String addPovertySql = "Insert Into povertyfactors (occupancy,schoolchildren,underemployed,otherincome,threshold,ownership) " +
                "Values (?,?,?,?,?,?)";

        //
        try {
            PreparedStatement ps = connection.prepareStatement(addPovertySql);
            ps.setString(1,familyPoverty.getOccupancy());
            ps.setString(2,familyPoverty.getChildreninSchool());
            ps.setString(3,familyPoverty.getIsunderEmployed());
            ps.setString(4,familyPoverty.getHasotherIncome());
            ps.setString(5,familyPoverty.getIsbelow8k());
            ps.setString(6,familyPoverty.getOwnership());

            int row = ps.executeUpdate();
                if (row == 1) {
                    isAdded =  true;
                }

        } catch (SQLException e) {
            isAdded = false;
            e.printStackTrace();
        }
        System.out.println("sucessfully added family");
        return isAdded;
    }

}

