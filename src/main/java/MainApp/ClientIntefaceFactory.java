package MainApp;

import RMI.ClientInterface;
import RMI.TimedRMIclientSocketFactory;
import global.OnlineClient;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;

/**
 * Created by Didoy on 2/24/2016.
 */
public class ClientIntefaceFactory {
    private static TimedRMIclientSocketFactory CSF ;
    private static String USERNAME;
    private static String HOST;
    private static int PORT;
    private static String ID;

    public static ClientInterface getClientInterface(OnlineClient client){
        Registry reg = null;
        ClientInterface clientInterface = null;


        try {
            USERNAME = client.getUsername();
            HOST = client.getIpaddress();
            PORT = client.getPort();

            ID = client.getREMOTE_ID();


            //CSF = new TimedRMIclientSocketFactory(6000);

            reg = LocateRegistry.getRegistry(System.setProperty("java.rmi.server.useLocalHostname=true", HOST),
                    PORT);
             clientInterface = (ClientInterface) reg.lookup(ID);

        } catch (RemoteException e) {
            System.out.println("RemoteException");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("NotBoundException");
            e.printStackTrace();
        }
        return clientInterface;
    }

    public static ClientInterface getClientInterface(OnlineClient client, String host, int port){
        Registry reg = null;
        ClientInterface clientInterface = null;

        try {
            //CSF = new TimedRMIclientSocketFactory(6000);
            //csf.createSocket(host,port);
            System.out.println(host);

            reg = LocateRegistry.getRegistry(host, port);
            clientInterface = (ClientInterface) reg.lookup(client.getREMOTE_ID());

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return clientInterface;
    }



}
