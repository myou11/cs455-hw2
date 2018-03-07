# Using Thread Pools to Manage and Load Balance Active Network Connections

##### CS455 - Distributed Systems - ASG2

##### Maxwell You

## Program Overview
This program models a real-world scenario in which a server must be able to handle the requests of many clients. In order to keep response times
quick, a thread pool is used to handle the incoming requests. It is more feasible to have one thread managing all requests than it is to
context switch between threads when requests come in. Thread pools also allow the workload to be distributed among all the threads since each
thread will pull tasks from the workQueue as they finish processing their current task. 

In this program, the user specfies the number of threads they want in the thread pool. The Server is started and Clients can connect and begin
transmitting random 8KB of data to the Server while also storing the hashed digest of this data. The Server adds the 8KB of data to a workQueue.
Any free worker thread will dequeue the data from the workQueue and compute the hash of the random 8KB of data. Then, the thread sends the hash back
to the Client that sent it. The Client will check the incoming hash against its list of sent hashses to verify if the Server processed the data
correctly. If the hash matches, it is removed from the Client's list of sent hashes.

## File Descriptions (by grouping):
### **client**
  - **Client**: Generates 8KB of random bytes to hash and send to the Server. Keeps track of the hashed messages so that Client can verify 
        if hash sent back by the Server was processed correctly.
  - **ClientSenderThread**: Continually sends random 8KB to the Server at a rate specified by the user (e.g. 2, 3, 4 messages per second).
  - **ClientStateThread**: Prints the state of the Client every 20s 
        (e.g. messages sent in past 20 seconds, size of sentHashCodes (to ensure hashes are being removed correctly)).

### **server**
  - **Server**: Maintains a Selector that accepts incoming connections and traffic. The Selector will register incoming connections and 
        add incoming traffic to the workQueue.
  - **ThreadPool**: Maintains a number of TaskHandler threads specified by the user. These
  - **TaskHandler**: When there is work, read data from the workQueue, hash the data, and write the data back to the Client that sent it. 
        Afterwards, check if there is work to be done again.

### **util**
  - **StatisticsCollector**: Every 20 seconds, print the server throughput, active client connections, mean per-client throughput, 
        and standard deviation of per-client throughput. Clear the throughput for each client after printing these statistics so
        we can get statistics for the next 20 seconds.

### **Disclaimers**
My program calculates throughput as a function of **messages per second for the last 20 seconds** (*msgs/s/20*)


