package MainApp.AdminServer;

import AdminModel.BarangayData;
import AdminModel.OverViewReportObject;
import AdminModel.Params;
import RMI.AdminInterface;
import RMI.Constant;
import Remote.Method.FamilyModel.FamilyPoverty;
import org.h2.jdbcx.JdbcConnectionPool;

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

    public AdminDB() throws RemoteException {

        connectionLock = new Object();
        lock1 = new Object();
        overviewLock = new Object();


                connectionPool = JdbcConnectionPool.create(host, user, pass);


    }

    public void StartAdminServer(){

        Registry reg = null;
        try {
            Registry reg2 = LocateRegistry.createRegistry(Constant.Adminport);
            reg2.bind(Constant.RMIAdminID, this);

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
    public OverViewReportObject getOverViewData(Params params, String type) throws RemoteException {
        ArrayList povertyList = new ArrayList();
        ArrayList factorList = new ArrayList();
        OverViewReportObject overViewReportObject = null ;

        String povertyFactorSQL = "Select * from povertyfactors where year like '2016%'";
        String povertyRateSQL = "Select name, unresolvepopulation  from barangay  where date like '2016%'";
            synchronized (overviewLock){

                    try {
                        connection = connectionPool.getConnection();
                        PreparedStatement povertyFactorPS = connection.prepareStatement(povertyFactorSQL);
                        PreparedStatement povertyRatePS = connection.prepareStatement(povertyRateSQL);

                        ResultSet factorRS = povertyFactorPS.executeQuery();
                        ResultSet povertyRS = povertyRatePS.executeQuery();

                            while (factorRS.next()){

                                String year = factorRS.getString("year");
                                int month = factorRS.getInt("month");
                                String occu = factorRS.getString("occupancy");
                                String schoolChildren = factorRS.getString("schoolchildren");
                                String underEmployed = factorRS.getString("underemployed");
                                String otherincome = factorRS.getString("otherincome");
                                String threshold = factorRS.getString("threshold");
                                String ownership = factorRS.getString("ownership");

                                LocalDate date = StringToLocalDate(year);
                                FamilyPoverty poverty = new FamilyPoverty(otherincome, threshold,
                                        ownership, occu, underEmployed, schoolChildren, date, month );

                                factorList.add(poverty);

                            }
                            while (povertyRS.next()){
                                String brngayName = povertyRS.getString("name");
                                int unresolvePopulation = povertyRS.getInt("unresolvepopulation");

                                BarangayData bd = new BarangayData(brngayName, unresolvePopulation);

                                povertyList.add(bd);

                            }

                        overViewReportObject = new OverViewReportObject(factorList, povertyList);

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
            }

        return overViewReportObject;
    }

    @Override
    public ArrayList getCompareOverViewData(Params params, String type) throws RemoteException {
        return null;
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
    public ArrayList getBarangayData() throws RemoteException {
        ArrayList<BarangayData>  barangayDataList= new ArrayList<BarangayData>();
        int size = 0;
        int ctr = 0;

                synchronized (lock1){

                    try {

                        connection = connectionPool.getConnection();
                        //  Select name, date,  SUM(unresolvepopulation) from barangay where date = 2014 GROUP BY name,date
                        String sql = "Select name, date,  SUM(unresolvepopulation) as total from barangay where date Like  '2015%'GROUP BY name,date";
                        PreparedStatement ps = connection.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery();


                        while (rs.next()){
                            String barangayName = rs.getString("name");
                            String  date = rs.getString("date");
                            int UnresPopu = rs.getInt("total");

                            BarangayData bd = new BarangayData(barangayName,UnresPopu);
                            barangayDataList.add(bd);

                        }

                        connection.close();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }

        return barangayDataList;
    }


    private LocalDate StringToLocalDate(String date){
        LocalDate localDate = LocalDate.parse(date);

        return localDate;
    }



}
