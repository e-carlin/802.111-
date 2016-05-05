# CS325
Ethan Russell and Evan Carlin

Our implementation of 802.11~ is based on two threads - a sender and receiver.  The sender thread takes care of most of our logic for timing/collisions.  It shares Vectors with the other classes - dataa that need to be sent, acks that needs to be sent, and acks that have been received by the receiver thread.  We use a class called PacketManipulator with static methods that do all of the bit math with the raw packets.  It then returns an array of bytes for the other threads to send.  We call its functions to build packets based on our input as well as parse out the required information from receieved packets.  The receive thread simply blocks until a packet is received.  When data is received, it creates an ack packet and sends it to the sender thread via the shared queue for it to transmit.  The clock method is a static method inside the link layer class that adds an offset stored in the link layer.  The clock gets moved forward when the receiver thread spots a beacon and calls the updateClock method which recalculates the offset if the sent clock is larger. 

Commands available:
	0)Print Commands //(this)
	1)Set Diagnostic Level //we only use 1 or 0 (0 default)
	2)Slot Random/Fixed //Expecting 0 for random or 1 for fixed. (0 default)
	3)Beacon Frame Interval //in seconds.  0 for disable (5 default)