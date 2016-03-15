package MainApp.AdminServer;

import AdminModel.Enum.AccountApproveStatus;
import AdminModel.Enum.AccountStatus;
import AdminModel.RequestAccounts;
import AdminModel.ResponseModel.ActiveAccounts;
import MainApp.DataBase.Database;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Utility;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by reiner on 3/3/2016.
 */
public class AccountDB {

    private static JdbcConnectionPool connectionPool = Database.getConnectionPool();

    public static String getAdminNameById(int AdminID) {
        Connection connection = null;
        String adminName = null;
        String sql = "SELECT name FROM client WHERE id = ?";

        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, AdminID);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                adminName = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Utility.closeConnection(connection);
        }

        return adminName;
    }

    public static boolean updateAccountStatusByID(int id, AccountStatus status) {
        boolean isUpdated = false;
        Connection connection = null;
        String sql = "UPDATE account SET requestStatus  = ? WHERE id = ?";

        try {

            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, status.toString());
            ps.setInt(2, id);

            int i = ps.executeUpdate();
            if (i == 1) {
                isUpdated = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Utility.closeConnection(connection);
        }
        return isUpdated;
    }


    public static boolean approveAccount(int id, AccountApproveStatus status) {
        boolean isUpdated = false;
        String role = "";
        Connection connection = null;
        String sql = "UPDATE account SET requestStatus = ?, Role  = ? WHERE id = ?";

        try {
            if (status == AccountApproveStatus.ADMIN) {
                role = "Admin";
            } else if (status == AccountApproveStatus.APPROVED) {
                role = "Client";
            } else if (status == AccountApproveStatus.REJECT) {
                role = "Client";
            }
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, status.toString());
            ps.setString(2, role);
            ps.setInt(3, id);

            int i = ps.executeUpdate();

            if (i >= 1 && TransferInformation(id)) {
                isUpdated = true;

            } else {
                isUpdated =false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Utility.closeConnection(connection);
        }
        return isUpdated;
    }

    public static boolean deleteStaffByAccountId(int id, Connection connection) {
        boolean isUpdated = false;
        String sql = "Delete FROM client WHERE accountid = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            int i = ps.executeUpdate();
            if (i == 1) {
                isUpdated = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Utility.closeConnection(connection);
        }
        return isUpdated;
    }
    public static boolean deleteAccount(int id) {
        boolean isUpdated = false;
        Connection connection = null;
        String sql = "Delete FROM account WHERE id = ?";

        try {
            connection = connectionPool.getConnection();

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            int i = ps.executeUpdate();
            if (i == 1) {
                isUpdated = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Utility.closeConnection(connection);
        }
        return isUpdated;
    }

    // transger personal informations after approveing account
    private static boolean  TransferInformation(int accountID) {
        Connection connection = null;

        String getClientInfo = "SELECT * FROM request WHERE accountid = ?";
        String sqlTransfer = "INSERT INTO client (Name, Address, contact, Gender, AccountID, SecretInfoID) VALUES (?,?,?,?,?,?)";
        try {
            connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(getClientInfo);
            ps.setInt(1, accountID);

            ResultSet rs = ps.executeQuery();

            rs.next();
            String name = rs.getString("name");
            String address = rs.getString("address");
            String contact = rs.getString("contact");
            String gender = rs.getString("gender");
            int secretinfoID = rs.getInt("secretinfoid");

            PreparedStatement transferPS = connection.prepareStatement(sqlTransfer);
            transferPS.setString(1, name);
            transferPS.setString(2, address);
            transferPS.setString(3, contact);
            transferPS.setString(4, gender);
            transferPS.setInt(5, accountID);
            transferPS.setInt(6, secretinfoID);

            transferPS.executeUpdate();

            connection.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    public static  String getUsername(int accountID)  {
        String username = "";

        String usernameSQL = "SELECT User FROM account WHERE id = ?";

        try {
            Connection  connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(usernameSQL);
            ps.setInt(1,accountID);

            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                username = rs.getString("user");
            }
            ps.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return username;
    }

    public static int getPendingAccounts() throws RemoteException {
        Connection connection = null;

        int numberOfPending = 0;
        String sql = "Select count(id) from request rq where accountid in  " +
                "(select id from account where RequestStatus = 'Pending')";

            try {
                connection = connectionPool.getConnection();

                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                rs.next();
                numberOfPending = rs.getInt(1);

                connection.close();

            }catch (SQLException e) {
                e.printStackTrace();
            }

        return numberOfPending;
    }

    public static ArrayList getRequestAccounts() throws RemoteException {

        Connection connection = null;

        ArrayList<RequestAccounts> requestList = new ArrayList();
        String sql = "SELECT  name, accountid  FROM request RE\n" +
                "LEFT JOIN account A ON RE.accountid = A.id\n" +
                "WHERE A.requestStatus = 'Pending'\n";

            try {
                connection = connectionPool.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

                while (rs.next()){
                    RequestAccounts ra = new RequestAccounts(rs.getString("Name"), rs.getInt("accountid"));
                    requestList.add(ra);
                }

                connection.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }

        return requestList;
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







}




