package MainApp;

import RMI.ClientInterface;
import global.OnlineClient;
import utility.TimedRMIclientSocketFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by Didoy on 2/24/2016.
 */
public class ClientIntefaceFactory {

    public static ClientInterface getClientInterface(OnlineClient client ){
        Registry reg = null;
        ClientInterface clientInterface = null;


        TimedRMIclientSocketFactory csf = new TimedRMIclientSocketFactory(6000);

        try {
            reg = LocateRegistry.getRegistry(System.setProperty("java.rmi.server.hostname",
                    client.getIpaddress()), client.getPort(), csf);
             clientInterface = (ClientInterface) reg.lookup(client.getREMOTE_ID());

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        return clientInterface;
    }
}
