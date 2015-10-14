package MainApp;

import RMI.Constant;
import org.h2.jdbc.JdbcSQLException;
import view.AdminFrame;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by Didoy on 8/24/2015.
 */
public class Server extends Application {


    public  static  void main (String []argh){

        Thread clientThread = new Thread(() -> {

            connectClientDB();

        });
        clientThread.start();

        Application.launch(argh);

    }


    public static void connectClientDB(){
        try {

            ClientDB clientDB = new ClientDB();
            Registry reg = LocateRegistry.createRegistry(Constant.Remote_port);
            reg.bind(Constant.Remote_ID,clientDB);

            System.out.println("Server is now Running..");

           //System.out.println(clientDB.Login("villerdex","123321"));

        }catch (RemoteException e){
            e.printStackTrace();
            System.out.println("Server:Server.java:connectClientDB: RemoteException ");

        } catch (AlreadyBoundException e) {
            System.out.println("Server:Server.java:connectClientDB: AlreadyBoundException ");
        }

    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        AdminFrame adminFrame = new AdminFrame();
        adminFrame.show();

        adminFrame.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(1);
            }
        });
    }
}
