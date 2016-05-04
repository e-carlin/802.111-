package wifi;

import rf.RF;

import java.io.PrintWriter;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;



/**
 * Timeout times were calculated between a machine running osx 10.11 and a virtual machine running Unbuntu 14.04.4
 */



/**
 * Sends packets over RF when they are supplied
 * @author Evan Carlin and Ethan Russell
 * @version 04//03/2016
 *
 */
public class Sender implements Runnable {
	private RF theRF; 
	private PrintWriter output;
	private Vector<byte[]> dataToTrans; //Data waiting to be transmitted
	private ConcurrentLinkedQueue<byte[]> rcvdACK; //ACKS received in Receiver thread
	private ConcurrentLinkedQueue<byte[]> acksToSend; //ACKS waiting to be transmitted

	private int cw = this.theRF.aCWmin; //The current collision window
	private int collisionCount = 0; //The number of collisions that have occurred since the last successful transmit
	private int reTrys = 0; //the number of times we have tried to send a packet
	private final int ACK_TIMEOUT = this.theRF.aSIFSTime +400+ this.theRF.aSlotTime; // How long to wait for an ACK = SIFS + ACK Transmission Duration + SlotTime 

	private final int DIFS = this.theRF.aSIFSTime + 2*this.theRF.aSlotTime; 
	private final int SIFS = this.theRF.aSIFSTime; 



	Sender(RF rfLayer, Vector<byte[]> data, ConcurrentLinkedQueue<byte[]> rcvdACK, ConcurrentLinkedQueue<byte[]>acksToSend, PrintWriter output){
		this.theRF = rfLayer;
		this.dataToTrans = data;
		this.rcvdACK = rcvdACK;
		this.acksToSend = acksToSend;
		this.output = output;
	}

	/**
	 * Waits for channel to be idle
	 */
	private void waitForIdleChannel(){
		while(this.theRF.inUse()){
			try{ //Sleep for a bit before checking to see if idle
				Thread.sleep(100); //Wait .1 seconds
			}
			catch(InterruptedException e){ //If interrupted during sleep
				this.output.println("Interrupted while waiting for Idle Channel "+e);
			}
		}
		//		this.output.println("Channel is now idle!");
	}

	/**
	 * Waits SIFS
	 */
	private void waitSIFS() {
		this.output.println("Waiting SIFS "+this.SIFS);
		try{ //Sleep the thread for DIFS
			Thread.sleep(this.SIFS); //sleep for DIFS
		}
		catch(InterruptedException e){ //If interrupted during sleep
			LinkLayer.statusCode = LinkLayer.UNSPECIFIED_ERROR;
			this.output.println("Interrupted while waiting SIFS "+e);

		}

	}

	/**
	 * Waits DIFS
	 */
	private void waitDIFS(){
		this.output.println("Waiting DIFS");
		try{ //Sleep the thread for DIFS
			Thread.sleep(this.DIFS); //sleep for DIFS
		}
		catch(InterruptedException e){ //If interrupted during sleep
			LinkLayer.statusCode = LinkLayer.UNSPECIFIED_ERROR;
			this.output.println("Interrupted while waiting DIFS "+e);
		}
	}

	/**
	 * Waits until an ACK arrives or a timeout occurs
	 * @return true if we received an ACK before timeout
	 */
	private boolean waitForACK(){
		long startTime = LinkLayer.clock();
		while(LinkLayer.clock() < (startTime + ACK_TIMEOUT)){ //we haven't timed out
			if(!rcvdACK.isEmpty()){ //We've rcvd an ack
				//Check to make sure it is the right seq #
				rcvdACK.poll(); //remove the ACK
				this.cw = this.theRF.aCWmin; //Reset collision window because successful transmit
				this.collisionCount = 0; ////Reset the number of collisions because ""
				this.output.println("Packet has been ACK'ed");
				return true; //Our packet has been ACK'ed
			}
			try{ //Sleep the thread for 20ms
				Thread.sleep(20);
			}
			catch(InterruptedException e){ //If interrupted during sleep
				this.output.println("Interrupted while sleeping waiting for an ACK "+e);
				LinkLayer.statusCode = LinkLayer.UNSPECIFIED_ERROR;
			}

		}
		this.output.println("Timed out while waiting for ACK");
		return false; //we timed out
	}

	/**
	 * Wait SIFS and then send the ACK 
	 */
	private void waitAndSendAck() {
		waitSIFS(); //Wait SIFS		
		this.output.print("Transmitting ACK ");
		PacketManipulator.printPacket(output, acksToSend.peek());
		this.theRF.transmit(acksToSend.poll()); //transmit the frame
	}

	/**
	 * A method that waits DIFS and attempts to transmit a packet if the channel is idle
	 * @return whether or not we were able to successfully transmit the packet
	 */
	private void waitAndSendData(){

		if(!this.theRF.inUse()){ //medium is idle				
			waitDIFS(); //Wait DIFS		
			if(!this.theRF.inUse()){ //medium is still idle
				this.theRF.transmit(dataToTrans.get(0)); //transmit the frame
				this.output.println("Transmitting data!");
				LinkLayer.statusCode = LinkLayer.TX_DELIVERED;
				return; //We transmitted the frame so we are done
			}
		}
		//The channel was in use so we must wait for it to be idle
		while(true){
			waitForIdleChannel();
			waitDIFS();
			if(!this.theRF.inUse()) //The channel is finally idle
				break;
		}
		backoffAndTransmit();  //Since the channel was in use we must go to the exponential backoff state
		return; //We transmitted the frame so we are done!
	}

	/**
	 * Enters the exponential backoff state to wait to transmit
	 */
	private void backoffAndTransmit(){
		//Exponential backoff while medium idle
		this.cw = this.cw*2 + 1; //Increase collision window

		if(this.cw > this.theRF.aCWmax)
			this.cw = this.theRF.aCWmax; //cw can't be greater than the max cw

		this.output.println("CW = " +this.cw);

		int backoff = (int) ((Math.random()*this.cw) * this.theRF.aSlotTime);

		while(backoff > 0){ //wait the backoff while sensing and pausing when channel isn't idle
			this.output.println("Waiting backoff = "+backoff);
			if(this.theRF.inUse()){ //the channel is in use so wait a little bit
				try{ //Sleep the thread for aSlotTime
					Thread.sleep(this.theRF.aSlotTime);
					this.output.println("Sleeping while waiting for idle channel");
				}
				catch(InterruptedException e){ //If interrupted during sleep
					this.output.println("Interrupted while sleeping while waiting for idle channel in backoff "+e);
					LinkLayer.statusCode = LinkLayer.UNSPECIFIED_ERROR;
				}
			}
			try{ //Sleep the thread for aSlotTime
				Thread.sleep(this.theRF.aSlotTime); //sleep for DIFS
				this.output.println("Sleeping aSlotTime in backoff");
			}
			catch(InterruptedException e){ //If interrupted during sleep
				this.output.println("Interrupted while sleeping aSlotTime "+e);
				LinkLayer.statusCode = LinkLayer.UNSPECIFIED_ERROR;
			}
			backoff -= this.theRF.aSlotTime;
		}



		//Transmit after waiting
		this.theRF.transmit(dataToTrans.get(0)); //transmit the frame - don't remove it because we need to wait for an ACK
		this.output.println("Transmitting data!");
		LinkLayer.statusCode = LinkLayer.TX_DELIVERED;
	}



	@Override
	public void run() {
		while(true){
			while(!(dataToTrans.isEmpty() && acksToSend.isEmpty())){ //while there is data to transmit
				if(!acksToSend.isEmpty()){
					waitAndSendAck(); //acks get priority
				}else{
					waitAndSendData(); //Do necessary sensing, waiting, and transmitting
					if(waitForACK()){ //After sending the packet wait for an ACK
						dataToTrans.remove(0);; //Remove this packet
						break; //We've rcvd the ACK so were all done with this packet!
					}else{ //We timed out while waiting for an ACK so there must have been a collision
						//Exponential backoff and retransmit
						while(true){
							this.reTrys++;
							if(this.reTrys > this.theRF.dot11RetryLimit){ //we've reached the retry limit
								this.output.println("Reached retry limit!");
								//Reset everything
								this.reTrys = 0;
								this.collisionCount = 0;
								this.cw = this.theRF.aCWmin;
								dataToTrans.remove(0); //Remove the packet we can't seem to send
								break;
							}
							this.output.println("There was a collision");
							this.collisionCount ++; //Increment the collision counter
							backoffAndTransmit();
							if(waitForACK()){ //After sending the packet wait for an ACK
								dataToTrans.remove(0); //Remove this packet
								break; //We've rcvd the ACK so were all done with this packet!
							}
							//else retry because we didn't retrieve an ACK
						}
					}
				}
			}
			try{ //Sleep the thread a bit before checking again for new data
				Thread.sleep(100); //Wait .1 second
			}
			catch(InterruptedException e){ //If interrupted during sleep
				this.output.println("Interrupted while waiting for data to brodcast "+e);

			}
		}

	}
}



