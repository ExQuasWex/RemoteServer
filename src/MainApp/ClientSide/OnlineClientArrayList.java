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
class OnlineClientArrayList extends ArrayList<OnlineClient> implements Runnable {

    private  TimedRMIclientSocketFactory csf;
    private  int x = 0;
    private Registry reg;
    private Thread thread;

    public OnlineClientArrayList() throws RemoteException {

         csf = new TimedRMIclientSocketFactory(6000);

         thread = new Thread(this){
        };


            thread.start();


    }

    protected void start(){

    }

    @Override
    public void run() {

        try {
            while (true){
                Thread.sleep(1000);
                System.out.println("starting thread");
                checkOnlines();
                System.out.println("thread end .. ");

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void checkOnlines() throws InterruptedException {


        if (isEmpty()){
            System.out.println("Online size: " + size());
        }else {
            System.out.println("Online size: " + size());
                for (OnlineClient client : this){
                            try {
                                System.setProperty("java.rmi.server.hostname", client.getIpaddress());
                                Registry reg = LocateRegistry.getRegistry(client.getIpaddress(), Constant.ClientPort,csf);

                                ClientInterface Ci = (ClientInterface) reg.lookup(Constant.Remote_ID);
                                Ci.imAlive();


                            } catch (RemoteException e) {

                                e.printStackTrace();
                                remove(client);
                                System.out.println(client.getUsername() + " is Now offline");
                                checkOnlines();

                            } catch (NotBoundException e) {
                                e.printStackTrace();
                            }
                }

        }


    }

    public void removeUserToList(String username){
        int ctr = 0;
        OnlineClient client;

        while (ctr <= size()){
            client = get(ctr);
            System.out.println("Size: " + size());
            System.out.println("logout: " + client.getUsername());
                    if (client.getUsername().equals(username)){
                       remove(client);
                    }
            ctr++;
        }

    }


}
