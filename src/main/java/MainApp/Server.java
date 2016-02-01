package MainApp;

import MainApp.AdminServer.AdminDB;
import MainApp.ClientSide.ClientDB;
import RMI.Constant;
import com.sun.imageio.plugins.common.ImageUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import utility.Utility;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by Didoy on 8/24/2015.
 */
public class Server extends Application {


    public  static  void main (String []argh){

        Application.launch(argh);

    }

    public static void StartServer(){
        try {


            ClientDB clientDB = new ClientDB();
            clientDB.StartClientServer();
            clientDB.getActiveConnection();

            // creating server for admin
            AdminDB adminserver = new AdminDB();
            adminserver.StartAdminServer();

            System.out.println("Server is now Running..");

           //clientDB.Login("villerdexz","123321","");


        }catch (RemoteException e){
            e.printStackTrace();
            RemoteMessageException();
            System.out.println("Server:Server.java:connectClientDB: RemoteException ");

        }

    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        StartServer();
        addServerToTrayIcon();
    }


    private void addServerToTrayIcon(){
        String trayNotSupported = "No system tray support, application exiting.";

        if (!java.awt.SystemTray.isSupported()) {
            Utility.showMessageBox(trayNotSupported, Alert.AlertType.INFORMATION);
            Platform.exit();
        }

        Image img = null;
        SystemTray tray = SystemTray.getSystemTray();

        img = Toolkit.getDefaultToolkit().getImage("src/main/java/images/server.png");

        Dimension traySize = tray.getTrayIconSize();
        img = img.getScaledInstance(traySize.width, traySize.height, Image.SCALE_SMOOTH);

        TrayIcon trayIcon = new TrayIcon(img);


        // add some pop up menu
        final java.awt.PopupMenu popup = new java.awt.PopupMenu();
        MenuItem exitItem = new MenuItem("Exit Server");
        popup.add(exitItem);

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
                System.exit(0);
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
