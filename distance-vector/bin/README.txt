Anil Poriya

RIT ID: arp6763@rit.edu

What if Node goes down?
	--> To implement the poison reverse technique, we use
		timeout mechanism to determine whether a node is up
		or down in the network. Whenever the packet is not 
		received from a particular port, we determine that
		node as timed out and conclude that node has gone 
		down in the network. We can either update cost values for
		that node as infinity or remove entries for that node
		from the routing table.
		
		
	--> Whenever the node goes down, the entries for that node
		will be removed from the routing tables of all the nodes.
	--> It is observed that when a node is removed from the 
		network, node may be removed and added again in the routing
		tables of some nodes due to packet sending before the network
		gets stable. However, this node is finally removed from all routing tables
		after few updates.
	--> Thus while removing a node or when adding a new node, there
		will be few incorrect updations before the correct values are
		populated in the routing tables.



Timeout for Socket:

--> The timeout taken for our socket to determine whether
a node is down from the network is 10 seconds.
--> also for checking a node's update time the time taken is
12 seconds to be on the safer side.

Build and Run Instructions:

While running the program, the program will take command
line parameter as the full path of the file with the
txt notation.

e.g. C:\Users\anilp\workspacealgos\CNBackup\src\simpletest1.txt

Thus to run the java program use the following command : 

	java NetworkNode C:\Users\anilp\workspacealgos\CNBackup\src\simpletest1.txt
	java NetworkNode C:\Users\anilp\workspacealgos\CNBackup\src\simpletest2.txt
	java NetworkNode C:\Users\anilp\workspacealgos\CNBackup\src\simpletest3.txt
	java NetworkNode C:\Users\anilp\workspacealgos\CNBackup\src\simpletest4.txt