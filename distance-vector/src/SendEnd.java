import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
//import java.util.ArrayList;
//import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/***
 * 
 * The class SendEnd will send packets to the destination port with its routing
 * table entries. However for all the nodes that can be reached through the port
 * that it is sending to, it will not regard that entry for sending. This is
 * basically done to achieve poison reverse in case any node goes down.
 * 
 * @author arp6763
 *
 */
public class SendEnd implements Runnable {

	DatagramSocket sendSock;
	DatagramPacket packSend;
	// HashMap<String, Entry> entryList;
	ConcurrentHashMap<String, Entry> entryList;
	int portNum;
	String address;
	int costIndex = 19;

	byte[] sendData = new byte[256];

	SendEnd(String address, ConcurrentHashMap<String, Entry> list,int port2) throws SocketException, UnknownHostException {
		entryList = list;
		//int indexSlash = line.indexOf(':');
		portNum = port2;
		this.address = address;
		// System.out.println("portNum : "+ portNum + " address : "+address);
		sendSock = new DatagramSocket();

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			sendRoutingTables();
			// Thread.sleep(5000);

		} catch (InterruptedException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	/***
	 * sendRoutingTables determines which entries of the routing table it needs
	 * to send to a particular node and sends a packet containing all those
	 * entries.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void sendRoutingTables() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		while (true) {
			//Thread.sleep(5000);
			byte[] dataArray;
			synchronized (entryList) {
				/***
				 * We synchronize the entryList which is a HashMap of our
				 * routing entries. This is because two sender threads of same
				 * node can simultaneously access this table which may lead to
				 * discrepancies. Thus we make sure only one thread accesses the
				 * table at one time.
				 */

				if (entryList.size() > 0) {
					int byteArrSize = caclulateSizeByteArray();
					// byte array based on then number of packets.
					dataArray = new byte[(byteArrSize * 20) + 4];
					/***
					 * Initialize the first 4 bytes of the packet. Command,
					 * Version and zero fields.
					 */
					dataArray[0] = 2;
					dataArray[1] = 2;
					dataArray[2] = 0;
					dataArray[3] = 0;
					int entries = 0;
					for (String key : entryList.keySet()) {
						/***
						 * For every entry, we create a byte array to store the
						 * address, CIDR, hop address and cost metric
						 * information and store it in the main byte array.
						 * We have omitted certain entries due to implementation
						 * of split horizon.
						 */
						Entry entry = entryList.get(key);
						if (!entry.getNextHopWPort().contains("" + portNum)) {
							byte[] entryBuffer = new byte[20];
							entryBuffer[1] = 2;
							byte[] addressArr = entry.getDestAddress().getAddress();
							// System.out.println(addressArr.length);
							for (int count = 4; count < 8; count++) {
								entryBuffer[count] = addressArr[count - 4];
							}
							
							// byte[] CIDR = new byte[4];
							String cidr = entry.getCIDR();
							/***
							 * calculate CIDR part of the network and store
							 * the CIDR bytes into the node packet.
							 */
							if (cidr != "") {
								int val = 0xffffffff << (32 - Integer.parseInt(cidr));
								byte[] cidrByte = new byte[] { (byte) (val >>> 24), (byte) (val >> 16 & 0xff),
										(byte) (val >> 8 & 0xff), (byte) (val & 0xff) };
								for (int count = 0; count < 4; count++) {
									entryBuffer[count + 8] = cidrByte[count];
								}
							}
							byte[] hopAdd = entry.getNextHop().getAddress();
							for (int count = 12; count < 16; count++) {
								entryBuffer[count] = hopAdd[count - 12];
							}
							entryBuffer[costIndex] = (byte) entry.getNoOfHops();
							
							/***
							 * finally byte array of each entry containing 
							 * Address, hop Address, CIDR and cost metric is 
							 * stored into the final packet of node at
							 * the appropriate location.
							 * 
							 */
							for (int buff = 0; buff < 20; buff++) {
								dataArray[(entries * 20) + buff + 4] = entryBuffer[buff];
							}
							entries++;
						}

					}

				} else {
					/***
					 * No Entries in the table. Send an empty packet.
					 */
					dataArray = new byte[1];
				}
				//System.out.println("Sending to Port : "+portNum + " packet length :" + dataArray.length);
				DatagramPacket packSend = new DatagramPacket(dataArray, dataArray.length,
						InetAddress.getByName(address), portNum);
				sendSock.send(packSend);

			}
			Thread.sleep(5000);
		}

	}

	/***
	 * This method calculates the number of entries in the routing table that
	 * need to be sent to other routers in the network. It avoids those entries
	 * where the next hop is via the portNumber it wants to send the packet to.
	 * Avoid miscalculations.
	 * 
	 * @return size
	 */
	private int caclulateSizeByteArray() {
		int size = 0;

		for (String key : entryList.keySet()) {

			Entry entry = entryList.get(key);
			
			if (!entry.getNextHopWPort().contains("" + portNum)) {
				size++;

			}
		}
		return size;
	}

}
