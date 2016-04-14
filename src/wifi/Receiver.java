package wifi;

import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import rf.RF;


/**
 * A class that listens for data on the RF layer
 * @author Evan Carlin and Ethan Russell
 * @version 04/03/2016
 *
 */
public class Receiver implements Runnable {

	private RF theRF;
	private Vector<byte[]> dataRcvd; //data shared with LinkLayer
	private ConcurrentLinkedQueue<byte[]> rcvdACK; 
	private short ourMAC; //our MAC address

	Receiver(RF rf, Vector<byte[]> data, short ourMAC, ConcurrentLinkedQueue<byte[]> rcvdACK){
		this.theRF = rf;
		this.dataRcvd = data;
		this.rcvdACK = rcvdACK;
		this.ourMAC = ourMAC;
	}
	@Override
	public void run() {

		while(true){
			byte[] data = this.theRF.receive(); //block until a packet is received
			
			 //Check to make sure we are the desired destination or -1 for a broadcast message
			if(PacketManipulator.getDestAddr(data) == this.ourMAC || PacketManipulator.getDestAddr(data) == -1){
				if(PacketManipulator.isDataPacket(data))
					dataRcvd.add(data);
				else
					rcvdACK.add(data);
			
				//**** need to add else if to check for Beacons ****
			}
		}

	}

}
