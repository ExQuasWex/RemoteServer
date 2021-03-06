package MainApp.ClientSide;

import AdminModel.RequestAccounts;
import MainApp.AdminServer.*;
import Remote.Method.FamilyModel.FamilyHistory;
import MainApp.ClientIntefaceFactory;
import MainApp.DataBase.Database;
import RMI.ClientInterface;
import RMI.Constant;
import RMI.RemoteMethods;
import  Remote.Method.FamilyModel.Family;
import Remote.Method.FamilyModel.FamilyPoverty;
import Remote.Method.FamilyModel.FamilyInfo;

import clientModel.ClientEntries;
import clientModel.StaffInfo;
import clientModel.StaffRegister;
import global.Credentials;
import global.OnlineClient;
import global.SecretDetails;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Logger;
import utility.Utility;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Created by Didoy on 8/24/2015.
 *
 */

public class ClientDB extends UnicastRemoteObject implements RemoteMethods  {

    private boolean isRegistered;

    private PovertyDB povertyDB = new PovertyDB();

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
    private final Object getCredentialLock;
    private final Object startUpLock;
    private final Object securityQustionLock;
    private final Object barangaIdLock;
    private final Object UpdateFamilyLock;

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
        securityQustionLock = new Object();
        barangaIdLock = new Object();
        UpdateFamilyLock = new Object();

        ApprovedLock = new Object();
        ApprovedAdminLock = new Object();
        RejectLock = new Object();

        getCredentialLock = new Object();
        startUpLock = new Object();

        connectionPool = Database.getConnectionPool();
        Database.setMaxConnection(40);

        onlineClientArrayList = OnlineClientArrayList.getInstance();

    }
    public void StartClientServer(){

        Registry reg = null;
        try {

            StartUp();

            reg = LocateRegistry.createRegistry( Constant.Remote_port);
            reg.bind(Constant.Remote_ID,this);

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    private void StartUp(){
        String sql = "select * from generatedport where status = 'Online'";

        Connection connection = null;
        synchronized (startUpLock){


                    try {
                        connection = connectionPool.getConnection();

                        PreparedStatement ps = connection.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery();

                        while (rs.next()){
                            int accountID  = rs.getInt("accountid");
                            int port = rs.getInt("remoteport");
                            String remoteID = rs.getString("remoteid");
                            String ip = rs.getString("ipaddress");

                            String username = getUsername(accountID);

                            OnlineClient onlineClient = new OnlineClient(username, ip, port, remoteID);

                            onlineClientArrayList.add(onlineClient);
                        }
                        onlineClientArrayList.monitorOnlines();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }finally {
                        Utility.closeConnection(connection);
                    }
            }

        }

        public  boolean checkConnectDB() throws SQLException {
            boolean isconnected = false;
            Connection connection = null;

                synchronized (connectionLock){

                        try {
                             connection = connectionPool.getConnection();

                            if (connection.isValid(5000)){
                                isconnected = true;

                            }else{
                                isconnected = false;
                            }

                        } catch (SQLException e) {
                            isconnected = false;
                            e.printStackTrace();
                        }finally {
                            Utility.closeConnection(connection);
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
        String sqlStaffInfo = "Update client set name=?,  address = ?, contact =?,  accountid =?";
        Connection connection = null;
        synchronized (updateStaffLock){
                        try {
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
                        }finally {
                            Utility.closeConnection(connection);
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

    private ArrayList searchSameName(String name){
        Connection connection = null;
        String barangayName;
        ArrayList<Family> list = new ArrayList();
        String sql = "Select id from family where Lower (name) like ?";
        String sqlgetbarangay = "Select barangayid from family where id = ?";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1,  "%" + name.toLowerCase() + "%");
            ResultSet rs = ps.executeQuery();

                    while (rs.next()){
                        int id = rs.getInt(1);

                        FamilyInfo familyinfo = FamilyDB.getFamilyData(id);
                        FamilyPoverty familyPoverty = povertyDB.getFamilyPovertyDataByFamilyId(id);
                        FamilyHistory familyHistory = HistoryDB.getFamilyHistoryById(id);


                        PreparedStatement brgyps = connection.prepareStatement(sqlgetbarangay);
                        brgyps.setInt(1,id);

                        ResultSet brgyRs = brgyps.executeQuery();

                                if (brgyRs.next()){
                                    int brgayID = brgyRs.getInt(1);
                                    barangayName = BarangayDB.getBarangayNameById(brgayID);
                                    familyinfo.setBarangay(barangayName);
                                }

                        Family fam = new Family(familyinfo, familyPoverty, familyHistory);

                        list.add(fam);

                        // notify use for loadbar
                    }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return  list;
    }

    // SYNCHRONIZATION DEPEND ON SEARCHLIST
    private ArrayList getSearchList(String name){
        Connection connection = null;
        FamilyHistory familyHistory = null;
        ArrayList list  = new ArrayList();
        String barangayName;

        String sqlfamily  = "SELECT id from family where (Lower(name) like ? or Lower (spouse) like ? or Lower (lastname) like ? or Lower (middle) like ?)  ";
        String sqlgetbarangay = "Select barangayid from family where id = ?";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement brgyps = connection.prepareStatement(sqlgetbarangay);

            PreparedStatement ps = connection.prepareStatement(sqlfamily);
            ps.setString(1, "%" + name.toLowerCase() + "%");
            ps.setString(2, "%" + name.toLowerCase()+  "%");
            ps.setString(3, "%" + name.toLowerCase()+  "%");
            ps.setString(4, "%" + name.toLowerCase()+  "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()){
                int id = rs.getInt(1);

                FamilyInfo familyinfo = FamilyDB.getFamilyData(id);
                FamilyPoverty familyPoverty = povertyDB.getFamilyPovertyDataByFamilyId(id);
                familyHistory = HistoryDB.getFamilyHistoryById(id);

                brgyps.setInt(1,id);

                ResultSet brgyRs = brgyps.executeQuery();

                if (brgyRs.next()){
                    int brgayID = brgyRs.getInt(1);
                     barangayName = BarangayDB.getBarangayNameById(brgayID);
                     familyinfo.setBarangay(barangayName);
                }

                Family fam = new Family(familyinfo, familyPoverty, familyHistory);

                list.add(fam);

                // notify use for loadbar
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        if (list.isEmpty()){
            list.clear();
            System.out.println("empty at getSearchlist");
        }else{
            System.out.println("list is not empty at getSearchList");
        }


        return list;
    }


    // total numbers of pending accounts
    @Override
    public int getPendingAccounts() throws RemoteException {
                synchronized (pendingAccountLock){
                    return AccountDB.getPendingAccounts();
                }
    }

    @Override
    public ArrayList getRequestAccounts() throws RemoteException {
            synchronized (requestAccountLock){
                return AccountDB.getRequestAccounts();
            }
    }

    //METHODS THAT ARE NEED TO BE SYNCRONIZED

    @Override
    public StaffInfo Login( String user, String pass, String ip, int port, String Remote_ID) throws RemoteException {
        Connection connection = null;
        REMOTE_ID = Remote_ID;
        PORT = port;

        username = user;
        StaffInfo staffInfo = new StaffInfo(false,0,null,null,null,null,null,null,null,0);
        synchronized (lock3){
            String loginSql = "SELECT id, User, password, status, role from account where User = ? and password = ?" +
                    "and RequestStatus = 'APPROVED'";
            String updateStatus = "UPDATE account SET status = ? WHERE User = ? and password = ?";

            try {

                 connection = connectionPool.getConnection();

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

                             staffInfo = getUserInformation(connection);

                    // record did not match
                    }else{
                    }
                // record is not existing
                }else {

                }

            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                System.out.println("Active Connections:" + connectionPool.getActiveConnections());
                Utility.closeConnection(connection);
                System.out.println("Active Connections:" + connectionPool.getActiveConnections());
            }
        }

    return staffInfo;
    }


    // SYNCHRONIZATION DEPENDS ON LOGINN
    private StaffInfo getUserInformation(Connection connection){
        StaffInfo  staffInfo = new StaffInfo(false,0,null, null,null,null,null,null,null,0);

        int x = 0;

            try {
                    String getInfoSql = "SELECT name, address, contact, totalentries FROM client WHERE accountid = ?";

                    PreparedStatement getPS = connection.prepareStatement(getInfoSql);
                    getPS.setInt(1, AccountID);

                    ResultSet getRS = getPS.executeQuery();

                    getRS.next();
                                    String name = getRS.getString("name");
                                    String address = getRS.getString("address");
                                    String contacno = getRS.getString("contact");
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

                                                                    OnlineClient client  = onlineClientArrayList.get(x);
                                                                   // System.out.println(client.getUsername()+ "=="+ username);
                                                                   //System.out.println(status);

                                                                            if (client.getUsername().equals(username) && status.equals("Online")){

                                                                                // indicates that client is already online
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
        Connection connection = null;
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

            String infoSQL = "INSERT INTO request (Name, Address, contact, Gender, AccountID, SecretInfoID) VALUES (?,?,?,?,?,?)";

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
        Connection connection = null;
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
        Connection connection = null;

        String logout = "Update account set status = ? where id = ?";

        synchronized (logoutLock){
            try {
                connection = connectionPool.getConnection();

                PreparedStatement ps = connection.prepareStatement(logout);
                ps.setString(1,"offline");
                ps.setInt(2,accountID);

                ps.executeUpdate();

                ps.close();

                onlineClientArrayList.removeUserToList(username);

                setCredentialStatus("offline", accountID, connection);

            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                Utility.closeConnection(connection);
            }
        }

    }
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
    public boolean addFamilyInfo(boolean instantSave, Family family) throws RemoteException {

        boolean bool = false;
        Connection connection = null;
        synchronized (FamilyLock) {

                try {
                    connection = connectionPool.getConnection();

                    if (instantSave){
                        bool =  addToBarangay(family, connection);
                    }else {
                        bool = addToFamilyWithCheck(family, connection);
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                            Utility.closeConnection(connection);
                }

        }
        return bool;
    }

    private boolean addToFamilyWithCheck(Family family, Connection connection){
        boolean bool = false;
        final String SEARCH = "SEARCH";
        final String NOTIFY = "NOTIFY";
        String checkRecord = "Select name from family where name = ?";

        try {

            PreparedStatement ps = connection.prepareStatement(checkRecord);
            ps.setString(1,family.getFamilyinfo().getName());

            ResultSet rs = ps.executeQuery();

                    if (rs.next()){
                        notifyClient(family);
                        setMethodIdentifier(NOTIFY);
                    }else{
                        bool =  addToBarangay(family, connection);
                        setMethodIdentifier(SEARCH);
                    }

        } catch (SQLException e) {
            e.printStackTrace();

        }

        return bool;
    }


    private boolean addToBarangay(Family family, Connection connection){
        return BarangayDB.addToBarangay(family, connection);
    }


    /*
    notify client that the family he tried to input
    has existing account
     */
    private void notifyClient(Family family){

        // get username
        String username = getUsername(family.getFamilyinfo().getClientID());
        OnlineClient client = onlineClientArrayList.getClientCredential(username);

                try {
                    String host = client.getIpaddress();
                    int port = client.getPort();

                          ArrayList commonNameList = searchSameName(family.getFamilyinfo().getName());

                          ClientInterface clientInterface = ClientIntefaceFactory.getClientInterface(client, host, port);

                          clientInterface.notifyClient(commonNameList);

                } catch (RemoteException e) {
                    e.printStackTrace();
                }

    }

    private String getUsername(int accountID)  {
        return AccountDB.getUsername(accountID);
    }

    @Override
    public Credentials getCredentials(String username, String ipAddress) throws RemoteException {
        Credentials credentials = null;
        String sql = "Select id from account where user = ?";
        Connection connection = null;

                synchronized (getCredentialLock){
                    this.ipAddress = ipAddress;

                    try {

                        connection = connectionPool.getConnection();

                            PreparedStatement ps = connection.prepareStatement(sql);
                            ps.setString(1,username);

                            ResultSet rs = ps.executeQuery();

                                    if (rs.next()){
                                        int accountID = rs.getInt("id");
                                        credentials = generateCredentials(accountID, connection );

                                    }

                    } catch (SQLException e) {
                            e.printStackTrace();
                    }finally {
                        Utility.closeConnection(connection);
                    }
                }

        return credentials;
    }

    //generate credenttial for client users, compose of remoteID and remoteport
    private Credentials generateCredentials(int accountID, Connection connection ){
        Credentials credentials = null;
        int remotePort = 0;
        String remoteID = "";

        String sql = "Select remoteport, remoteid from generatedport where accountid=?";
        String saveCredentials = "Insert into generatedport (accountid, remoteport, remoteid, ipaddress) values (?,?,?, ?)";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1,accountID);

            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                remotePort = rs.getInt("remoteport");
                remoteID = rs.getString("remoteid");

                AccountDB.updateClientIpAddress(ipAddress, accountID, connection);
            }else {
                remotePort = generatePort();
                remoteID = generateRemoteID();

                ps = connection.prepareStatement(saveCredentials);
                ps.setInt(1,accountID);
                ps.setInt(2, remotePort);
                ps.setString(3, remoteID);
                ps.setString(4, ipAddress);

                ps.executeUpdate();

            }
            credentials = new Credentials(remoteID, remotePort);

            setCredentialStatus("Online", accountID, connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return credentials;
    }

    private void setCredentialStatus(String status, int Accountid, Connection connection){
        String sql = "Update generatedport set status = ? where accountid = ? ";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, status);
            ps.setInt(2, Accountid);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

    }
    private int generatePort(){
        int port = 0;

        Random ran = new Random();

        for (int i = 0; i < 10; i++) {
            port= (10000 + ran.nextInt(50000));
        }

        return port;
    }

    private String generateRemoteID(){
        int ID = 0;

        Random ran = new Random();

        for (int i = 0; i < 10; i++) {
            ID= (1000 + ran.nextInt(3560));
        }

        String Remoteid = String.valueOf(ID);

        return Remoteid;
    }


    @Override
    public SecretDetails getSecurityQuestion(String hint1) throws RemoteException {
        Connection connection = null;

        String password = "";
        int secretID = 0;
        SecretDetails secretDetails = null;

        String secretSQL = "Select secretquestionid, secretanswer from secretinfo where id = ?";
        String sql = "Select secretinfoID, A.password from client C\n" +
                "  Left Join account A\n" +
                "  On A.id = C.accountid \n" +
                "\n" +
                "where A.user = ?";

            synchronized (securityQustionLock){

                try {
                    connection = connectionPool.getConnection();
                    PreparedStatement ps = connection.prepareStatement(sql);
                    ps.setString(1, hint1);

                    ResultSet rs = ps.executeQuery();

                          if (rs.next()){
                              secretID = rs.getInt("secretinfoID");

                              ps = connection.prepareStatement(secretSQL);
                              ps.setInt(1, secretID);

                              password = rs.getString("password");

                              rs = ps.executeQuery();

                                  if (rs.next()){
                                      int secretId = rs.getInt("secretquestionid");
                                      String secretAns = rs.getString("secretanswer");

                                      secretDetails = new SecretDetails(secretId, secretAns, password);
                                  }

                          }

                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                    Utility.closeConnection(connection);
                }

            }

        return secretDetails;
    }

    @Override
    public boolean UpdateFamilyInformation(Family family) throws RemoteException {
        boolean isEdited = false;
        Connection connection = null;
        synchronized (UpdateFamilyLock){

                try {
                    int oldBarangayID = 0;
                    int newBarangayID = 0;
                    String barangayName = family.getFamilyinfo().getBarangay();
                    LocalDate date = family.getFamilyinfo().getSurveyedYr();

                    connection = connectionPool.getConnection();

                    boolean exist = isBarangayExisting(barangayName, date.toString(), connection);

                            if (exist){
                                newBarangayID = getBarangayID(barangayName , date.toString() );
                            }else {
                                newBarangayID = createNewBarangay(barangayName, date);
                            }

                    oldBarangayID = getBarangayID(family.getFamilyinfo().familyId());

                            updateFamily(family.getFamilyinfo(), newBarangayID, connection);
                            updateFamilyPoverty(family.getFamilypoverty(), family.getFamilyinfo().familyId(), connection);

                    if (oldBarangayID != newBarangayID){
                        updateFamilyBarangay(oldBarangayID, newBarangayID, date, connection);
                    }

                    isEdited = true;

                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                    Utility.closeConnection(connection);
                }
        }

        return isEdited;
    }

    @Override
    public void getClientEntries(int ClientID) throws RemoteException {
        String username = getUsername(ClientID);
        OnlineClient client = onlineClientArrayList.getClientCredential(username);
        ClientEntries clientEntries = null;

        int size = getClientEntryMaxSize(ClientID);

        String host = client.getIpaddress();
        int port = client.getPort();

        ClientInterface clientInterface = ClientIntefaceFactory.getClientInterface(client, host, port);
        clientInterface.setClientEntriesMaxSize(size);

        String sql = "Select id, name, date from family where clientid = ?";

        try {
            Connection connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, ClientID);

            ResultSet rs = ps.executeQuery();

            while (rs.next()){
                int     id = rs.getInt("id");
                String  name = rs.getString("name");
                String  date =  rs.getString("date");

                 clientEntries =  new ClientEntries(id, name, date);
                 clientInterface.addClientEntry(clientEntries);
                }

            Utility.closeConnection(connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private int getClientEntryMaxSize(int ClientID){
        int size = 0;
        String sql = "Select Count(id) from family where clientid = ?";

        try {
            Connection connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, ClientID);

            ResultSet rs  = ps.executeQuery();

            rs.next();
            size = rs.getInt(1);

            Utility.closeConnection(connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return  size;
    }

    private void updateFamily(FamilyInfo familyInfo, int barangayiD, Connection connection){
        FamilyDB.updateFamily(familyInfo, barangayiD, connection);
    }

    private void updateFamilyPoverty(FamilyPoverty familyPoverty, int familyID, Connection connection){
        PovertyDB.updateFamilyPoverty(familyPoverty, familyID, connection);
    }

    private void updateFamilyBarangay(int oldBarangayID, int newBarangayID, LocalDate date, Connection connection){
        BarangayDB.updateFamilyBarangay(oldBarangayID, newBarangayID, date, connection);
    }

    private int getBarangayID(int familyID){

        synchronized (barangaIdLock){
            return BarangayDB.getBarangayID(familyID);
        }
    }

    private int getBarangayID(String barangayName, String date){
            return BarangayDB.getBarangayID(barangayName, date);
    }

    private boolean isBarangayExisting(String barangayName, String date, Connection connection){
        return BarangayDB.isBarangayExisting(barangayName, date, connection);
    }

    private int createNewBarangay(String barangayName, LocalDate date){
        return BarangayDB.createNewBarangay(barangayName, date);
    }


}

