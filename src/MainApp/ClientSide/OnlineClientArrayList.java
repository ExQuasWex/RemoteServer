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
import java.util.Iterator;

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
                Thread.sleep(5000);
                checkOnlines();
                System.out.println("Online size: " + size());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void checkOnlines() throws InterruptedException {

        if (isEmpty()){
        }else {
            Iterator ite = iterator();
            while (ite.hasNext()){
                OnlineClient client = (OnlineClient) ite.next();
                System.out.println(client.getUsername());

                try {
                    System.setProperty("java.rmi.server.hostname", client.getIpaddress());
                    Registry reg = LocateRegistry.getRegistry(client.getIpaddress(), Constant.ClientPort,csf);

                    ClientInterface Ci = (ClientInterface) reg.lookup(Constant.Remote_ID);
                    Ci.imAlive();


                } catch (RemoteException e) {
                    ite.remove();
                    System.out.println(client.getUsername() + " is Now offline");
                    e.printStackTrace();


                } catch (NotBoundException e) {
                    e.printStackTrace();
                }

            }

        }


    }

    public boolean updateUsername(String oldUsername, String newUsername){

        Iterator iterator = iterator();
            while (iterator.hasNext()){
                OnlineClient client = (OnlineClient) iterator.next();

                    if (client.getUsername().equals(oldUsername)){
                        client.setUsername(newUsername);
                        return true;
                    }
            }
        return false;
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
