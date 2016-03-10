package MainApp;

import MainApp.AdminServer.AdminDB;

import MainApp.ClientSide.ClientDB;
import MainApp.DataBase.Database;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utility.Utility;

import java.awt.*;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

/**
 * Created by Didoy on 8/24/2015.
 */
public class Server extends Application {


    public  static  void main (String []argh){

        Application.launch(argh);

    }

    @Override
    public void start(Stage primaryStage) throws Exception {

     if ( SynchDatabase()) {
         StartServer();
         addServerToTrayIcon();
     }else {
         Utility.showConfirmationMessage("Cant Start Sever", Alert.AlertType.ERROR);
     }
    }


    private  static boolean SynchDatabase(){
        return Database.SynchDB();
    }
    public static void StartServer(){
        try {

            ClientDB clientDB = new ClientDB();
            clientDB.StartClientServer();

            // creating server for admin
            AdminDB adminserver = new AdminDB();
            adminserver.StartAdminServer();

            Database.monitorActiveConnection();

            System.out.println("Server is now Running..");

        }catch (RemoteException e){
            e.printStackTrace();
            RemoteMessageException();
            System.out.println("Server:Server.java:connectClientDB: RemoteException ");

        }

    }

    private void addServerToTrayIcon(){
        String trayNotSupported = "No system tray support, application exiting.";

        if (!java.awt.SystemTray.isSupported()) {
            Utility.showMessageBox(trayNotSupported, Alert.AlertType.INFORMATION);
            Platform.exit();
        }

        Image img = null;
        SystemTray tray = SystemTray.getSystemTray();

        img = Toolkit.getDefaultToolkit().getImage("C:\\Users\\reiner\\IdeaProjects\\RemoteServer\\src\\main\\java\\images\\server.png");

        Dimension traySize = tray.getTrayIconSize();
        img = img.getScaledInstance(traySize.width, traySize.height, Image.SCALE_SMOOTH);

        TrayIcon trayIcon = new TrayIcon(img);

        // add some pop up menu
        final java.awt.PopupMenu popup = new java.awt.PopupMenu();
        MenuItem exitItem = new MenuItem("Exit Server");
        MenuItem showIp = new MenuItem("Show IP address");
        popup.add(exitItem);
        popup.add(showIp);

        MenuItem ip = null;
        InetAddress rawIp  = null;

        try {
            rawIp = InetAddress.getLocalHost();
            String strIp = rawIp.toString();
            showIp.setName(strIp);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        trayIcon.setPopupMenu(popup);

        // if the trayIcon doubled-click show some dialog/stage
        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });


        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                SynchDatabase();
                System.exit(0);
            }
        });

        showIp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        // add trayAIcon to tray
        try {
            tray.add(trayIcon);
            trayIcon.displayMessage("Server","Poverty Decision Support System  Server is now Running", TrayIcon.MessageType.INFO);
        } catch (AWTException e) {
            e.printStackTrace();
        }


    }

    private static void RemoteMessageException(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText("Im already running on the background");

        alert.showAndWait();
    }
}
