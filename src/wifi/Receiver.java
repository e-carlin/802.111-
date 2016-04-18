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
	//TODO shared AcksToSend queue (sender waits SIFS always, sends ack if needed, waits remaining time for DIFS otherwise)
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
			System.out.println("Rcvd a packet");
			System.out.println("Is it data? "+ PacketManipulator.isDataPacket(data));
			
			 //Check to make sure we are the desired destination or -1 for a broadcast message
			short destAddr = PacketManipulator.getDestAddr(data);
			short srcAddr = PacketManipulator.getSourceAddr(data);
			int seqNum = PacketManipulator.getSeqNum(data);
			if(destAddr == this.ourMAC || destAddr == -1){
				if(PacketManipulator.isDataPacket(data)){
					dataRcvd.add(data);
					if(destAddr != -1){
						byte[] ackPacket;
						ackPacket = PacketManipulator.buildACKPacket(srcAddr, this.ourMAC, seqNum);
						System.out.printf("ack(%d) ", seqNum);
						PacketManipulator.printPacket(ackPacket);
						//TODO throw ackPacket on shared queue
						this.theRF.transmit(ackPacket);
					}
				}else if(PacketManipulator.isACKPacket(data))
					rcvdACK.add(data);
			
				//**** need to add else if to check for Beacons ****
			}
		}

	}

}
