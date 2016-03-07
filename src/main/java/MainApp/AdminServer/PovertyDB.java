package MainApp.AdminServer;

import ListModels.ChildrenSchoolCategory;
import MainApp.DataBase.Database;
import Remote.Method.FamilyModel.FamilyPoverty;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Logger;
import utility.Utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Created by reiner on 2/27/2016.
 */
public class PovertyDB {

    private static JdbcConnectionPool connectionPool = Database.getConnectionPool();

    public static FamilyPoverty getFamilyPovertyDataByFamilyId(int id) {
        Connection connection = null;
        FamilyPoverty familyPoverty = null;

        String date = null;
        String occupancy = null;
        String childrenInSchool = null;
        String underemployed = null;
        String otherincome = null;
        String threshold = null;
        String ownership = null;

        String sql = "Select * from povertyfactors where familyid = ?";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            Logger.Log("Family id: " + id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                date = rs.getString("year");
                occupancy = rs.getString("occupancy");
                childrenInSchool = rs.getString("schoolchildren");
                underemployed = rs.getString("underemployed");
                otherincome = rs.getString("otherincome");
                threshold = rs.getString("threshold");
                ownership = rs.getString("ownership");
            }

           //LocalDate localDate =  Utility.StringToLocalDate(date);
            LocalDate localDate =  LocalDate.parse(date);

            familyPoverty = new FamilyPoverty();

            ChildrenSchoolCategory childrenCat = null;
            if ( !(childrenInSchool == null || childrenInSchool.equals("")) ){
                childrenCat = ChildrenSchoolCategory.valueOf(childrenInSchool.toUpperCase());
            }

            familyPoverty.setYear(localDate);
            familyPoverty.setOccupancy(occupancy);
            familyPoverty.setChildreninSchool(childrenCat);
            familyPoverty.setIsunderEmployed(underemployed);
            familyPoverty.setHasotherIncome(otherincome);
            familyPoverty.setIsbelow8k(threshold);
            familyPoverty.setOwnership(ownership);

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return  familyPoverty;
    }

    public static void updateFamilyPoverty(FamilyPoverty familyPoverty, int familyID, Connection connection){

        String povertySql = "Update povertyfactors SET " +
                "year = ?," +
                "month = ?," +
                "occupancy = ?," +
                "schoolchildren = ?," +
                "underemployed = ?," +
                "otherincome = ?," +
                "threshold = ?," +
                "ownership = ?" +
                "WHERE familyid = ?";

        try {

            String childrenInSchool = "";
            if (familyPoverty.getChildreninSchool() != null) {
                childrenInSchool = familyPoverty.getChildreninSchool().toString();
            }

            PreparedStatement  ps = connection.prepareStatement(povertySql);

            ps.setString(1, familyPoverty.getYear().toString());
            ps.setInt   (2,    familyPoverty.getMonth());
            ps.setString(3, familyPoverty.getOccupancy());
            ps.setString(4, childrenInSchool);
            ps.setString(5, familyPoverty.getIsunderEmployed());
            ps.setString(6, familyPoverty.getHasotherIncome());
            ps.setString(7, familyPoverty.getIsbelow8k());
            ps.setString(8, familyPoverty.getOwnership());
            ps.setInt   (9,    familyID);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static boolean addPovertyFactors(FamilyPoverty familyPoverty, int familyId, Connection connection){
        boolean isAdded = false;

        String addPovertySql = "Insert Into povertyfactors (Familyid, year, month, occupancy,schoolchildren," +
                "underemployed,otherincome,threshold,ownership) " +
                "Values (?,?,?,?,?,?,?,?,?)";

        //
        try {
            String childrenInSchool = "";
            if (familyPoverty.getChildreninSchool() != null) {
                childrenInSchool = familyPoverty.getChildreninSchool().toString();
            }

            PreparedStatement ps = connection.prepareStatement(addPovertySql);
            ps.setInt(1, familyId);
            ps.setString(2, familyPoverty.getYear().toString());
            ps.setInt(3, familyPoverty.getMonth());
            ps.setString(4, familyPoverty.getOccupancy());
            ps.setString(5, childrenInSchool);
            ps.setString(6, familyPoverty.getIsunderEmployed());
            ps.setString(7, familyPoverty.getHasotherIncome());
            ps.setString(8, familyPoverty.getIsbelow8k());
            ps.setString(9, familyPoverty.getOwnership());

            int row = ps.executeUpdate();
            if (row == 1) {
                isAdded = true;
                System.out.println("sucessfully added Family");
            }

        } catch (SQLException e) {
            isAdded = false;
            e.printStackTrace();
        }
        return isAdded;
    }


}