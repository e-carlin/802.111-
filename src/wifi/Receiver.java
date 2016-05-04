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
	private Vector<byte[]> dataRcvd; //Received data packets
	private ConcurrentLinkedQueue<byte[]> acksToSend; //Acks we need to send
	private ConcurrentLinkedQueue<byte[]> rcvdACK;  //Acks we've received
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
			if(destAddr == this.ourMAC || destAddr == -1){ //This is intended for us

				if(PacketManipulator.isBeaconFrame(packet)){ //If it is a beacon frame then update our clock
					LinkLayer.updateClock(PacketManipulator.getTimeFromBeacon(packet));
				}
				else{
					if(dataRcvd.size() < 5){ //Drop the incoming packet if there are already 4 queued up
						if(PacketManipulator.isDataPacket(packet)){
							dataRcvd.add(packet);
							if(destAddr != -1){ //We don't ACK broadcast packets

								short srcAddr = PacketManipulator.getSourceAddr(packet);
								int seqNum = PacketManipulator.getSeqNum(packet);
								byte[] ackPacket =  PacketManipulator.buildACKPacket(srcAddr, this.ourMAC, seqNum);
								//throw ackPacket on shared queue
								acksToSend.add(ackPacket);
							}
						}else if(PacketManipulator.isACKPacket(packet))
							rcvdACK.add(packet);
					}
				}
			}
			try{ //Sleep a bit
				Thread.sleep(100); //sleep for 100ms
			}
			catch(InterruptedException e){ //If interrupted during sleep
				this.output.println("Interrupted while sleeping in Recv "+e);
			}
		}

	}
}


