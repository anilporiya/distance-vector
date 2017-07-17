
/***
 * NetworkNode.java
 */
import java.io.BufferedReader;
import java.io.File;
//import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/***
 * The class NetworkNode basically parses the input from the file for each node
 * and triggers receiver and sender threads for all the links given in the file.
 *  
 * @author arp6763
 *
 */
class NetworkNode {

	public static void main(String args[]) throws IOException {
		// HashMap<String,Entry> entryList = new HashMap<>();
		ConcurrentHashMap<String, Entry> entryList = new ConcurrentHashMap<>();
		// Scanner scan = new Scanner(System.in);
		// String fileName = scan.nextLine();
		File config = new File(args[0]);
		BufferedReader bf = new BufferedReader(new FileReader(config));
		String read;
		while ((read = bf.readLine()) != null) {
			String[] line = read.trim().split(" ");
			if (line[0].contains("LINK")) {
				// System.out.println("inside Link");
				String[] addrPort1 = line[1].split(":");
				String[] addrPort2 = line[2].split(":");
 				int port1 = Integer.parseInt(addrPort1[1]);
 				int port2 = Integer.parseInt(addrPort2[1]);
				//System.out.println("Sender Port - "+ senderPort);
				/***
				 * Starts a new thread acting as a receiver interface for the
				 * node.
				 */
				new Thread(new ReceiveEnd(port1, entryList,port2)).start();

				/**
				 * Starts a new thread acting as a sender interface for the
				 * node, sending updated table to connected opposite interface
				 * every 5 seconds.
				 */
				new Thread(new SendEnd(addrPort2[0], entryList,port2)).start();

			} else if (line[0].contains("NETWORK")) {
				// System.out.println("Inside Network");
				String[] network = line[1].trim().split("/");
				// String address = line[1].substring(0,line[1].indexOf('/'));
				String address = network[0];
				String CIDR = "";
				if (network.length > 1) {
					CIDR = network[1];// line[1].substring(line[1].indexOf('/')+1);
				}

				InetAddress destAddress = InetAddress.getByName(address);
				InetAddress nextHop = InetAddress.getByName("0.0.0.0");
				synchronized(entryList){
					Entry entry = new Entry(0, destAddress, nextHop, "0.0.0.0:00000");
					// entry.setRemoved(false);
					entry.setCIDR(CIDR);
					entryList.put(address, entry);
				}
				

			}
		}
		
		/***
		 * Printing the initial routing table for the node which will only
		 * contain the current network it can reach(that is the network
		 * in which the node itself is). 
		 */
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>> Initial Table <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		System.out.println("Address" + "\t\t" + "Next Hop" + "\t\t" + "Hops To Reach Dest");
		System.out.println("==================================================================");
		for (String key : entryList.keySet()) {
			Entry entry = entryList.get(key);
			System.out.println(entry.getDestAddress().getHostAddress() + "/" + entry.getCIDR() + "\t"
					+ (entry.getNextHopWPort()) + "\t\t" + entry.getNoOfHops());
		}
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Initial Table <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		bf.close();
		// scan.close();

	}

}
