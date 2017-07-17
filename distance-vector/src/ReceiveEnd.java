import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 * The class ReceiveEnd will receive the packets from the identified port
 * numbers and wait for at least 10 seconds before determining the node as
 * unavailable. If node becomes unavailable it removes the entry of that node
 * from its routing table.
 * 
 * @author arp6763
 *
 */
public class ReceiveEnd implements Runnable {

	DatagramPacket packetRec;
	DatagramSocket recSocket;
	// HashMap<String,Entry> entryList;
	ConcurrentHashMap<String, Entry> entryList;
	byte[] recData = new byte[256];
	int portNum;
	// String address;
	int costIndex = 19;
	int senderPort;

	ReceiveEnd(int port1, ConcurrentHashMap<String, Entry> list, int port2) throws SocketException {
		entryList = list;
		portNum = port1;
		// address = line.substring(0, indexSlash);
		this.senderPort = port2;
		// System.out.println(senderPort);
		recSocket = new DatagramSocket(port1);
		recSocket.setSoTimeout(10000);

	}

	@Override
	public void run() {

		try {
			updateRoutingTables();
			// Thread.sleep(5000);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	/***
	 * Contains the actual logic of receiving packets and checking whether the
	 * current node's cost is better if it is already present or if the node is
	 * not present, make an entry into its routing table. If node times out,
	 * remove that node's entry.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void updateRoutingTables() throws IOException, InterruptedException {
		// boolean caught = false;
		while (true) {
			//Thread.sleep(5000);
			byte[] buff = new byte[256];
			DatagramPacket packetRec = new DatagramPacket(buff, buff.length);
			try {
				recSocket.receive(packetRec);
			} catch (SocketTimeoutException e) {
				/***
				 * If the socket times out(after 10 seconds of no packet
				 * received), it assumes that the node is down and will remove
				 * the entries corresponding to those where the nextHop is this
				 * node(down)'s port.
				 */
				synchronized (entryList) {
					boolean found = false;
					for (String key : entryList.keySet()) {
						Entry entry = entryList.get(key);
						if (entry.getNextHopWPort().contains("" + senderPort)
								|| (System.currentTimeMillis() - entry.getUpdateTime() > 10000)) {
							if (!(entry.getNextHopWPort().contains("0.0.0.0"))) {
								entryList.remove(key);
								// caught = true;
								found = true;
							}
						}
					}
					/***
					 * If entry for that node is present and removed from the
					 * table, we print the table because it is updated.
					 */
					if (found) {
						printTable();
					}
					continue;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}


			int noEntries = packetRec.getLength() / 20;

			/**
			 * check if the packet contains at least one entry to be processed.
			 */
			if (packetRec.getLength() > 4) {

				boolean entryPresent = false, valueChange = false;
				/**
				 * we synchronize our entryList which is a HashMap for storing
				 * all the routing table entries. If we do not synchronize, two
				 * receiver threads of this node may simultaneously update the
				 * routing table and may lead to corrupt or loss of data.
				 */
				synchronized (entryList) {

					for (int i = 0; i < noEntries; i++) {
						String destAddress = "";
						for (int j = i * 20 + 4 + 4; j < i * 20 + 8 + 4; j++) {
							// destAddress += recData[j] + ".";
							destAddress += buff[j] + ".";
						}
						destAddress = destAddress.substring(0, destAddress.length() - 1);

						String hopAddress = "";
						for (int j = i * 20 + 12 + 4; j < i * 20 + 16 + 4; j++) {
							// hopAddress += recData[j] + ".";
							hopAddress += buff[j] + ".";
						}

						hopAddress = hopAddress.substring(0, hopAddress.length() - 1);

						/***
						 * get the CIDR bytes from the packet.
						 */
						byte[] cidr = getCidrBytes(i, buff);

						/***
						 * Calculating CIDR part of the network if present.
						 */
						int maskVal = 0;
						for (byte b : cidr) {
							int mask1 = 0x80;

							for (int a = 0; a < 8; a++) {
								int result = b & mask1;
								if (result != 0) {
									maskVal++;
								}
								mask1 >>>= 1;
							}
						}
						// retrieve the cost or the metric to reach destination.

						int cost = buff[i * 20 + costIndex + 4];

						//

						for (String key : entryList.keySet()) {
							Entry entry = entryList.get(key);
							if (System.currentTimeMillis() - entry.getUpdateTime() > 10000
									&& !entry.getNextHopWPort().contains("0.0.0.0")) {
								entryList.remove(key);
								valueChange = true;
							}
						}
						entryPresent = false;
						for (String key : entryList.keySet()) {
							Entry entry = entryList.get(key);
							if (entry.getDestAddress().getHostAddress().equals(destAddress)
									&& !(System.currentTimeMillis() - entry.getUpdateTime() > 12000)) {
								
								entryPresent = true;

								/***
								 * If the entry is found in the table, we check
								 * if the current metric is better than the one
								 * present in the table. If it is better, we
								 * update the cost in the routing table and then
								 * update the "updateTime" of this entry to
								 * current time.
								 */
								if (entry.getNoOfHops() > (cost + 1)) {
									entry.setNoOfHops(cost + 1);

									entry.setNextHop(InetAddress.getByName(packetRec.getAddress().getHostAddress()));

									entry.setNextHopWPort(packetRec.getAddress().getHostAddress() + ":" + senderPort);

									/**
									 * If any of the entry changes, we keep a
									 * track so as to print the updated table
									 * once this is done.
									 */
									valueChange = true;

								}
								entry.setUpdateTime(System.currentTimeMillis());
								entryList.put(key, entry);
								break;
								/***
								 * If the cost metric is not better, we do not
								 * update the cost. We still update the
								 * "updateTime" of this entry indicating the
								 * node is still present in the network.
								 */

							}
						}

						/***
						 * Adds the entry to the routing table of the node, if
						 * entry for it is not present in the table.
						 */
						if (!entryPresent) {
								//System.out.println("Entry not present for address "+ destAddress);
							if (!(maskVal == 0)) {
								Entry entry = new Entry(cost + 1, InetAddress.getByName(destAddress),
										InetAddress.getByName(packetRec.getAddress().getHostAddress()),
										packetRec.getAddress().getHostAddress() + ":" + senderPort);
								entry.setCIDR("" + maskVal);
								entryList.put(destAddress, entry);
							} else {
								Entry entry = new Entry(cost + 1, InetAddress.getByName(destAddress),
										InetAddress.getByName(packetRec.getAddress().getHostAddress()),
										packetRec.getAddress().getHostAddress() + ":" + senderPort);
								entry.setUpdateTime(System.currentTimeMillis());
								entryList.put(destAddress, entry);
							}

							valueChange = true;
							// printTable();
						}

						/**
						 * If an entry for some node gets updated in the routing
						 * table, we print the table.
						 */

					}
					if (valueChange) {
						printTable();
					}
				}
			}

		}

	}

	/***
	 * Uses the respective net mask to calculate the CIDR part of the address.
	 * 
	 * @param pos
	 *            position of the routing table entry to get CIDR of the
	 *            corresponding packet present at that position.
	 * @return byte array containing the CIDR information.
	 */
	private byte[] getCidrBytes(int pos, byte[] buff) {

		byte[] bytes = { buff[4 + (pos * 20) + 8], buff[4 + (pos * 20) + 9], buff[4 + (pos * 20) + 10],
				buff[4 + (pos * 20) + 11] };
		return bytes;
	}

	/***
	 * print the routing table of the node every time the table of the node
	 * changes due to a new entry, removed entry or updated entry.
	 */
	private void printTable() {
		System.out.println("==================================================================");
		System.out.println("Address" + "\t\t" + "Next Hop" + "\t\t" + "Hops To Reach Dest");
		System.out.println("==================================================================");
		for (String key : entryList.keySet()) {
			Entry entry = entryList.get(key);
			System.out.println(entry.getDestAddress().getHostAddress() + "/" + entry.getCIDR() + "\t"
					+ (entry.getNextHopWPort()) + "\t\t" + entry.getNoOfHops());

		}

	}

}
