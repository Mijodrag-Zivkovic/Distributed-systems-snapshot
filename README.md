# Distributed-systems-snapshot
Implementation of special algorithm that enables multiple snapshots in a distributed system, as well as multiple nodes to call snapshot at the same time. </br>
In a distributed system with constant transactions between nodes, sometimes we want to check the state of a system, in other words, 
how many of some currency there is in a system. This proccess is called snapshot. </br>

A naive solution for snapshot algorithm would be to just ask all nodes to tell us their current amount of said currency. Problem with this approach are transactions sent before
 snapshot call, but not recieved before snapshot occurs, which gives us bad snapshot result. </br>
 
There are algorithms that can solve this problem, but the downside is that they prevent transactions while snapshot is happening. In order to make system more efficient, we can
 use Lai-Yang algorithm, which doesn't stop transactions. All nodes keep history of recivied/given transactions, and send them along with the their amount of currency. Node that 
 has called snapshot recieves these "transaction histories", and by comparing them, is able to figure out how many transactions weren't recieved before snapshot call, which in turn 
 gives us correct result. Only problem with Lai-Yang algorithm is that it doesn't allow for multiple snapshot calls, just one. </br>
 
Li variation of Lai-Yang algorithm solves this problem. Just like in previous solution, every node keeps track of it's own transaction history, but this time it keeps 
 separate copies for every potential snapshot caller (we assume we are given a list of nodes which have privilege of calling snapshots). This allows multiple nodes 
 to call snapshot and still recieve good results. </br>
 
However, what if multiple nodes call snapshot at the same time? Instead of having multiple snapshots happening at the same time and clogging our system with unneccessary reply mesages,
  we want each node to send results to only one snapshot caller. Each snapshot caller would then get some of the results, and would then exchange results with other snapsjot callers,
  thus making every snapshot caller get correct final result. But how do we make sure each node only sends its result and history to one snapshot caller? </br>
  
That's where Spezialetti-Kearns algorithm comes in. It suggest making a tree structure throughout our system for every snapshot caller, 
effectively dividing system into regions. Each snapshot caller sends message to its neighbours. If neighbours are not part of any tree, they become part of snapshot caller's 
tree. Nodes that become children proceed to send same message to their neighbours, which either accept or refuse (if they are part of other tree). If asked node is already a part of some tree, 
it will notify its root (snapshot caller which got to said node first) that there are other snapshot callers making snapshots. This proccess repeats untill all nodes are part of some snapshot caller's tree. 
Snapshot callers will then recieve results of nodes belonging to their trees, after which they need to exchange these results with other snapshot callers. This part of algorithm happens in rounds. 
In each round, snapshot callers send their results to other snapshot callers. If they recieved results they previously didn't have, they save them and send them in the next round. 
If snapshot caller recieves only results they previously were aware of, they send blank messages to other snapshot callers. Algorithm ends when every snapshot caller recieves 
only blank messages from other snapshot callers. </br>

In this project, I have combined both Li variation of Lai-Yang and Spezialetti-Kearns algorithms, to take the best of both worlds. To run the code, just run "MultipleServerStarter", 
which will create separate proccess for every node. Configuration file for distributed system is located in "ly_snapshot1" directory. In this directory are also input, outpur and error files for each node.
 
