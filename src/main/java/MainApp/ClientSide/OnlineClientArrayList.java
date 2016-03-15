package MainApp.ClientSide;

import MainApp.ClientIntefaceFactory;
import RMI.ClientInterface;
import RMI.TimedRMIclientSocketFactory;
import global.OnlineClient;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by reiner on 11/23/2015.
 */
public class OnlineClientArrayList extends ArrayList<OnlineClient>  {

    private static OnlineClientArrayList onlineClientslist = new OnlineClientArrayList();

    private OnlineClientArrayList()  {

    }

    private void checkOnlines() throws InterruptedException {
            Iterator ite = iterator();
            if (!ite.hasNext()){

            }else {
                iterateList(ite);
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

    public OnlineClient getClientCredential(String username){
        int ctr = 0;
        OnlineClient returnclient = null;

            Iterator iterator = iterator();

                while (iterator.hasNext()){
                   OnlineClient client = (OnlineClient) iterator.next();

                            if (client.getUsername().equals(username)){
                                returnclient = client;
                            }
                }
    return  returnclient;
    }

    public  boolean isTheAccountOnline(String username){
        boolean isOnline =false;
        Iterator iterator = iterator();

        while (iterator.hasNext()){
            OnlineClient client = (OnlineClient) iterator.next();

            if (client.getUsername().equals(username)){
                isOnline = true;
            }
        }
        return isOnline;
    }

    public static OnlineClientArrayList getInstance(){
        if (onlineClientslist == null){
            onlineClientslist = new OnlineClientArrayList();
        }
        return onlineClientslist;
    }

    public void monitorOnlines(){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true){
                        Thread.sleep(6000);
                         checkOnlines();
                    }

                } catch (InterruptedException e) {
                    System.out.println("InterruptedException");
                    e.printStackTrace();
                }
            }
        });
        
        thread.start();
    }

    private void iterateList(Iterator ite)  {
        while (ite.hasNext()){
            OnlineClient client = (OnlineClient) ite.next();
            // System.out.println(client.getUsername());

                synchronized (this){
                  TimedRMIclientSocketFactory csf = new TimedRMIclientSocketFactory(6000);
                    String HOST = client.getIpaddress();
                    int PORT = client.getPort();
                    String ID = client.getREMOTE_ID();
                    String username = client.getUsername();

                    System.out.println(HOST);

                    try {

                        csf.createSocket(HOST, PORT);

                        Registry reg = LocateRegistry.getRegistry( HOST  , PORT);
                        ClientInterface clientInterface = (ClientInterface) reg.lookup(ID);

                    } catch (RemoteException e) {
                        ite.remove();
                        System.out.println(username + " is Now offline");
                        e.printStackTrace();
                    } catch (NotBoundException e) {
                        ite.remove();
                        System.out.println(username + " is Now offline");
                        e.printStackTrace();
                    } catch (IOException e) {
                        ite.remove();
                        System.out.println(username + " is Now offline");
                        e.printStackTrace();
                    }

                }

        }
    }


}
