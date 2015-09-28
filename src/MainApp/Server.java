package MainApp;

import RMI.Constant;
import clientModel.StaffRegister;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by Didoy on 8/24/2015.
 */
public class Server {


    public  static  void main (String []argh){

                Thread clientThread = new Thread(() -> {

                    connectClientDB();

                });
                    clientThread.start();

    }


    public static void connectClientDB(){
        try {

            ClientDB clientDB = new ClientDB();
            Registry reg = LocateRegistry.createRegistry(Constant.Remote_port);
            reg.bind(Constant.Remote_ID,clientDB);

            System.out.println("Server is now Running..");

          //  System.out.println(clientDB.getAdminKeyCode("wew"));


        }catch (RemoteException e){
            e.printStackTrace();
            System.out.println("Server:Server.java:connectClientDB: RemoteException ");

        } catch (AlreadyBoundException e) {
            System.out.println("Server:Server.java:connectClientDB: AlreadyBoundException ");
        }
    }


}
