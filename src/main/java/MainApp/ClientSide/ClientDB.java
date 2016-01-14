package MainApp.ClientSide;

import AdminModel.RequestAccounts;
import RMI.ClientInterface;
import RMI.Constant;
import RMI.RemoteMethods;
import  Remote.Method.FamilyModel.Family;
import Remote.Method.FamilyModel.FamilyPoverty;
import Remote.Method.FamilyModel.FamilyInfo;

import clientModel.StaffInfo;
import clientModel.StaffRegister;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import global.OnlineClient;
import org.h2.jdbcx.JdbcConnectionPool;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * Created by Didoy on 8/24/2015.
 * slack test
 */


public class ClientDB extends UnicastRemoteObject implements RemoteMethods  {

    private boolean isRegistered;

    private Connection connection;
    private final String host = "jdbc:h2:file:c:/pdsss/database/pdss;Mode=MySQL;LOCK_MODE=1";
    private final String user = "admin";
    private final String pass = "admin";

    private final Object lock1;
    private final Object lock2;
    private final Object lock3;
    private final Object lock4;
    private final Object pendingAccountLock;
    private final Object updateStaffLock;
    private final Object usernameLock;
    private final Object logoutLock;
    private final Object FamilyLock;
    private final Object searchLock;
    private final Object connectionLock;
    private final Object requestAccountLock;
    private final Object ApprovedLock;
    private final Object ApprovedAdminLock;
    private final Object RejectLock;


    private String methodIdentifier;

    // CLient Credential
    private int PORT = 0;
    private  String REMOTE_ID;


    // this list holds all the usernames who are online
    private OnlineClientArrayList onlineClientArrayList;

    private static JdbcConnectionPool connectionPool;

    private String ipAddress;

    private int AccountID = 0;
    private String status;
    private String username;
    private String role;
    private String globalPassword;
    private static ArrayList searchList;


    public ClientDB() throws RemoteException{

        lock1 = new Object();
        lock2 = new Object();
        lock3 = new Object();
        lock4 = new Object();
        logoutLock = new Object();
        usernameLock = new Object();
        FamilyLock = new Object();
        updateStaffLock = new Object();
        searchLock = new Object();
        connectionLock = new Object();
        pendingAccountLock = new Object();
        requestAccountLock = new Object();

        ApprovedLock = new Object();
        ApprovedAdminLock = new Object();
        RejectLock = new Object();


        connectionPool = JdbcConnectionPool.create(host, user, pass);
        connectionPool.setMaxConnections(40);
        onlineClientArrayList = new OnlineClientArrayList();

    }
    public void StartClientServer(){

        Registry reg = null;
        try {
            reg = LocateRegistry.createRegistry(Constant.Remote_port);
            reg.bind(Constant.Remote_ID,this);

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    public  boolean checkConnectDB() throws SQLException {
        boolean isconnected = false;

                synchronized (connectionLock){

                        try {
                             connection = connectionPool.getConnection();

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
                }

        return isconnected;
    }

    // this is used by the server
    @Override
    public  boolean checkDatabase() throws RemoteException, SQLException {

        synchronized (lock4){
            return checkConnectDB();
        }
    }



    @Override
    public boolean updateStaffInfo(StaffInfo staffInfo, String oldUsername) throws RemoteException {
        boolean isUpdated = false;
        String sqlAccount = "Update account set user = ?, password = ? where id = ?";
        String sqlStaffInfo = "Update client set name=?,  address = ?, contactno =?,  accountid =?";

        synchronized (updateStaffLock){
                        try {
                            System.out.println("updatingg .... ");
                            connection = connectionPool.getConnection();
                            PreparedStatement ps = connection.prepareStatement(sqlAccount);

                            ps.setString(1, staffInfo.getUsername());
                            ps.setString(2, staffInfo.getPassword());
                            ps.setInt(3, staffInfo.getAccountID());

                            PreparedStatement psStaff = connection.prepareStatement(sqlStaffInfo);

                            psStaff.setString(1, staffInfo.getName());
                            psStaff.setString(2, staffInfo.getAddress());
                            psStaff.setString(3, staffInfo.getContact());
                            psStaff.setInt(4, staffInfo.getAccountID());

                            // execution
                            int affectedRowPS = ps.executeUpdate();
                            int affectedEowpsStaff = ps.executeUpdate();

                            if (affectedRowPS == 1 && affectedEowpsStaff == 1){
                                isUpdated = onlineClientArrayList.updateUsername(oldUsername, staffInfo.getUsername());
                            }else {
                                isUpdated =   false;
                            }

                        } catch (SQLException e) {
                            isUpdated =   false;
                            e.printStackTrace();
                        }
             }


        return isUpdated;
    }

    @Override
    public ArrayList searchedList(String name) throws RemoteException {
        searchList = new ArrayList<>();

        synchronized (searchLock){
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            searchList = getSearchList(name);

                        }
                    });

                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


            if (searchList.isEmpty()){
                System.out.println("empty at searchlist");
                searchList.clear();
            }else {
                System.out.println("not empty at searchlist");
            }

        }

        return searchList;
    }

    // SYNCHRONIZATION DEPEND ON SEARCHLIST
    private ArrayList getSearchList(String name){
        ArrayList list  = new ArrayList();

        //SELECT * from family where (Lower(name) like  Lower('Orville%'))
        String sqlfamily  = "SELECT * from family where (Lower(name) like ? or Lower (spouse) like ?) ";
        String sqlgetbarangay = "Select name from barangay where id = ?";
        String sqlfamPoverty = "SELECT * from povertyfactors where familyid = ? ";


        try {
            Connection connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sqlfamily);
            ps.setString(1, name.toLowerCase() + "%");
            ps.setString(2,name.toLowerCase()+ "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()){

                // get family information
                int familyid = rs.getInt("id");
                int clientid = rs.getInt("clientid");
                int barangayid = rs.getInt("barangayid");
                String date = rs.getString("date");
                String Name = rs.getString("name");
                String maritalStat = rs.getString("maritalstatus");
                String age = rs.getString("age");
                String spouse = rs.getString("spouse");
                String address = rs.getString("address");
                int childrenNo = rs.getInt("childrenno");
                String gender = rs.getString("gender");
                int yrResidency = rs.getInt("yrresidency");
                LocalDate YrIssued = StringToLocalDate(rs.getString("yrissued"));


                System.out.println(barangayid);

                // get the barangay name
                ps = connection.prepareStatement(sqlgetbarangay);
                ps.setInt(1, barangayid);

                ResultSet barangayRS = ps.executeQuery();

                barangayRS.next();
                String barangayName = barangayRS.getString("name");

                // create object family
                FamilyInfo familyinfo = new FamilyInfo(clientid, date, YrIssued, yrResidency,
                        childrenNo, Name, spouse, age, maritalStat, barangayName, gender, address);
                familyinfo.setfamilyId(familyid);

                // get family poverty factors information
                ps = connection.prepareStatement(sqlfamPoverty);
                ps.setInt(1, familyid);
                ResultSet povertyRS = ps.executeQuery();

                povertyRS.next();
                String hasOtherIncome = povertyRS.getString("otherincome");
                String isBelow8k = povertyRS.getString("threshold");
                String ownership = povertyRS.getString("ownership");
                String occupancy = povertyRS.getString("occupancy");
                String isUnderEmployed = povertyRS.getString("underemployed");
                String schooldChildren = povertyRS.getString("schoolchildren");
                LocalDate year = StringToLocalDate(povertyRS.getString("year"));

                int month = povertyRS.getInt("month");

                FamilyPoverty familyPoverty = new FamilyPoverty(hasOtherIncome, isBelow8k,
                        ownership, occupancy, isUnderEmployed, schooldChildren, year, month);

                Family fam = new Family(familyinfo, familyPoverty);

                list.add(fam);

            }

            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (list.isEmpty()){
            list.clear();
            System.out.println("empty at getSearchlist");
        }else{
            System.out.println("list is not empty at getSearchList");
        }


        return list;
    }


    @Override
    public int getPendingAccounts() throws RemoteException {
        int numberOfPending = 0;
        String sql = "Select count(id) from account where RequestStatus = 'Pending'";


                synchronized (pendingAccountLock){

                    try {
                        connection = connectionPool.getConnection();

                        PreparedStatement ps = connection.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery();
                        rs.next();
                        numberOfPending = rs.getInt(1);

                        connection.close();

                    }catch (SQLException e) {
                        e.printStackTrace();
                    }

                }

        return numberOfPending;
    }

    @Override
    public ArrayList getRequestAccounts() throws RemoteException {
        ArrayList<RequestAccounts> requestList = new ArrayList();
            String sql = "SELECT  name, accountid  FROM client C\n" +
                    "LEFT JOIN account A ON C.accountid = A.id\n" +
                    "WHERE A.requestStatus = 'Pending'\n";


            synchronized (requestAccountLock){
                    try {
                        connection = connectionPool.getConnection();
                        PreparedStatement ps = connection.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery();

                        while (rs.next()){
                            RequestAccounts ra = new RequestAccounts(rs.getString("Name"), rs.getInt("accountid"));
                            requestList.add(ra);
                        }

                        connection.close();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
            }

        return requestList;
    }

    // Use for tableItemListener
    @Override
    public boolean Approve(RequestAccounts ra) {
        boolean isApproved = false;

        String sql = "Update account SET requestStatus = 'Approved' WHERE id = ?";

                synchronized (ApprovedLock){
                        try {
                            connection = connectionPool.getConnection();

                            PreparedStatement ps = connection.prepareStatement(sql);
                            ps.setInt(1,ra.getId());

                            int row = ps.executeUpdate();

                                    if (row >= 1){
                                        isApproved = true;
                                    }else {
                                        isApproved = false;
                                    }

                            connection.close();
                        } catch (SQLException e) {
                            isApproved = true;
                            e.printStackTrace();
                        }
                }

        return isApproved;
    }

    @Override
    public boolean ApproveAdmin(RequestAccounts ra)  {
        boolean isActivated = false;

        String sql = "Update account set requeststatus = 'Approved', Role = 'Admin' where id= ?";

                synchronized (ApprovedAdminLock){
                            try {
                                connection = connectionPool.getConnection();

                                PreparedStatement ps = connection.prepareStatement(sql);
                                ps.setInt(1,ra.getId());


                                int row  = ps.executeUpdate();

                                            if (row >= 1){
                                                isActivated = true;

                                            }else {
                                                isActivated = false;
                                            }



                            } catch (SQLException e) {
                                isActivated = false;
                                e.printStackTrace();
                            }finally {
                                try {
                                    connection.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }

                            }
                }

        return isActivated;
    }

    @Override
    public boolean Reject(RequestAccounts ra) {
        boolean isRejected = false;
        String sql = "Update account set requeststatus = 'Rejected' where id = ?";

            synchronized (RejectLock){
                try {
                    connection = connectionPool.getConnection();

                    PreparedStatement  ps = connection.prepareStatement(sql);
                    ps.setInt(1,ra.getId());

                    int row = ps.executeUpdate();

                            if (row >= 1){
                                isRejected = true;
                            }else {
                                isRejected = false;
                            }

                } catch (SQLException e) {
                    isRejected = false;
                    e.printStackTrace();
                }


            }


        return isRejected;
    }


    //METHODS THAT ARE NEED TO BE SYNCRONIZED

    @Override
    public StaffInfo Login(String user, String pass, String ip, int port, String Remote_ID) throws RemoteException {
        System.out.println("Login method called port number:" + port);

        REMOTE_ID = Remote_ID;
        PORT = port;

        username = user;
        StaffInfo staffInfo = new StaffInfo(false,0,null,null,null,null,null,null,null,0);

        synchronized (lock3){
            String loginSql = "SELECT id, User, password, status, role from account where User = ? and password = ?" +
                    "and RequestStatus = 'Approved'";
            String updateStatus = "UPDATE account SET status = ? WHERE User = ? and password = ?";

            try {
              Connection  connection = connectionPool.getConnection();
                PreparedStatement ps = connection.prepareStatement(loginSql);
                ps.setString(1,user);
                ps.setString(2, pass);


                // execute queries
                ResultSet rs = ps.executeQuery();

                //  the record exist
                if (rs.next()){
                    String username = rs.getString("User");
                    String password = rs.getString("password");


                    // check if the record match
                    if (username.equals(user) && password.equals(pass)){
                        AccountID = rs.getInt("id");
                        role = rs.getString("Role");
                        status = rs.getString("Status");
                        globalPassword = pass;
                        ipAddress = ip;

                        // set the account status to online
                        PreparedStatement updatePS = connection.prepareStatement(updateStatus);
                        updatePS.setString(1,"Online");
                        updatePS.setString(2,user);
                        updatePS.setString(3, pass);
                        updatePS.executeUpdate();

                        // decide whether admin or client to return
                                    if (role.equals("Client")){
                                                staffInfo = getClientInformation(connection);
                                            }
                                    else if (role.equals("Admin")){
                                                staffInfo = getAdminInfo(connection);
                                    }

                    // record did not match
                    }else{
                        System.out.println("exlse1");
                    }
                // record is not existing
                }else {
                    System.out.println("exlse");

                }
                System.out.println("Active Connections:" + connectionPool.getActiveConnections());
                connection.close();
                System.out.println("Active Connections:" + connectionPool.getActiveConnections());
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    return staffInfo;
    }



    // SYNCHRONIZATION DEPENDS ON LOGINN
    private StaffInfo getAdminInfo(Connection connection) throws SQLException {
        StaffInfo  staffInfo = new StaffInfo(false,0,null, null,null,null,null,null,null,0);

        try {

            String sql = "Select name,contact from admin where accountId = ?";
                    PreparedStatement ps = connection.prepareStatement(sql);
                    ps.setInt(1, AccountID);

                    ResultSet rs = ps.executeQuery();
                    rs.next();

                    String name = rs.getString("Name");
                    String contact = rs.getString("Contact");

                                staffInfo.setAccountExist(true);
                                staffInfo.setAccountID(AccountID);
                                staffInfo.setStatus("offline");
                                staffInfo.setRole(role);
                                staffInfo.setName(name);
                                staffInfo.setUsername(username);
                                staffInfo.setPassword(globalPassword);
                                staffInfo.setAddress("");
                                staffInfo.setContact(contact);
                    System.out.println("successfully login ass admin");

            }catch (SQLException e) {
                    e.printStackTrace();
            }


        return staffInfo;
    }


    // SYNCHRONIZATION DEPENDS ON LOGINN
    private StaffInfo getClientInformation(Connection connection){
        StaffInfo  staffInfo = new StaffInfo(false,0,null, null,null,null,null,null,null,0);

        int x = 0;

            try {
                    String getInfoSql = "SELECT name, address, contactno, totalentries FROM client WHERE accountid = ?";

                    PreparedStatement getPS = connection.prepareStatement(getInfoSql);
                    getPS.setInt(1, AccountID);

                    ResultSet getRS = getPS.executeQuery();

                    getRS.next();
                                    String name = getRS.getString("name");
                                    String address = getRS.getString("address");
                                    String contacno = getRS.getString("contactno");
                                    int totalentries = getRS.getInt("totalentries");

                                                    if (onlineClientArrayList.isEmpty()){
                                                                    staffInfo.setAccountExist(true);
                                                                    staffInfo.setAccountID(AccountID);
                                                                    staffInfo.setStatus("offline");
                                                                    staffInfo.setRole(role);
                                                                    staffInfo.setName(name);
                                                                    staffInfo.setUsername(username);
                                                                    staffInfo.setPassword(globalPassword);
                                                                    staffInfo.setAddress(address);
                                                                    staffInfo.setContact(contacno);
                                                                    staffInfo.setEntries(totalentries);

                                                                    // add this account to list
                                                                    OnlineClient onlineClient = new OnlineClient(username,ipAddress, PORT, REMOTE_ID);
                                                                    onlineClientArrayList.add(onlineClient);
                                                                    System.out.println("first online");
                                                                    return staffInfo;

                                                    }else {
                                                                while (x <= onlineClientArrayList.size() -1){
                                                                    System.out.println("Starting too loop");

                                                                    OnlineClient client  = onlineClientArrayList.get(x);
                                                                    System.out.println(client.getUsername()+ "=="+ username);
                                                                    System.out.println(status);


                                                                            if (client.getUsername().equals(username) && status.equals("Online")){

                                                                                // indicate that client is already online
                                                                                System.out.println(client.getUsername() + " is already Online");
                                                                                staffInfo.setAccountExist(true);
                                                                                staffInfo.setStatus("Online");

                                                                                return staffInfo;

                                                                            }else{
                                                                                // if the username is not existing in the list

                                                                                staffInfo.setAccountExist(true);
                                                                                staffInfo.setAccountID(AccountID);
                                                                                staffInfo.setStatus("offline");
                                                                                staffInfo.setRole(role);
                                                                                staffInfo.setName(name);
                                                                                staffInfo.setUsername(username);
                                                                                staffInfo.setPassword(globalPassword);
                                                                                staffInfo.setAddress(address);
                                                                                staffInfo.setContact(contacno);
                                                                                staffInfo.setEntries(totalentries);

                                                                                // add this to client list
                                                                                OnlineClient onlineClient = new OnlineClient(username,ipAddress, PORT, REMOTE_ID);
                                                                                onlineClientArrayList.add(onlineClient);
                                                                                System.out.println("Successfully Login");

                                                                                 x++;

                                                                                return  staffInfo;

                                                                            }


                                                                }

                                                    }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return staffInfo;
        }



    @Override
    public boolean register(StaffRegister staffRegister) throws RemoteException {

        synchronized (lock2){

            int accountID = 0;
            int secretInfoID = 0;

            /// insert account of the encode here

            String insertAccount = "INSERT INTO account (User,password,RequestStatus, Status) VALUES (?,?,?,?)";
            try {
                connection = connectionPool.getConnection();
                PreparedStatement ps = connection.prepareStatement(insertAccount, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1,staffRegister.getUsername());
                ps.setString(2,staffRegister.getPassword());
                ps.setString(3,"Pending");
                ps.setString(4,"Offline");


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
                connection = connectionPool.getConnection();
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
    public void Logout(int accountID, String username) throws RemoteException {
        String logout = "Update account set status = ? where id = ?";

        synchronized (logoutLock){
            try {
                connection = connectionPool.getConnection();

                PreparedStatement ps = connection.prepareStatement(logout);
                ps.setString(1,"offline");
                ps.setInt(2,accountID);

                ps.executeUpdate();

                ps.close();
                connection.close();

                onlineClientArrayList.removeUserToList(username);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }
    /*
     represents method name, which can distinguish
     Search from notifyclient in addFamilyInfo method
     */
    public String getMethodIdentifier() {
        return methodIdentifier;
    }

    public void setMethodIdentifier(String methodIdentifier) {
        this.methodIdentifier = methodIdentifier;
    }

    @Override
    public String getMethodIdentifiers() throws RemoteException {
        return getMethodIdentifier();
    }


    @Override
    public boolean addFamilyInfo(Family family) throws RemoteException {
         final String SEARCH = "SEARCH";
         final String NOTIFY = "NOTIFY";

          boolean bool = false;

        String checkRecord = "Select name from family where name = ?";

        synchronized (FamilyLock) {
            try {
                connection = connectionPool.getConnection();

                PreparedStatement ps = connection.prepareStatement(checkRecord);
                ps.setString(1,family.getFamilyinfo().getName());

                ResultSet rs = ps.executeQuery();

                        if (rs.next()){
                            notifyClient(family);
                            setMethodIdentifier(NOTIFY);
                        }else{
                         bool =  addToBarangay(family);
                            setMethodIdentifier(SEARCH);
                        }

            } catch (SQLException e) {
                e.printStackTrace();

            }finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return bool;
    }

    // Date column represent YEAR!
    private boolean addToBarangay(Family family){
    boolean isValid = false;

        String insertbarangay = "Insert INTO barangay (name,date,month,unresolvepopulation,resolvepopulation) VALUES " +
                "(?,?,?,?,?)";
        String chckBarangay = "Select id from barangay where name = ? and date = ? and month = ?";
        String updateBarangay = "Update barangay  SET Unresolvepopulation = Unresolvepopulation + 1 where id = ?";

        int currentMonth = getCurrentMonth();

        int barangayID = 0;

            try {
                // check if barangay is already existing

                PreparedStatement chckPs = connection.prepareStatement(chckBarangay,Statement.RETURN_GENERATED_KEYS);
                chckPs.setString(1,family.getFamilyinfo().getBarangay());
                chckPs.setString(2, family.getFamilyinfo().getSurveyedYr().toString());
                chckPs.setInt(3,currentMonth);


                ResultSet chckRs = chckPs.executeQuery();

                if (chckRs.next()){
                    barangayID = chckRs.getInt(1);

                    //update the barangay
                    PreparedStatement updatePs = connection.prepareStatement(updateBarangay,Statement.RETURN_GENERATED_KEYS);
                    updatePs.setInt(1,barangayID);
                    int row = updatePs.executeUpdate();

                }else {
                    // insert new  barangay record
                    PreparedStatement barangayPS = connection.prepareStatement(insertbarangay, Statement.RETURN_GENERATED_KEYS);
                    barangayPS.setString(1,family.getFamilyinfo().getBarangay());
                    barangayPS.setString(2, family.getFamilyinfo().getSurveyedYr().toString());
                    barangayPS.setInt(3,currentMonth);
                    barangayPS.setInt(4,1);
                    barangayPS.setInt(5,0);

                    int row = barangayPS.executeUpdate();
                    ResultSet barangayRs = barangayPS.getGeneratedKeys();

                    if (row == 1 && barangayRs.next()) {
                        barangayID = barangayRs.getInt(1);
                    }

                    System.out.println("BarangayID from insert barangay "+barangayID);
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

        String addFamilySql = "Insert INTO family (barangayid,date,name,maritalstatus,age,spouse,address,childrenno,gender,yrresidency,yrissued,clientid)" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            int numofChildren = family.getFamilyinfo().getNumofChildren();
            PreparedStatement ps = connection.prepareStatement(addFamilySql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, barangayID);
            ps.setString(2,family.getFamilyinfo().getInputDate());
            ps.setString(3,family.getFamilyinfo().getName());
            ps.setString(4,family.getFamilyinfo().getMaritalStatus());
            ps.setString(5,family.getFamilyinfo().getAge());
            ps.setString(6,family.getFamilyinfo().getSpouseName());
            ps.setString(7,family.getFamilyinfo().getAddress());
            ps.setInt(8,numofChildren);
            ps.setString(9,family.getFamilyinfo().getGender());
            ps.setInt(10, family.getFamilyinfo().getResidencyYr());
            ps.setString(11, family.getFamilyinfo().getSurveyedYr().toString());
            ps.setInt(12,family.getFamilyinfo().getClientID());

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

        String addPovertySql = "Insert Into povertyfactors (Familyid, year, month, occupancy,schoolchildren," +
                "underemployed,otherincome,threshold,ownership) " +
                "Values (?,?,?,?,?,?,?,?,?)";

        //
        try {
            PreparedStatement ps = connection.prepareStatement(addPovertySql);
            ps.setInt(1,familyId);
            ps.setString(2, familyPoverty.getYear().toString());
            ps.setInt(3,familyPoverty.getMonth());
            ps.setString(4,familyPoverty.getOccupancy());
            ps.setString(5,familyPoverty.getChildreninSchool());
            ps.setString(6,familyPoverty.getIsunderEmployed());
            ps.setString(7,familyPoverty.getHasotherIncome());
            ps.setString(8,familyPoverty.getIsbelow8k());
            ps.setString(9,familyPoverty.getOwnership());

            int row = ps.executeUpdate();
                if (row == 1) {
                    isAdded =  true;
                }

        } catch (SQLException e) {
            isAdded = false;
            e.printStackTrace();
        }
        System.out.println("sucessfully added Family");
        return isAdded;
    }

    /*
    notify client that the faily he tried to input
    has existing account
     */
    private void notifyClient(Family family){
        Registry reg = null;

        // get username
        String username = getUsername(family.getFamilyinfo().getClientID());
        OnlineClient client = onlineClientArrayList.getClientCredential(username);

        TimedRMIclientSocketFactory  csf = new TimedRMIclientSocketFactory(6000);
                try {
                        reg = LocateRegistry.getRegistry(System.setProperty("java.rmi.server.hostname",
                                    client.getIpaddress()), client.getPort(), csf);
                        ClientInterface clientInterface = (ClientInterface) reg.lookup(client.getREMOTE_ID());

                        ArrayList commonNameList = searchedList(family.getFamilyinfo().getName());

                          System.out.println("family name from notif : " + family.getFamilyinfo().getName()) ;
                        clientInterface.notifyClient(commonNameList);

                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (NotBoundException e) {
                    e.printStackTrace();
                }

    }
    private String getUsername(int accountID)  {
        String username = "";

            String usernameSQL = "SELECT User FROM account WHERE id = ?";

            try {
              Connection  connection = connectionPool.getConnection();
                PreparedStatement ps = connection.prepareStatement(usernameSQL);
                ps.setInt(1,accountID);

                ResultSet rs = ps.executeQuery();

                if (rs.next()){
                    username = rs.getString("user");
                }
                ps.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }



        return username;
    }


    private int getCurrentMonth(){
        java.util.Date date= new java.util.Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH);

        return month + 1;
    }

    private LocalDate StringToLocalDate(String date){
        LocalDate localDate = LocalDate.parse(date);

        return localDate;
    }


    // getActiveConnection method is just used for development purpose
    public void getActiveConnection(){
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

