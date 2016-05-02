package wifi;

import rf.RF;
import java.util.concurrent.ConcurrentLinkedQueue;




/**
 * Need to add max number of tries to transmit probably in while loop with backoff in run()
 */



/**
 * Sends packets over RF when they are supplied
 * @author Evan Carlin and Ethan Russell
 * @version 04//03/2016
 *
 */
public class Sender implements Runnable {
	private RF theRF; 
	private ConcurrentLinkedQueue<byte[]> dataToTrans;
	private ConcurrentLinkedQueue<byte[]> rcvdACK;
	private ConcurrentLinkedQueue<byte[]> acksToSend;

	private int cw = this.theRF.aCWmin; //The current collision window
	private int collisionCount = 0; //The number of collisions that have occurred since the last successful transmit
	private int reTrys = 0; //the number of times we have tried to send a packet
	private final int TIMEOUT = 10000; //milliseconds - This is completely made up need better number
	private final int DIFS = this.theRF.aSIFSTime + 2*this.theRF.aSlotTime; 
	private final int SIFS = this.theRF.aSIFSTime; 

	Sender(RF rfLayer, ConcurrentLinkedQueue<byte[]> data, ConcurrentLinkedQueue<byte[]> rcvdACK, ConcurrentLinkedQueue<byte[]>acksToSend){
		this.theRF = rfLayer;
		this.dataToTrans = data;
		this.rcvdACK = rcvdACK;
		this.acksToSend = acksToSend;
	}

	/**
	 * Waits for channel to be idle
	 * ****Should we be sleeping in here waiting for channel to be idle??
	 */
	private void waitForIdleChannel(){
		while(this.theRF.inUse()){
			try{ //Sleep for a bit before checking to see if idle
				Thread.sleep(100); //Wait .1 seconds
			}
			catch(InterruptedException e){ //If interrupted during sleep
				System.out.println("Interrupted while waiting for Idle Channel "+e);
			}
		}
		System.out.println("Channel is now idle!");
	}

	/**
	 * Waits DIFS
	 */
	private void waitDIFS(){
		System.out.println("Waiting DIFS "+this.DIFS);
		try{ //Sleep the thread for DIFS
			Thread.sleep(this.DIFS); //sleep for DIFS
		}
		catch(InterruptedException e){ //If interrupted during sleep
			System.out.println("Interrupted while waiting DIFS "+e);

		}
	}

	/**
	 * A method that waits DIFS and attempts to transmit a packet
	 * @return whether or not we were able to successfully transmit the packet
	 */
	private void waitAndSendData(){

		if(!this.theRF.inUse()){ //medium is idle				
			waitDIFS(); //Wait DIFS		
			if(!this.theRF.inUse()){ //medium is still idle
				this.theRF.transmit(dataToTrans.peek()); //transmit the frame
				System.out.println("Transmitting data!");
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
		backoffAndTransmit();
		return; //We transmitted the frame so we are done!

	}

	private void backoffAndTransmit(){
		//Exponential backoff while medium idle
		this.cw = this.cw*2 + 1; //Exponential increase

		if(this.cw > this.theRF.aCWmax)
			this.cw = this.theRF.aCWmax; //cw can't be greater than the max cw

		System.out.println("CW = " +this.cw);

		int backoff = (int) ((Math.random()*this.cw) * this.theRF.aSlotTime);

		while(backoff > 0){ //wait the backoff while sensing and pausing when channel isn't idle
			System.out.println("Waiting backoff = "+backoff);
			if(this.theRF.inUse()){ //the channel is in use so wait a little bit
				try{ //Sleep the thread for aSlotTime
					Thread.sleep(this.theRF.aSlotTime); //sleep for DIFS
					System.out.println("Sleeping aSlotTime in backoff");
				}
				catch(InterruptedException e){ //If interrupted during sleep
					System.out.println("Interrupted while sleeping aSlotTime "+e);

				}
			}
			else if(!this.theRF.inUse())
				break;
			backoff -= this.theRF.aSlotTime;
		}



		System.out.println("Waiting DIFS after backoff "+this.DIFS);
		//Transmit after waiting
		this.theRF.transmit(dataToTrans.peek()); //transmit the frame
		System.out.println("Transmitting data!");
	}

	private boolean waitForACK(){
		long startTime = LinkLayer.clock();
		while(LinkLayer.clock() < (startTime + TIMEOUT)){ //we haven't timed out
			if(!rcvdACK.isEmpty()){ //We've rcvd an ack
				//Check to make sure it is the right seq #
				rcvdACK.poll(); //remove the ACK
				this.cw = this.theRF.aCWmin; //Reset collision window because successful transmit
				this.collisionCount = 0; ////Reset the number of collisions because ""
				System.out.println("Packet has been ACK'ed");
				return true; //Our packet has been ACK'ed
			}

		}
		System.out.println("Timed out while waiting for ACK");
		return false; //we timed out
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
						dataToTrans.poll(); //Remove this packet
						break; //We've rcvd the ACK so were all done with this packet!
					}else{ //We timed out while waiting for an ACK so there must have been a collision
						//Exponential backoff and retransmit
						while(true){
							this.reTrys++;
							if(this.reTrys > this.theRF.dot11RetryLimit){ //we've reached the retry limit
								System.out.println("Reached retry limit!");
								//Reset everything
								this.reTrys = 0;
								this.collisionCount = 0;
								this.cw = this.theRF.aCWmin;
								dataToTrans.poll(); //Remove the packet we can't seem to send
								break;
							}
							System.out.println("There was a collision");
							this.collisionCount ++; //Increment the collision counter
							backoffAndTransmit();
							if(waitForACK()){ //After sending the packet wait for an ACK
								dataToTrans.poll(); //Remove this packet
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
				System.out.println("Interrupted while waiting for data to brodcast "+e);

			}
		}

	}

	private void waitAndSendAck() {
		while(true){
			if(!this.theRF.inUse()){ //medium is idle				
				waitSIFS(); //Wait SIFS		
				if(!this.theRF.inUse()){ //medium is still idle
					this.theRF.transmit(acksToSend.poll()); //transmit the frame
					System.out.println("Transmitting Ack!");
					return; //We transmitted the AcK so we are done
				}
			}
			try{
				Thread.sleep(this.theRF.aSlotTime);
			}
			catch(InterruptedException e){ //If interrupted during sleep
				System.out.println("Interrupted while waiting for idle channel to transmit ACK "+e);

			}
		}
	}


	private void waitSIFS() {
		System.out.println("Waiting SIFS "+this.SIFS);
		try{ //Sleep the thread for DIFS
			Thread.sleep(this.SIFS); //sleep for DIFS
		}
		catch(InterruptedException e){ //If interrupted during sleep
			System.out.println("Interrupted while waiting SIFS "+e);

		}

	}
}



