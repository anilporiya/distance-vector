import java.net.InetAddress;
//import java.sql.Time;
//import java.util.concurrent.ConcurrentHashMap;

/***
 * Entry class is used to store information
 * for a route. It includes destination address,
 * next hop address, cost to get to the destination node.
 * The format of the entry table for every node is of the
 * form :
 * 
 * 	Address         Next Hop            Cost
    ================================================
    10.0.1.0/24     0.0.0.0:0           0
    10.0.2.0/24     127.0.0.1:63002     1
 * 
 * where Address or network is of the form : 
 * x.x.x.x/x
 * Next Hop is of the form:
 * x.x.x.x:x
 * 
 * and cost is in the form of metric.
 * 
 * @author arp6763 Anil Poriya
 *
 */
public class Entry {
	
	private int noOfHops;
	private InetAddress destAddress;
	private InetAddress nextHop;
	private String nextHopWPort;
	private boolean removed = false;
	private long updateTime;
	private String CIDR;
	
	public String getCIDR() {
		return CIDR;
	}
	public void setCIDR(String cIDR) {
		CIDR = cIDR;
	}
	public boolean isRemoved() {
		return removed;
	}
	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
	public long getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
	Entry(int noOfHops, InetAddress destAddress, InetAddress nextHop,String nextHopWPort){
		this.noOfHops = noOfHops;
		this.destAddress = destAddress;
		this.nextHop = nextHop;
		this.nextHopWPort = nextHopWPort;
		this.updateTime = System.currentTimeMillis();
	}
	public String getNextHopWPort() {
		return nextHopWPort;
	}
	public void setNextHopWPort(String nextHopWPort) {
		this.nextHopWPort = nextHopWPort;
	}
	public int getNoOfHops() {
		return noOfHops;
	}
	public void setNoOfHops(int noOfHops) {
		this.noOfHops = noOfHops;
	}
	public InetAddress getDestAddress() {
		return destAddress;
	}
	public void setDestAddress(InetAddress destAddress) {
		this.destAddress = destAddress;
	}
	public InetAddress getNextHop() {
		return nextHop;
	}
	public void setNextHop(InetAddress nextHop) {
		this.nextHop = nextHop;
	}
	

}
