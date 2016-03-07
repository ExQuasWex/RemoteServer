package MainApp.AdminServer;

import MainApp.DataBase.Database;
import Remote.Method.FamilyModel.Family;
import Remote.Method.FamilyModel.FamilyInfo;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.rmi.RemoteException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Created by reiner on 2/27/2016.
 */
public class FamilyDB {

    private static JdbcConnectionPool connectionPool = Database.getConnectionPool();

    public static FamilyInfo getFamilyData(int id) {
        Connection connection = null;
        FamilyInfo familyInfo = null;

        String sql = "SELECT * FROM family WHERE id = ?";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            rs.next();
            int famid = rs.getInt("id");
            int barangayId = rs.getInt("barangayid");
            int yrResidency = rs.getInt("yrresidency");
            int numOfChildren = rs.getInt("childrenno");
            String name = rs.getString("name");
            String address = rs.getString("address");
            String spouseName = rs.getString("spouse");
            String age = rs.getString("age");
            String maritalstatus = rs.getString("maritalstatus");
            String gender = rs.getString("gender");

            String inputDate = rs.getString("date");
            String dateIssued = rs.getString("yrissued");
            LocalDate dateIssue = Utility.StringToLocalDate(dateIssued);
            String barangayName = BarangayDB.getBarangayNameById(barangayId);

            familyInfo =
                    new FamilyInfo(famid, yrResidency, numOfChildren, name,
                            spouseName, age, maritalstatus, gender);

            familyInfo.setInputDate(inputDate);
            familyInfo.setSurveyedYr(dateIssue);
            familyInfo.setAddress(address);
            familyInfo.setBarangay(barangayName);
            familyInfo.setBarangayID(barangayId);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return familyInfo;
    }

    public static  ArrayList<Integer> getFamilyIdList(String year, String barangayName){
        Connection connection = null;
        ArrayList idList = new ArrayList();

        String sql = "  SELECT F.id\n" +
                "  FROM family F LEFT JOIN barangay B ON B.id = F.barangayid\n" +
                "  WHERE B.date LIKE ? and B.name = ? GROUP BY F.id  ";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, year + "%");
            ps.setString(2, barangayName);

            ResultSet rs = ps.executeQuery();
                while (rs.next()){
                    int id = rs.getInt("id");
                    idList.add(id);
                }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

       return idList;
    }

    public static void updateFamily(FamilyInfo familyInfo, int barangayiD, Connection connection){

        String sql = "Update family SET " +
                "barangayid = ?," +
                "date = ?," +
                "name = ?," +
                "maritalstatus = ?," +
                "age = ?," +
                "spouse = ?," +
                "address = ?," +
                "childrenno = ?," +
                "gender = ?," +
                "yrresidency = ?," +
                "yrissued = ?," +
                "clientid = ?" +
                "where  id = ?";

        try {
            int numofChildren = familyInfo.getNumofChildren();

            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setInt(1,    barangayiD);
            ps.setString(2, familyInfo.getInputDate());
            ps.setString(3, familyInfo.getName());
            ps.setString(4, familyInfo.getMaritalStatus());
            ps.setString(5, familyInfo.getAge());
            ps.setString(6, familyInfo.getSpouseName());
            ps.setString(7, familyInfo.getAddress());
            ps.setInt(8,    numofChildren);
            ps.setString(9, familyInfo.getGender());
            ps.setInt(10,   familyInfo.getResidencyYr());
            ps.setString(11,familyInfo.getSurveyedYr().toString());
            ps.setInt(12, familyInfo.getClientID());
            ps.setInt(13,   familyInfo.familyId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void updateClientIpAddress(String ipaddress, int accountID, Connection connection){

        String sql = "Update generatedport set ipaddress = ?where accountid = ?";

        try {

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, ipaddress);
            ps.setInt(2, accountID);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int  addFamily(FamilyInfo familyInfo, int barangayID, Connection connection){
        int familyID = 0;

        String addFamilySql = "Insert INTO family (barangayid,date,name,maritalstatus,age,spouse," +
                "address,childrenno,gender,yrresidency,yrissued,clientid)" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            int numofChildren = familyInfo.getNumofChildren();
            PreparedStatement ps = connection.prepareStatement(addFamilySql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, barangayID);
            ps.setString(2,familyInfo.getInputDate());
            ps.setString(3,familyInfo.getName());
            ps.setString(4,familyInfo.getMaritalStatus());
            ps.setString(5,familyInfo.getAge());
            ps.setString(6,familyInfo.getSpouseName());
            ps.setString(7,familyInfo.getAddress());
            ps.setInt(8,numofChildren);
            ps.setString(9,familyInfo.getGender());
            ps.setInt(10, familyInfo.getResidencyYr());
            ps.setString(11,familyInfo.getSurveyedYr().toString());
            ps.setInt(12,familyInfo.getClientID());

            int row = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();

            rs.next();
            if (row == 1 ){
                familyID = rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return familyID;
    }

    public static int countResolveofBarangay(int barangayID){
        Connection connection = null;
        int population = 0;
        String sql = "select count(DISTINCT id)as id  from family where status = 'Resolve' And barangayid  = ?";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, barangayID);

            ResultSet rs = ps.executeQuery();
            rs.next();
            population = rs.getInt("id");

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }
        return population;
    }

    public static int countAllUnresolveFromBarangay(int barangayID){
        Connection connection = null;
        int population = 0;
        String sql = "select count(DISTINCT id)as id  from family where status = 'Unresolve' And barangayid  = ?";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, barangayID);

            ResultSet rs = ps.executeQuery();
            rs.next();
            population = rs.getInt("id");

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }
        return population;
    }




}
