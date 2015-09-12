package MainApp;

import RMI.Constant;

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
                    clientThread.start();;

    }


    public static void connectClientDB(){
        try {

            ClientDB clientDB = new ClientDB();
            Registry reg = LocateRegistry.createRegistry(Constant.Remote_port);
            reg.bind(Constant.Remote_ID,clientDB);

            System.out.println("Server is now Running..");

        }catch (RemoteException e){

            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }

    }


}
