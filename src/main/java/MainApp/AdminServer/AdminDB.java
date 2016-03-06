package MainApp.AdminServer;

import AdminModel.Enum.AccountApproveStatus;
import AdminModel.Enum.AccountStatus;
import AdminModel.Enum.FactorCategoryParameter;
import AdminModel.Enum.ReportCategoryMethod;
import AdminModel.Params;
import AdminModel.Report.Children.Model.ResponsePovertyFactor;
import AdminModel.Report.Parent.ResponseCompareOverview;
import AdminModel.Report.Parent.ResponseSpecific;
import AdminModel.Report.Parent.ResponseSpecificOverView;
import AdminModel.Report.Parent.ResponseOverviewReport;
import AdminModel.ResponseModel.ActiveAccounts;
import BarangayData.BarangayData;
import DecisionSupport.Prioritizer;
import MainApp.ClientSide.OnlineClientArrayList;
import MainApp.DataBase.Database;
import MainApp.Preferences.Preference;
import PriorityModels.PriorityLevel;
import PriorityModels.PriorityType;
import RMI.AdminInterface;
import RMI.Constant;
import Remote.Method.FamilyModel.Family;
import Remote.Method.FamilyModel.FamilyInfo;
import Remote.Method.FamilyModel.FamilyPoverty;
import javafx.scene.control.Alert;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.io.File;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


/**
 * Created by Didoy on 11/26/2015.
 */
public class AdminDB extends UnicastRemoteObject implements AdminInterface {

    private Object lock1;

    private Connection connection;

    private static JdbcConnectionPool connectionPool;

    private Object connectionLock;
    private Object overviewLock;
    private Object activeAccount;
    private Object historyLock;

    private BarangayDB barangayData = new BarangayDB();
    private PovertyFactorsData povertyFactorsData = new PovertyFactorsData();

    public AdminDB() throws RemoteException {

        connectionLock = new Object();
        lock1 = new Object();
        overviewLock = new Object();
        activeAccount = new Object();
        historyLock = new Object();

        connectionPool = Database.getConnectionPool();
        Database.setMaxConnection(40);

    }

    public void StartAdminServer(){

        Registry reg = null;
        try {
            Registry reg2 = LocateRegistry.createRegistry(Constant.Adminport);
            reg2.bind(Constant.RMIAdminID, this);

            getYears();

        } catch (RemoteException e) {
            Utility.showConfirmationMessage("Server can only run one instance at the same time", Alert.AlertType.ERROR);

            System.exit(0);
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
        @Override
    public  boolean checkConnectDB()  {
        boolean isconnected = false;

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

    @Override
    public ResponseOverviewReport getOverViewData(Params params, String type) throws RemoteException {

        ArrayList povertyPopulation = new ArrayList();
        ArrayList factorList = new ArrayList();
        ResponseOverviewReport responseOverviewReport = null ;

            synchronized (overviewLock){
                        String currentYear = Utility.getCurrentYear();

                povertyPopulation = barangayData.getOverViewPopulation(currentYear);
                factorList = povertyFactorsData.getOverViewFactors(currentYear);

                        responseOverviewReport = new ResponseOverviewReport(factorList, povertyPopulation);

            }

        return responseOverviewReport;
    }

    @Override
    public ResponseCompareOverview getCompareOverViewData(Params params, String type) throws RemoteException {

        ResponseCompareOverview compareOverview = null;
        int totalPovertyYearOne = 0;
        int totalPovertyYearTwo = 0;
        ArrayList factorList1 = new ArrayList();
        ArrayList factorList2 = new ArrayList();

             totalPovertyYearOne = barangayData.getPovertyCompareOverVieData(params.getYear());
             totalPovertyYearTwo = barangayData.getPovertyCompareOverVieData(params.getMaxYear());

             factorList1 = povertyFactorsData.getOverViewFactors(String.valueOf(params.getYear()));
             factorList2 = povertyFactorsData.getOverViewFactors(String.valueOf(params.getYear()));

        compareOverview =  new ResponseCompareOverview(totalPovertyYearOne, totalPovertyYearTwo, factorList1, factorList2 );

        return compareOverview;
    }

    @Override
    public ResponseCompareOverview getCompareSpecificData(Params params, String type) throws RemoteException {

        ResponseCompareOverview compareOverview = null;
        int totalPovertyBarangayOne = 0;
        int totalPovertyBarangayTwo = 0;
        ArrayList factorList1 = null;
        ArrayList factorList2 = null;

        String yr1 = String.valueOf(params.getYear());
        String yr2 = String.valueOf(params.getMaxYear());

        totalPovertyBarangayOne = barangayData.getPovertyCompareSpecificData(yr1, params.getBarangay1());
        totalPovertyBarangayTwo = barangayData.getPovertyCompareSpecificData(yr2, params.getBarangay2());

        factorList1 = povertyFactorsData.getCompareSpecificFactors(yr1, params.getBarangay1());
        factorList2 = povertyFactorsData.getCompareSpecificFactors(yr2, params.getBarangay2());

        compareOverview =  new ResponseCompareOverview(totalPovertyBarangayOne, totalPovertyBarangayTwo, factorList1, factorList2 );

        return compareOverview;
    }

    @Override
    public ResponseSpecificOverView getSpecificOverViewData(Params params, String type) throws RemoteException {
        String yr = String.valueOf(params.getYear());

        ArrayList monthlyPopulationList = barangayData.getPovertySpecificOverviewData(params);
        ArrayList factorList = povertyFactorsData.getSpecificOverViewData(yr, params.getBarangay1());

        ResponseSpecificOverView responseSpecificOverView = new ResponseSpecificOverView(monthlyPopulationList, factorList);

        return responseSpecificOverView;
    }

    @Override
    public ResponseSpecific getSpecific(Params params, String type) throws RemoteException {

        final  String date = params.getDate();
        String barangayName = params.getBarangay1();

        int population = barangayData.getPovertyCompareSpecificData(date,barangayName );
         ResponsePovertyFactor povertyFactor = povertyFactorsData.getSpecificFactors(date, barangayName);

        ResponseSpecific responseSpecific = new ResponseSpecific(population, povertyFactor);

        return responseSpecific;
    }

    @Override
    public ArrayList getYears() throws RemoteException {
        ArrayList yearList = new ArrayList();
        String sql = "Select date from barangay";

        // add Sycnhronization
        try {
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                String year = rs.getString("date").substring(0,4);
                if (!yearList.contains(year)){
                    yearList.add(year);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return yearList;
    }

    // ADD THREAD HERE
    @Override
    public ArrayList getBarangayData(String year) throws RemoteException {

        Connection connection = null;
        FamilyInfo familyInfo = null;
        FamilyPoverty familyPoverty = null;

        String brngayName;
        int unresolvePopulation;
        int resolvepopulation ;

        ArrayList<FamilyInfo> familyInfoArrayList= new ArrayList<FamilyInfo>();
        ArrayList<FamilyPoverty> familyPovertyArrayList= new ArrayList<FamilyPoverty>();

        ArrayList<BarangayData> barangayDataList = new ArrayList<BarangayData>();

            String sql = "SELECT\n" +
                    "  name,\n" +
                    "  sum(DISTINCT  unresolvepopulation) as unresolvepopulation ,\n" +
                    "  sum(DISTINCT resolvepopulation) as resolvepopulation \n" +
                    "FROM barangay\n" +
                    "\n" +
                    "WHERE date LIKE ? GROUP BY name";

                synchronized (lock1){

                    try {
                        connection = connectionPool.getConnection();

                        PreparedStatement ps = connection.prepareStatement(sql);
                        ps.setString(1, Utility.getCurrentYear() + "%");

                        ResultSet rs = ps.executeQuery();

                        while (rs.next()){
                             brngayName = rs.getString("name");
                             unresolvePopulation = rs.getInt("unresolvepopulation");
                             resolvepopulation = rs.getInt("resolvepopulation");

                                    ArrayList<Integer> idList = FamilyDB.getFamilyIdList(year, brngayName);

                                        for(Integer id: idList){
                                            familyInfo = FamilyDB.getFamilyData(id);
                                            familyPoverty = PovertyDB.getFamilyPovertyDataByFamilyId(id);

                                            int children = familyInfo.getNumofChildren();
                                            familyPoverty =  Prioritizer.addPriorityLevel(familyPoverty, children);

                                            familyInfoArrayList.add(familyInfo);
                                            familyPovertyArrayList.add(familyPoverty);

                                        }
                            PriorityLevel priorityLevel = Prioritizer.getBarangayPriorityLevel(familyPovertyArrayList);
                            PriorityType  priorityType   = Prioritizer.getBarangayPriorityType(familyPovertyArrayList);

                            BarangayData barangayData = new BarangayData(brngayName, unresolvePopulation,
                                    resolvepopulation, priorityLevel, priorityType);

                            barangayDataList.add(barangayData);
                        }

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }finally {
                        Utility.closeConnection(connection);
                    }
                }

        return barangayDataList;
    }


    @Override
    public  ArrayList getFamilyBarangay(Params params, ReportCategoryMethod method) throws RemoteException {
        Connection connection = null;
        PreparedStatement ps = null;
        ArrayList<Family> list = new ArrayList();

        try {
            connection = connectionPool.getConnection();

            ps = getPrepareStatement(params, method, connection );

            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                int id = rs.getInt("id");
                FamilyInfo familyInfo = FamilyDB.getFamilyData(id);
                FamilyPoverty familyPoverty = PovertyDB.getFamilyPovertyDataByFamilyId(id);

                Family family = new Family(familyInfo, familyPoverty );
                list.add(family);
                // notify admin user to create progressbar
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Utility.closeConnection(connection);
        }

        return  list;

    }

    @Override
    public ArrayList getActiveAccounts( ) throws RemoteException {
        Connection connection = null;
        ArrayList list = new ArrayList();
        String sql = "\n" +
                "SELECT A.id, user, C.name, A.requeststatus, A.status FROM account A \n" +
                "LEFT JOIN client C ON C.accountid = A.id\n" +
                "WHERE (A.requeststatus = 'APPROVED' or A.requeststatus = 'DISABLE'  or A.requeststatus = 'REJECT') and A.role = 'Client'";

            synchronized (activeAccount){

                try {
                    connection = connectionPool.getConnection();
                    PreparedStatement ps = connection.prepareStatement(sql);

                    ResultSet rs = ps.executeQuery();

                            while (rs.next()){
                                int id = rs.getInt("id");
                                String name = rs.getString("Name");
                                String username = rs.getString("user");
                                String aCstatus = rs.getString("requeststatus");
                                String status = rs.getString("status");

                                list.add(new ActiveAccounts(id,username, name, aCstatus, status));

                            }

                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                    Utility.closeConnection(connection);
                }

            }

        return list;
    }

    @Override
    public boolean addHistoryToFamily(Family family) throws RemoteException {

        synchronized (historyLock){

            int barangayId = family.getFamilyinfo().getBarangayID();
            boolean isAdded = BarangayDB.addResolvePopulationById(barangayId);
            boolean isAdded2 = HistoryDB.addHistoryToFamily(family);

                if (isAdded && isAdded2){
                    return true;
                }else {
                    return false;
                }
            }
        }

    @Override
    public File getBackUp() throws RemoteException {
        String path = Preference.getDBpath();
        File file = new File(path + "copy");
        return file;
    }

    @Override
    public boolean updateAccountStatus(int id, AccountStatus status) throws RemoteException {
        if (status == AccountStatus.DELETE){
            return AccountDB.deleteAccount(id);
        }else {
            return  AccountDB.updateAccountStatusByID(id, status);
        }

    }

    @Override
    public boolean approveAccount(int id, AccountApproveStatus status) throws RemoteException {
        return AccountDB.approveAccount(id, status);
    }

    @Override
    public boolean isTheAccountOnline(String username) throws RemoteException {
        return OnlineClientArrayList.getInstance().isTheAccountOnline(username);
    }

    private boolean isFactorType(String xValue ){
            boolean isFactortType = false;
            for(FactorCategoryParameter c : FactorCategoryParameter.values()){
                if (c.toString().equals(xValue)){
                    isFactortType = true;
                }
            }
            return isFactortType;
        }

        private PreparedStatement getPrepareStatement(Params params, ReportCategoryMethod method, Connection connection){
        PreparedStatement ps = null;
        String xValue = params.getxValue();

       boolean isFactortType =  isFactorType( xValue );

        String sql = "SELECT id FROM family WHERE barangayid IN\n" +
                "(SELECT id FROM barangay WHERE name = ?  AND date LIKE ?)";


        if (method != null){

                    if (isFactortType){
                        ps = getFatortTypePreparedStatement(connection, params, method);

                    }else{

                        ps = getPovertyPopulationPreparedStatement(connection, params, method);
                    }
        }
        else {

                try {
                    ps = connection.prepareStatement(sql);

                    String barangayName = params.getBarangay1();
                    String date =  params.getDate() + "%";

                    ps.setString(1,barangayName);
                    ps.setString(2, date + "%");
                } catch (SQLException e) {
                    e.printStackTrace();
                }

        }

        return ps;
    }

    private PreparedStatement getFatortTypePreparedStatement(Connection connection, Params params, ReportCategoryMethod method){
        PreparedStatement ps = null;

        String factor = params.getxValue();
        String barangayName = params.getBarangay1();
        String date = params.getDate();

        String colName = getColumnName(factor);
        String value = getParseXvalue(factor);

        String sql = "SELECT F.id FROM family F\n" +
                "  LEFT JOIN povertyfactors P ON P.familyid = F.id\n" +
                "WHERE barangayid IN\n" +
                    "      (SELECT id FROM barangay WHERE name = ?  AND date LIKE ?)" +
                " AND P."+ colName +   " = ? ";

        String sql2 = "SELECT F.id FROM family F\n" +
                "  LEFT JOIN povertyfactors P ON P.familyid = F.id\n" +
                "WHERE barangayid IN\n" +
                "      (SELECT id FROM barangay where date LIKE ?)" +
                " AND P."+ colName +   " = ? ";


        try {
                if (method == ReportCategoryMethod.OVERVIEW || method == ReportCategoryMethod.COMPARE_OVERVIEW ){
                    ps = connection.prepareStatement(sql2);

                    ps.setString(1, date + "%");
                    ps.setString(2, value);

                    System.out.println("Column name: " + colName + " DBValue: " + value);
                }else {
                    ps = connection.prepareStatement(sql);

                    ps.setString(1,barangayName);
                    ps.setString(2, date + "%");
                    ps.setString(3, value);

                    System.out.println("Column name: " + colName + " DBValue: " + value);

                }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return  ps;

    }
    private PreparedStatement getPovertyPopulationPreparedStatement(Connection connection, Params params, ReportCategoryMethod method){
        PreparedStatement ps = null;

        String barangayName = params.getBarangay1();
        String date = params.getDate();


        String sql = "SELECT id FROM family WHERE barangayid IN\n" +
                "(SELECT id FROM barangay WHERE name = ?  AND date LIKE ?)";

        String sql2 = "SELECT id FROM family WHERE barangayid IN\n" +
                "(SELECT id FROM barangay where date LIKE ?)";



        try {
            ps = connection.prepareStatement(sql);

            ps.setString(1,barangayName);
            ps.setString(2, date + "%");


            if (method == ReportCategoryMethod.OVERVIEW || method == ReportCategoryMethod.COMPARE_OVERVIEW){
                ps = connection.prepareStatement(sql2);
                ps.setString(1, date + "%");

            }else {
                ps = connection.prepareStatement(sql);

                ps.setString(1,barangayName);
                ps.setString(2, date + "%");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ps;
    }

    private String getColumnName(String value){
        String colName = "";
        switch (value){
            case "Unemployed": colName = "occupancy";
                break;
            case "No other Income": colName = "otherincome";
                break;
            case "Below City Threshold": colName = "threshold";
                break;

            case "UnderEmployed": colName = "underemployed";
                break;
            case "No Home": colName = "ownership";
                break;

        }

        return  colName;
    }

    private String getParseXvalue(String xValue){
        String val = "";
        switch (xValue){
            case "Unemployed": val = "Unemployed";
                break;
            case "No other Income": val = "No";
                break;
            case "Below City Threshold": val = "Yes";
                break;
            case "UnderEmployed": val = "Yes";
                break;
            case "No Home": val = "Illegal settlers";
                break;
        }
        return val;
    }

}
