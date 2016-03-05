package MainApp.AdminServer;

import Remote.Method.FamilyModel.FamilyHistory;
import MainApp.DataBase.Database;
import Remote.Method.FamilyModel.Family;
import Remote.Method.FamilyModel.FamilyInfo;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.sql.*;

/**
 * Created by reiner on 3/3/2016.
 */
public class HistoryDB {

    private  static JdbcConnectionPool connectionPool  = Database.getConnectionPool();


    public static FamilyHistory getFamilyHistoryById(int familyID){
        Connection connection = null;
        FamilyHistory familyHistory = null;
        String sql = "Select * from history where familyid = ?";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, familyID);

            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                int id = rs.getInt("id");
                Date date = rs.getDate("date");
                int adminId = rs.getInt("adminid");
                String action = rs.getString("action_taken");
                boolean isRevoke = rs.getBoolean("revoke");
                String revokeDesc = rs.getString("revoke_description");

               String adminName =  AccountDB.getAdminNameById(adminId);

                String strDate = date.toString();

                familyHistory = new FamilyHistory(id, familyID,strDate,adminName,
                        action, isRevoke, revokeDesc );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }
        return familyHistory;
    }


    public static boolean addHistoryToFamily(Family family){
        Connection connection = null;
        boolean isAdded = false;

        String sql = "Insert Into history (familyID, date, admin_name, action_taken, revoke, revoke_description)" +
                "Values (?,?,?,?,?,?)";
        try {
            FamilyInfo familyInfo = family.getFamilyinfo();
            FamilyHistory familyHistory = family.getFamilyHistory();
            connection = connectionPool.getConnection();

            String date = familyHistory.getDate();
            Date sqlDate = Date.valueOf(date);

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, familyInfo.familyId());
            ps.setDate(2, sqlDate);
            ps.setString(3, familyHistory.getAdminName());
            ps.setString(4, familyHistory.getAction());
            ps.setBoolean(5, familyHistory.isRevoke());
            ps.setString(6, familyHistory.getRevokeDescription());

           int  i =  ps.executeUpdate();
            if (i == 1){
                isAdded = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }
        return isAdded;
    }
}
