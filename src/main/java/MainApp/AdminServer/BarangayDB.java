package MainApp.AdminServer;

import AdminModel.Params;
import AdminModel.Report.Children.Model.ResponseMonthlyPovertyRate;
import AdminModel.Report.Children.Model.ResponsePovertyRate;
import MainApp.DataBase.Database;
import Remote.Method.FamilyModel.Family;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Logger;
import utility.Utility;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Created by Didoy on 2/25/2016.
 */
public class BarangayDB {

    private  static  JdbcConnectionPool connectionPool  = Database.getConnectionPool();

    // overview sql
    private String povertyPopulationOfeveryBarangay;

    // compare overview sql
    private String totalPovertyYearPopulationSql;

    // compare specific sql
    private String povertyPopulationOfBarangay;


    public BarangayDB() {

        // overview sql
        povertyPopulationOfeveryBarangay = "SELECT count(Distinct  F.id) as population, B.name\n" +
                "                                            FROM Family F\n" +
                "                                              LEFT JOIN barangay B on B.id = F.barangayid\n" +
                "                                            where B.date like ? GROUP BY B.name";

        // compare overview sql
        totalPovertyYearPopulationSql = "SELECT  sum(population) as population\n" +
                "FROM barangay WHERE date LIKE ? ";

        // compare specific sql
        povertyPopulationOfBarangay = "SELECT count(Distinct  F.id) as population, B.name\n" +
                "                                            FROM Family F\n" +
                "                                              LEFT JOIN barangay B on B.id = F.barangayid\n" +
                "                                            where B.date like ? and B.name = ? GROUP BY B.name";

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
                int population = povertyRS.getInt("population");

                ResponsePovertyRate bd = new ResponsePovertyRate(brngayName, population);

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
            totalPoverty = rs.getInt("population");

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
            totalPoverty = rs.getInt("population");

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

        String sql = "SELECT  date, sum(population) as population\n" +
                "FROM barangay WHERE name = ? and date LIKE ? GROUP BY date order by date";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, params.getBarangay1());
            ps.setString(2, String.valueOf(params.getYear()) + "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()){
                String date = rs.getString("date");
                int population = rs.getInt("population");

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

    public static  String getBarangayNameById(int id){
        Connection connection = null;
        String sql = "Select name from barangay where id = ?";
        String barangayName = "";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

                rs.next();
                barangayName = rs.getString("name");

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }
     return  barangayName;
    }


    public static int createNewBarangay(String barangayName, LocalDate date){
        int barangayID = 0;
        Connection connection = null;

        String insertbarangay = "Insert INTO barangay (name,date,month,population) VALUES " +
                "(?,?,?,?)";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement barangayPS = connection.prepareStatement(insertbarangay, Statement.RETURN_GENERATED_KEYS);
            barangayPS.setString(1, barangayName );
            barangayPS.setString(2, date.toString());
            barangayPS.setInt(3, Utility.convertStringMonth(date.getMonth().toString()));
            barangayPS.setInt(4,1);

            int row = barangayPS.executeUpdate();
            ResultSet barangayRs = barangayPS.getGeneratedKeys();

            if (row == 1 && barangayRs.next()) {
                barangayID = barangayRs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return barangayID;
    }

    public static boolean isBarangayExisting(String barangayName, String date, Connection connection){
        boolean exist = false;

        String chckBarangay = "Select id from barangay where name = ? and date like  ?";
        date = Utility.subStringDate(date);

        try {

            PreparedStatement chckPs = connection.prepareStatement(chckBarangay,Statement.RETURN_GENERATED_KEYS);
            chckPs.setString(1, barangayName);
            chckPs.setString(2, date + "%");

            ResultSet rs = chckPs.executeQuery();

            if (rs.next()){
                exist = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return exist;
    }

    public static int getBarangayID(String barangayName, String date){
        Connection connection = null;
        int barangayID = 0;
        String sql = "Select id from barangay where name = ? and date like ?";

        try {
            date = Utility.subStringDate(date);

            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, barangayName);
            ps.setString(2, date + "%");
            ResultSet rs = ps.executeQuery();
                    rs.next();
                barangayID = rs.getInt("id");

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }
        return barangayID;
    }


    public static ArrayList getBarangayIDList(String barangayName, String date){
        Connection connection = null;
        int barangayID = 0;
        ArrayList idList = new ArrayList();
        String sql = "Select id from barangay where name = ? and date like ?";

            try {
                date = Utility.subStringDate(date);

                connection = connectionPool.getConnection();

                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, barangayName);
                ps.setString(2, date + "%");
                ResultSet rs = ps.executeQuery();

                while (rs.next()){
                    barangayID = rs.getInt("id");
                    idList.add(barangayID);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                Utility.closeConnection(connection);
            }
        return idList;
    }

    public static int getBarangayID(int familyID){
        Connection connection = null;
        int barangayID = 0;
        String sql = "Select barangayid from family where id = ?";

            try {
                connection = connectionPool.getConnection();

                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, familyID);

                ResultSet rs = ps.executeQuery();

                rs.next();

                barangayID = rs.getInt("barangayid");

            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                Utility.closeConnection(connection);
            }

        return barangayID;
    }

    public static  void updateFamilyBarangay(int oldBarangayID, int newBarangayID, LocalDate date, Connection connection){

        String sql = "Update barangay  set population   = population -1 where id = ?";
        String sql2 = "Update barangay set population   = population +1, date = ?, month = ? where id = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, oldBarangayID);
            ps.executeUpdate();

            ps.clearParameters();

            ps = connection.prepareStatement(sql2);
            ps.setString(1, date.toString());
            ps.setInt(2, Utility.convertStringMonth(date.getMonth().toString()));
            ps.setInt(3, newBarangayID);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static boolean addToBarangay(Family family, Connection connection){
        boolean isSave = false;
        int familyID = 0;

        String updateBarangay = "Update barangay  SET population = population + 1 where id = ?";

        int barangayID = 0;

        try {
            Logger.Log("addToBarangay");

            String barangayName = family.getFamilyinfo().getBarangay();
            LocalDate date =  family.getFamilyinfo().getSurveyedYr();

            // check if barangay is already existing
            boolean existing = isBarangayExisting(barangayName, date.toString(), connection);

            if (existing){

               ArrayList idList = getBarangayIDList(barangayName, date.toString());
                for (Object id : idList){
                    barangayID = (int) id;
                    //update the barangay
                    PreparedStatement updatePs = connection.prepareStatement(updateBarangay,Statement.RETURN_GENERATED_KEYS);
                    updatePs.setInt(1, (Integer) id);
                    updatePs.executeUpdate();

                }
            }else {
                // insert new  barangay record
                Logger.Log("createNewBarangay ");
                barangayID = createNewBarangay(barangayName, date);
            }

            familyID  = FamilyDB.addFamily(family.getFamilyinfo(), barangayID, connection);

            isSave  = PovertyDB.addPovertyFactors(family.getFamilypoverty(),familyID,connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isSave;
    }


}
