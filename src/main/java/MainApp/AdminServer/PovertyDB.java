package MainApp.AdminServer;

import ListModels.ChildrenSchoolCategory;
import MainApp.DataBase.Database;
import Remote.Method.FamilyModel.FamilyPoverty;
import org.h2.jdbcx.JdbcConnectionPool;
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

        String sql = "Select * from povertyfactors where familyid = ?";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            rs.next();

            String date = rs.getString("year");
            String occupancy = rs.getString("occupancy");
            String childrenInSchool = rs.getString("schoolchildren");
            String underemployed = rs.getString("underemployed");
            String otherincome = rs.getString("otherincome");
            String threshold = rs.getString("threshold");
            String ownership = rs.getString("ownership");

            LocalDate localDate =  Utility.StringToLocalDate(date);

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
}