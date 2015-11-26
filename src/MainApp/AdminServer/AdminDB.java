package MainApp.AdminServer;

import AdminModel.BarangayData;
import MainApp.ClientSide.ClientDB;
import RMI.AdminInterface;
import RMI.Constant;
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
import java.util.ArrayList;

/**
 * Created by Didoy on 11/26/2015.
 */
public class AdminDB extends UnicastRemoteObject implements AdminInterface {

    private ArrayList barangayDataList;
    private Object lock1;

    private Connection connection;
    private final String host = "jdbc:h2:file:c:/pdsss/database/pdss;Mode=MySQL;LOCK_MODE=1";
    private final String user = "admin";
    private final String pass = "admin";
    private static JdbcConnectionPool connectionPool;


    public AdminDB() throws RemoteException {

                barangayDataList = new ArrayList();
                lock1 = new Object();
                connectionPool = JdbcConnectionPool.create(host, user, pass);


    }

    @Override
    public ArrayList getBarangayData() throws RemoteException {
        int size = 0;
        int ctr = 0;

                synchronized (lock1){

                    try {
                        connection = connectionPool.getConnection();
                        //  Select name, date,  SUM(unresolvepopulation) from barangay where date = 2014 GROUP BY name,date
                        String sql = "  Select name, date,  SUM(unresolvepopulation) as total from barangay GROUP BY name,date";
                        PreparedStatement ps = connection.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery();


                        while (rs.next()){
                            String barangayName = rs.getString("name");
                            int date = rs.getInt("date");
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



}
