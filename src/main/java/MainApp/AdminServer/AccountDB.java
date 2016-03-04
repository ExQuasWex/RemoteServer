package MainApp.AdminServer;

import MainApp.DataBase.Database;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by reiner on 3/3/2016.
 */
public class AccountDB {

    private static JdbcConnectionPool connectionPool = Database.getConnectionPool();

    public static String getAdminNameById(int AdminID){
        Connection connection = null;
        String adminName = null;
        String sql = "Select name from client where id = ?";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, AdminID);

            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                adminName = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return adminName;
    }

}
