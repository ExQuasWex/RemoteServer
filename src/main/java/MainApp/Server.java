package MainApp;

import MainApp.AdminServer.AdminDB;
import MainApp.ClientSide.ClientDB;
import RMI.Constant;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by Didoy on 8/24/2015.
 */
public class Server extends Application {


    public  static  void main (String []argh){

            connectClientDB();

            Application.launch(argh);

    }


    public static void connectClientDB(){
        try {

            ClientDB clientDB = new ClientDB();
            clientDB.getActiveConnection();

            Registry reg = LocateRegistry.createRegistry(Constant.Remote_port);
            reg.bind(Constant.Remote_ID,clientDB);

            // creating server for admin
            AdminDB adminserver = new AdminDB();
            Registry reg2 = LocateRegistry.createRegistry(Constant.Adminport);
            reg2.bind(Constant.RMIAdminID, adminserver);

            System.out.println("Server is now Running..");

           //clientDB.Login("villerdexz","123321","");


        }catch (RemoteException e){
            e.printStackTrace();
            RemoteMessageException();
            System.out.println("Server:Server.java:connectClientDB: RemoteException ");

        } catch (AlreadyBoundException e) {
            RemoteMessageException();
            System.out.println("Server:Server.java:connectClientDB: AlreadyBoundException ");
        }

    }


    @Override
    public void start(Stage primaryStage) throws Exception {
//        AdminFrame adminFrame = new AdminFrame();
//        adminFrame.show();
//
//        adminFrame.setOnCloseRequest(new EventHandler<WindowEvent>() {
//            @Override
//            public void handle(WindowEvent event) {
//                System.exit(1);
//            }
//        });

        showStartUpMessage();
    }

    private void showStartUpMessage(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText("Server is now Running, after you press OK im still running on the background" +
                " Terminate me manually on windows task Manager Under process command 'Java.exe'");

        alert.showAndWait();
    }


    private static void RemoteMessageException(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText("Im already running on the background");

        alert.showAndWait();
    }
}
