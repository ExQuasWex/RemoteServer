package MainApp.AdminServer;

import AdminModel.Params;
import AdminModel.Report.Children.Model.ResponseMonthlyPovertyRate;
import AdminModel.Report.Children.Model.ResponsePovertyRate;
import MainApp.DataBase.Database;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Didoy on 2/25/2016.
 */
public class BarangayDB {

    private  JdbcConnectionPool connectionPool  = Database.getConnectionPool();

    // overview sql
    private String povertyPopulationOfeveryBarangay;

    // compare overview sql
    private String totalPovertyYearPopulationSql;

    // compare specific sql
    private String povertyPopulationOfBarangay;


    public BarangayDB() {

        // overview sql
        povertyPopulationOfeveryBarangay = "SELECT name,  sum(unresolvepopulation) as unresolvepopulation\n" +
                "FROM barangay\n" +
                "WHERE date LIKE ? GROUP BY name \n";

        // compare overview sql
        totalPovertyYearPopulationSql = "SELECT  sum(unresolvepopulation) as unresolvepopulation\n" +
                "FROM barangay WHERE date LIKE ? ";

        // compare specific sql
        povertyPopulationOfBarangay = "SELECT  sum(unresolvepopulation) as unresolvepopulation\n" +
                "FROM barangay WHERE date LIKE ? and name = ?";

    }

    //overview
    public ArrayList getOverViewPopulation(String year){
        Connection connection = null;
        ArrayList povertyList = new ArrayList();

        try {
             connection = connectionPool.getConnection();

            PreparedStatement povertyRatePS = connection.prepareStatement(povertyPopulationOfeveryBarangay);
            povertyRatePS.setString(1, year + "%");

            ResultSet povertyRS = povertyRatePS.executeQuery();

            while (povertyRS.next()){
                String brngayName = povertyRS.getString("name");
                int unresolvePopulation = povertyRS.getInt("unresolvepopulation");

                ResponsePovertyRate bd = new ResponsePovertyRate(brngayName, unresolvePopulation);

                povertyList.add(bd);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }


        return povertyList;
    }

    // compare overview
    public int getPovertyCompareOverVieData(int year){
        Connection connection = null;

        int totalPoverty = 0;
        try {
            String yr = String.valueOf(year);

             connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(totalPovertyYearPopulationSql);
            ps.setString(1, yr + "%");

            ResultSet rs = ps.executeQuery();
            rs.next();
            totalPoverty = rs.getInt("unresolvepopulation");

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return  totalPoverty;
    }

    // compare Specific this comparison include month in date field
    public int getPovertyCompareSpecificData(String date, String BarangayName){
        Connection connection = null;

        int totalPoverty = 0;
        try {

             connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(povertyPopulationOfBarangay);
            ps.setString(1, date + "%");
            ps.setString(2, BarangayName);

            ResultSet rs = ps.executeQuery();
            rs.next();
            totalPoverty = rs.getInt("unresolvepopulation");

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return  totalPoverty;
    }

    public ArrayList getPovertySpecificOverviewData(Params params){
        Connection connection = null;
        ArrayList list = new ArrayList();

        String sql = "SELECT  date, sum(unresolvepopulation) as unresolvepopulation\n" +
                "FROM barangay WHERE name = ? and date LIKE ? GROUP BY date order by date";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, params.getBarangay1());
            ps.setString(2, String.valueOf(params.getYear()) + "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()){
                String date = rs.getString("date");
                int population = rs.getInt("unresolvepopulation");

                date = Utility.DateToMonth(date);
                String month = Utility.rebirtDigitalMonth(date);

                ResponseMonthlyPovertyRate monthlyPovertyRate = new ResponseMonthlyPovertyRate(month, population);

                list.add(monthlyPovertyRate);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return list;
    }



}
