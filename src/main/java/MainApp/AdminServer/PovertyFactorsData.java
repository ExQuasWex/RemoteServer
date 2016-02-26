package MainApp.AdminServer;

import AdminModel.Report.Children.Model.ResponsePovertyFactor;
import MainApp.DataBase.Database;
import org.h2.jdbcx.JdbcConnectionPool;
import utility.Logger;
import utility.Utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Didoy on 2/25/2016.
 */
public class PovertyFactorsData {

    private JdbcConnectionPool connectionPool  = Database.getConnectionPool();

    // overview sql and compare overview
    private String povertyFactorsOfTheCity;

    // compare specific sql
    private String povertyFactorsOfBarangay;


    // specific overview sql
    private String povertyFactorsSpecificOverview;

    public PovertyFactorsData() {

        // overview sql and compare overview
        povertyFactorsOfTheCity = "SELECT\n" +
                "  sum(CASE\n" +
                "      WHEN occupancy ='Unemployed' THEN 1\n" +
                "      ELSE 0\n" +
                "      END) as unemployed,\n" +
                "\n" +
                "  sum( CASE\n" +
                "       WHEN underemployed = 'Yes' THEN 1\n" +
                "       ELSE 0\n" +
                "       END)as Underemployed,\n" +
                "  sum( CASE\n" +
                "       WHEN otherincome = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as NoExtra,\n" +
                "  sum( CASE\n" +
                "       WHEN threshold = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as BelowMinimum,\n" +
                "  sum( CASE\n" +
                "       WHEN (ownership = 'Rental' or ownership = 'Shared' or ownership = 'Informal settler') THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as NOshELTER\n" +
                "\n" +
                "\n" +
                "from povertyfactors where year like ?";



    // compare specific sql
        povertyFactorsOfBarangay = "SELECT\n" +
                "  sum(CASE\n" +
                "      WHEN occupancy ='Unemployed' THEN 1\n" +
                "      ELSE 0\n" +
                "      END) as unemployed,\n" +
                "  sum( CASE\n" +
                "       WHEN underemployed = 'Yes'THEN 1\n" +
                "       ELSE 0\n" +
                "       END)as Underemployed,\n" +
                "  sum( CASE\n" +
                "       WHEN otherincome = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as NoExtra,\n" +
                "  sum( CASE\n" +
                "       WHEN threshold = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as BelowMinimum,\n" +
                "  sum( CASE\n" +
                "       WHEN (ownership = 'Rental' or ownership = 'Shared' or ownership = 'Informal settler') THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as NOshELTER\n" +
                "\n" +
                "from povertyfactors P\n" +
                "\n" +
                "  Left JOin family F ON F.id = P.familyid\n" +
                "\n" +
                "where year like ? and F.barangayid in (SELECT id from barangay where date like ? and name = ? )";


        // specific overview
        povertyFactorsSpecificOverview = "SELECT  B.date,\n" +
                "  sum(CASE\n" +
                "      WHEN occupancy ='Unemployed' THEN 1\n" +
                "      ELSE 0\n" +
                "      END) as unemployed,\n" +
                "  sum( CASE\n" +
                "       WHEN underemployed = 'Yes' THEN 1\n" +
                "       ELSE 0\n" +
                "       END)as Underemployed,\n" +
                "  sum( CASE\n" +
                "       WHEN otherincome = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END ) as NoExtra,\n" +
                "  sum( CASE\n" +
                "       WHEN threshold = 'No' THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as BelowMinimum,\n" +
                "  sum( CASE\n" +
                "       WHEN (ownership = 'Rental' or ownership = 'Shared' or ownership = 'Informal settler') THEN +1\n" +
                "       ELSE 0\n" +
                "       END)as NOshELTER\n" +
                "from povertyfactors P\n" +
                "  LEFT JOIN family F ON F.id = P.familyid\n" +
                "  LEFT JOIN Barangay B ON F.barangayid = B.id\n" +
                "where f.barangayid in (Select id from barangay where name = ? and date like ?)\n" +
                " GROUP BY B.id";

    }

    public ArrayList getOverViewFactors(String year){
        Connection connection = null;
        ArrayList factorList = new ArrayList();

        try {
             connection = connectionPool.getConnection();

            PreparedStatement PS = connection.prepareStatement(povertyFactorsOfTheCity);
            PS.setString(1, Utility.getCurrentYear() + "%");

            ResultSet rs = PS.executeQuery();

            while (rs.next()){
                int unemployed = rs.getInt("Unemployed");
                int underEmployed = rs.getInt("UnderEmployed");
                int noextra = rs.getInt("NoExtra");
                int BelowMinimum = rs.getInt("BelowMinimum");
                int NoShelter = rs.getInt("NoShelter");

                ResponsePovertyFactor povertyRate  =
                        new ResponsePovertyFactor(unemployed, underEmployed, noextra, BelowMinimum, NoShelter );

                factorList.add(povertyRate);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return factorList;
    }

    // compare specfic
    public ArrayList getCompareSpecificFactors(String year, String BarangayName){
        Connection connection = null;

        ArrayList factorList = new ArrayList();

        try {
             connection = connectionPool.getConnection();

            PreparedStatement PS = connection.prepareStatement(povertyFactorsOfBarangay);
            PS.setString(1, year + "%");
            PS.setString(2, year + "%");
            PS.setString(3, BarangayName);

            ResultSet rs = PS.executeQuery();

            while (rs.next()){

                int unemployed = rs.getInt("Unemployed");
                int underEmployed = rs.getInt("UnderEmployed");
                int noextra = rs.getInt("NoExtra");
                int BelowMinimum = rs.getInt("BelowMinimum");
                int NoShelter = rs.getInt("NoShelter");

                ResponsePovertyFactor povertyRate  =
                        new ResponsePovertyFactor(unemployed, underEmployed, noextra, BelowMinimum, NoShelter );

                factorList.add(povertyRate);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return factorList;
    }

    public ArrayList getSpecificOverViewData(String year, String BarangayName){
        Connection connection = null;

        ArrayList factorList = new ArrayList();

        try {
            connection = connectionPool.getConnection();

            PreparedStatement PS = connection.prepareStatement(povertyFactorsSpecificOverview);
            PS.setString(1, BarangayName );
            PS.setString(2, year + "%");

            ResultSet rs = PS.executeQuery();

            while (rs.next()){
                String date = rs.getString("date");
                int unemployed = rs.getInt("Unemployed");
                int underEmployed = rs.getInt("UnderEmployed");
                int noextra = rs.getInt("NoExtra");
                int BelowMinimum = rs.getInt("BelowMinimum");
                int NoShelter = rs.getInt("NoShelter");

                date = Utility.convertIntMonth(date);

                Logger.Log(date);

                ResponsePovertyFactor povertyRate  =
                        new ResponsePovertyFactor(unemployed, underEmployed, noextra, BelowMinimum, NoShelter, date );

                factorList.add(povertyRate);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            Utility.closeConnection(connection);
        }

        return factorList;
    }



}
