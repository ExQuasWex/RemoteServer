package MainApp.AdminServer;

import AdminModel.Report.Children.Model.ResponseCompareOverview;
import AdminModel.Report.Children.Model.ResponsePovertyFactor;
import AdminModel.Report.Children.Model.ResponsePovertyRate;
import AdminModel.Params;
import AdminModel.Report.Parent.Model.ResponseOverviewReport;
import RMI.AdminInterface;
import RMI.Constant;
import Remote.Method.FamilyModel.FamilyPoverty;
import com.sun.deploy.nativesandbox.comm.Response;
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
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Created by Didoy on 11/26/2015.
 */
public class AdminDB extends UnicastRemoteObject implements AdminInterface {

    private Object lock1;

    private Connection connection;
    private final String host = "jdbc:h2:file:c:/pdsss/database/pdss;Mode=MySQL;LOCK_MODE=1";
    private final String user = "admin";
    private final String pass = "admin";
    private static JdbcConnectionPool connectionPool;

    private Object connectionLock;
    private Object overviewLock;


    private String overViewSQL;
    public AdminDB() throws RemoteException {

        connectionLock = new Object();
        lock1 = new Object();
        overviewLock = new Object();


                connectionPool = JdbcConnectionPool.create(host, user, pass);


        overViewSQL= "SELECT\n" +
                "  sum(CASE\n" +
                "      WHEN occupancy ='Unemployed' THEN 1\n" +
                "      ELSE 0\n" +
                "      END) as unemployed,\n" +
                "\n" +
                "  sum( CASE\n" +
                "       WHEN underemployed = 'Yes' THEN 1\n" +
                "       ELSE 0\n" +
                "       END)as Underemployed,\n" +
                "  sum( CASE\n" +
                "       WHEN otherincome = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as NoExtra,\n" +
                "  sum( CASE\n" +
                "       WHEN threshold = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as BelowMinimum,\n" +
                "  sum( CASE\n" +
                "       WHEN (ownership = 'Rental' or ownership = 'Shared' or ownership = 'Informal settler') THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as NOshELTER\n" +
                "\n" +
                "\n" +
                "from povertyfactors where year like '2016%'";

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
                connection.close();

            } catch (SQLException e) {
                isconnected = false;
                e.printStackTrace();
            }
        }

        return isconnected;
    }

    @Override
    public ResponseOverviewReport getOverViewData(Params params, String type) throws RemoteException {
        ArrayList povertyList = new ArrayList();
        ArrayList factorList = new ArrayList();
        ResponseOverviewReport responseOverviewReport = null ;

        String year = Utility.getCurrentYear();
        String povertyFactorSQL = overViewSQL;
        String povertyRateSQL = "SELECT name,  sum(unresolvepopulation) as unresolvepopulation\n" +
                "FROM barangay\n" +
                "WHERE date LIKE ? GROUP BY name \n";

            synchronized (overviewLock){

                    try {
                        connection = connectionPool.getConnection();
                        PreparedStatement povertyFactorPS = connection.prepareStatement(povertyFactorSQL);
                        PreparedStatement povertyRatePS = connection.prepareStatement(povertyRateSQL);
                        povertyRatePS.setString(1,year + "%");

                        ResultSet factorRS = povertyFactorPS.executeQuery();

                            while (factorRS.next()){

                                int unemployed = factorRS.getInt("Unemployed");
                                int underEmployed = factorRS.getInt("UnderEmployed");
                                int noextra = factorRS.getInt("NoExtra");
                                int BelowMinimum = factorRS.getInt("BelowMinimum");
                                int NoShelter = factorRS.getInt("NoShelter");


                            ResponsePovertyFactor povertyRate  =
                                    new ResponsePovertyFactor(unemployed, underEmployed, noextra, BelowMinimum, NoShelter );

                                factorList.add(povertyRate);
                            }

                        ResultSet povertyRS = povertyRatePS.executeQuery();

                        while (povertyRS.next()){
                                String brngayName = povertyRS.getString("name");
                                int unresolvePopulation = povertyRS.getInt("unresolvepopulation");

                                ResponsePovertyRate bd = new ResponsePovertyRate(brngayName, unresolvePopulation);

                                povertyList.add(bd);

                            }

                        responseOverviewReport = new ResponseOverviewReport(factorList, povertyList);

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }finally {
                        Utility.closeConnection(connection);
                    }
            }

        return responseOverviewReport;
    }

    @Override
    public ResponseCompareOverview getCompareOverViewData(Params params, String type) throws RemoteException {
        ResponseCompareOverview compareOverview = null;
        String sql = "SELECT  sum(unresolvepopulation) as unresolvepopulation\n" +
                "FROM barangay WHERE date LIKE ? ";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, String.valueOf(params.getYear()) + "%");

            ResultSet rs = ps.executeQuery();
            rs.next();
            int povertyRateYearOne = rs.getInt("unresolvepopulation");

            // getting total poverty rate year 2
            ps.setString(1, String.valueOf(params.getMaxYear())+ "%");
            rs = ps.executeQuery();
            rs.next();
            int povertyRateYearTwo = rs.getInt("unresolvepopulation");

          compareOverview =  new ResponseCompareOverview(povertyRateYearOne, povertyRateYearTwo);


        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }


        return compareOverview;
    }

    @Override
    public ArrayList getCompareSpecificData(Params params, String type) throws RemoteException {
        return null;
    }

    @Override
    public ArrayList getSpecificOverViewData(Params params, String type) throws RemoteException {
        return null;
    }

    @Override
    public ArrayList getSpecific(Params params, String type) throws RemoteException {
        return null;
    }

    @Override
    public ArrayList getYears() throws RemoteException {
        ArrayList yearList = new ArrayList();
        String sql = "Select date from barangay ";

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

    @Override
    public ArrayList getBarangayData() throws RemoteException {
        ArrayList<ResponsePovertyRate> responsePovertyRateList = new ArrayList<ResponsePovertyRate>();

                synchronized (lock1){

                    try {

                        connection = connectionPool.getConnection();
                        //  Select name, date,  SUM(unresolvepopulation) from barangay where date = 2014 GROUP BY name,date
                        String sql = "SELECT name,  sum(unresolvepopulation) as unresolvepopulation\n" +
                                "FROM barangay\n" +
                                "WHERE date LIKE ? GROUP BY name \n";;
                        PreparedStatement ps = connection.prepareStatement(sql);
                        ps.setString(1, Utility.getCurrentYear() + "%");
                        ResultSet rs = ps.executeQuery();


                        while (rs.next()){
                            String brngayName = rs.getString("name");
                            int unresolvePopulation = rs.getInt("unresolvepopulation");

                            ResponsePovertyRate bd = new ResponsePovertyRate(brngayName,unresolvePopulation);
                            responsePovertyRateList.add(bd);

                        }

                        connection.close();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }

        return responsePovertyRateList;
    }


    private LocalDate StringToLocalDate(String date){
        LocalDate localDate = LocalDate.parse(date);

        return localDate;
    }



}
