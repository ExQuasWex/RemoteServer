package MainApp.ClientSide;

import RMI.ClientInterface;
import RMI.Constant;
import global.OnlineClient;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * Created by reiner on 11/23/2015.
 */
public class OnlineClientArrayList extends ArrayList<OnlineClient> implements Runnable {

    private  TimedRMIclientSocketFactory csf;
    private         int x = 0;

    public OnlineClientArrayList() throws RemoteException {

         csf = new TimedRMIclientSocketFactory(6000);

        Thread thread = new Thread(this){
        };


            thread.start();


    }


    @Override
    public void run() {

        try {
            while (true){
                checkOnlines();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void checkOnlines() throws InterruptedException {


        if (isEmpty()){
            Thread.sleep(5000);
            System.out.println("Online size: " + size());
        }else {

                    while ( x <= size() ) {
                        Thread.sleep(5000);
                        OnlineClient client = get(x);
                        System.out.println("Online size: " + size());



                                        try {
                                            System.setProperty("java.rmi.server.hostname", client.getIpaddress());
                                            Registry reg = LocateRegistry.getRegistry(client.getIpaddress(), Constant.ClientPort,csf);

                                            ClientInterface Ci = (ClientInterface) reg.lookup(Constant.Remote_ID);
                                            Ci.imAlive();


                                        } catch (RemoteException e) {

                                            e.printStackTrace();
                                            remove(client);
                                            System.out.println(client.getUsername() + " is Now offline");

                                        } catch (NotBoundException e) {
                                            e.printStackTrace();
                                        }


                                        if (x == size()-1){
                                            x = 0;
                                        }else {
                                            System.out.println("X++");
                                            x++;
                                        }
                    }

        }


    }
}
