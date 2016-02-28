package MainApp.AdminServer;

import AdminModel.Params;
import AdminModel.Report.Children.Model.ResponsePovertyFactor;
import AdminModel.Report.Children.Model.ResponsePovertyRate;
import AdminModel.Report.Parent.ResponseCompareOverview;
import AdminModel.Report.Parent.ResponseSpecific;
import AdminModel.Report.Parent.ResponseSpecificOverView;
import AdminModel.Report.Parent.ResponseOverviewReport;
import AdminModel.ResponseModel.ActiveAccounts;
import BarangayData.BarangayData;
import DecisionSupport.Prioritizer;
import MainApp.DataBase.Database;
import PriorityModels.PriorityLevel;
import PriorityModels.PriorityType;
import RMI.AdminInterface;
import RMI.Constant;
import Remote.Method.FamilyModel.Family;
import Remote.Method.FamilyModel.FamilyInfo;
import Remote.Method.FamilyModel.FamilyPoverty;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

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

    private BarangayDB barangayData = new BarangayDB();
    private PovertyFactorsData povertyFactorsData = new PovertyFactorsData();

    public AdminDB() throws RemoteException {

        connectionLock = new Object();
        lock1 = new Object();
        overviewLock = new Object();
        activeAccount = new Object();

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
    public  ArrayList getFamilyBarangay(Params params) throws RemoteException {
        Connection connection = null;
        ArrayList<Family> list = new ArrayList();

        String sql = "SELECT id FROM family WHERE barangayid IN\n" +
                "(SELECT id FROM barangay WHERE name = ?  AND date LIKE ?)";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, params.getBarangay1());
            ps.setString(2, String.valueOf(params.getDate()) + "%");

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
        ArrayList list = new ArrayList();
        String sql = "\n" +
                "SELECT A.id, user, C.name FROM account A \n" +
                "LEFT JOIN client C ON C.accountid = A.id\n" +
                "WHERE A.requeststatus = 'Approved' and A.role = 'Client'";

            synchronized (activeAccount){

                try {
                    connection = connectionPool.getConnection();
                    PreparedStatement ps = connection.prepareStatement(sql);

                    ResultSet rs = ps.executeQuery();

                            while (rs.next()){
                                int id = rs.getInt("id");
                                String name = rs.getString("Name");
                                String username = rs.getString("user");

                                list.add(new ActiveAccounts(id,username, name));

                            }

                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                    Utility.closeConnection(connection);
                }

            }

        return list;
    }


}
