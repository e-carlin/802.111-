package wifi;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import rf.RF;

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

	private int collisions = this.theRF.aCWmin; //The number of collisions that have occurred
	private final int TIMEOUT = 1000; //milliseconds - This is completely made up need better number
	private final int DIFS = 50/1000; //Also made up

	Sender(RF rfLayer, ConcurrentLinkedQueue<byte[]> data, ConcurrentLinkedQueue<byte[]> rcvdACK){
		this.theRF = rfLayer;
		this.dataToTrans = data;
		this.rcvdACK = rcvdACK;
	}



	/**
	 * A method that waits DIFS and attempts to transmit a packet
	 * @return whether or not we were able to successfully transmit the packet
	 */
	private boolean waitAndSendData(){

		boolean keepTrying = true;
		while(keepTrying){
			try{ //Sleep the thread for DIFS
				Thread.sleep(0,50000); //Wait 50 microseconds ******Is that the correct DIFS wait ???*****
			}
			catch(InterruptedException e){ //If interrupted during sleep
				System.out.println("Interrupted while waiting DIFS "+e);

			}

			if(!theRF.inUse()){ //The channel is still idle
				
				//wait our necessary exponential backoff time pausing whenever channel is in use
				
				//Calculating the necessary backoff
				Random rn = new Random();
				if(collisions > theRF.aCWmax) //We have maxed out the cw
					collisions = theRF.aCWmax;
				int cw = rn.nextInt(collisions - theRF.aCWmin + 1) + theRF.aCWmin;
				double interval = Math.random()*cw;
				int wait = (int) (interval * theRF.aSlotTime);
				
				try{ //Wait our random time
					Thread.sleep(wait);
				}
				catch(InterruptedException e){ //If interrupted during sleep
					System.out.println("Interrupted while waiting exponential backoff "+e);

				}
				
				//Transmit after waiting
				this.theRF.transmit(dataToTrans.peek()); //transmit the frame
				System.out.println("Transmitting data!");
				return true; //We transmitted the frame so we are done
			}
		}

		//The channel was no longer idle so we were unable to send the frame
		System.out.println("Channel was no longer idle after waiting DIFS");
		return false;
	}

	private boolean waitForACK(){
		long startTime = LinkLayer.clock();
		while(LinkLayer.clock() < (startTime + TIMEOUT)){ //we haven't timed out
			if(!rcvdACK.isEmpty()){ //We've rcvd an ack
				//Check to make sure it is the right seq #
				rcvdACK.poll(); //remove the ACK
				this.collisions = 0; //Reset the number of collisions because we succesfully transmitted
				return true; //Our packet has been ACK'ed
			}

		}
		return false; //we timed out
	}

	@Override
	public void run() {
		while(true){

			while(!dataToTrans.isEmpty()){ //while there is data to transmit
				if(waitAndSendData()){ //try to send the packet
					if(waitForACK()){ //After sending the packet wait for an ACK
						dataToTrans.poll(); //Remove this packet
						break; //We've rcvd the ACK so were all done with this packet!
					}
					else{ //We didn't rcv an ACK so there must have been a collision
						//Exponential backoff and retransmit
						collisions ++; //Increment the collision counter
						waitAndSendData();
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



}



