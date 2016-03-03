package MainApp.AdminServer;

import MainApp.DataBase.Database;
import Remote.Method.FamilyModel.FamilyInfo;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            int yrResidency = rs.getInt("yrresidency");
            int numOfChildren = rs.getInt("childrenno");
            String name = rs.getString("name");
            String spouseName = rs.getString("spouse");
            String age = rs.getString("age");
            String maritalstatus = rs.getString("maritalstatus");
            String gender = rs.getString("gender");

            String inputDate = rs.getString("date");
            String dateIssued = rs.getString("yrissued");
            LocalDate dateIssue = Utility.StringToLocalDate(dateIssued);

            familyInfo =
                    new FamilyInfo(famid, yrResidency, numOfChildren, name,
                            spouseName, age, maritalstatus, gender);

            familyInfo.setInputDate(inputDate);
            familyInfo.setSurveyedYr(dateIssue);

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

}
