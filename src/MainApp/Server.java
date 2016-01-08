package MainApp;

import MainApp.AdminServer.AdminDB;
import MainApp.ClientSide.ClientDB;
import RMI.Constant;
import javafx.application.Application;
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

            AdminDB adminserver = new AdminDB();
            Registry reg2 = LocateRegistry.createRegistry(Constant.Adminport);
            reg2.bind(Constant.RMIAdminID, adminserver);

            System.out.println("Server is now Running..");

           //clientDB.Login("villerdexz","123321","");


        }catch (RemoteException e){
            e.printStackTrace();
            System.out.println("Server:Server.java:connectClientDB: RemoteException ");

        } catch (AlreadyBoundException e) {
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
    }
}
