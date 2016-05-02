package wifi;

import java.io.PrintWriter;
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
	private PrintWriter output;
	private Vector<byte[]> dataRcvd; //data shared with LinkLayer
	private ConcurrentLinkedQueue<byte[]> acksToSend;
	//TODO shared AcksToSend queue (sender waits SIFS always, sends ack if needed, waits remaining time for DIFS otherwise)
	private ConcurrentLinkedQueue<byte[]> rcvdACK; 
	private short ourMAC; //our MAC address

	Receiver(RF rf, Vector<byte[]> data, short ourMAC, ConcurrentLinkedQueue<byte[]> rcvdACK, ConcurrentLinkedQueue<byte[]> acksToSend, PrintWriter output){
		this.theRF = rf;
		this.dataRcvd = data;
		this.rcvdACK = rcvdACK;
		this.ourMAC = ourMAC;
		this.acksToSend = acksToSend;
		this.output = output;
	}
	@Override
	public void run() {

		while(true){
			byte[] packet = this.theRF.receive(); //block until a packet is received	
			
			 //Check to make sure we are the desired destination or -1 for a broadcast message
			short destAddr = PacketManipulator.getDestAddr(packet);
			if(destAddr == this.ourMAC || destAddr == -1){
				
				output.println("Rcvd a packet");
				output.println("Is it data? "+ PacketManipulator.isDataPacket(packet));
				
				if(PacketManipulator.isDataPacket(packet)){
					dataRcvd.add(packet);
					if(destAddr != -1){ //We don't ACK broadcast packets
						
						short srcAddr = PacketManipulator.getSourceAddr(packet);
						int seqNum = PacketManipulator.getSeqNum(packet);
						byte[] ackPacket =  PacketManipulator.buildACKPacket(srcAddr, this.ourMAC, seqNum);

						output.printf("ack(%d:%d) ", srcAddr, seqNum);
						//throw ackPacket on shared queue
						acksToSend.add(ackPacket);
					}
				}else if(PacketManipulator.isACKPacket(packet))
					rcvdACK.add(packet);
			
				//**** need to add else if to check for Beacons ****
			}
		}

	}

}
