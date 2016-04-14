package wifi;

import java.util.Arrays;
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

	private final int TIMEOUT = 1000; //milliseconds - This is completely made up need better number
	private final int DIFS = 50/1000; //Also made up

	Sender(RF rfLayer, ConcurrentLinkedQueue<byte[]> data, ConcurrentLinkedQueue<byte[]> rcvdACK){
		this.theRF = rfLayer;
		this.dataToTrans = data;
		this.rcvdACK = rcvdACK;
	}

	/**
	 * This function waits a certain interval for an ACK 
	 * @return Whether or not the ACK was received before the timeout
	 */
	private boolean waitForACK(){

		System.out.println("Waiting for ACK");
		long startTime = LinkLayer.clock();
		while(LinkLayer.clock() > startTime + TIMEOUT){ //We have not yet reached the timeout
			while(!rcvdACK.isEmpty()){ //An ACK was rcvd
				rcvdACK.poll(); //We no longer need this ACK
				//****Probably should check sequence numbers here
				return true;
			}
		}
		return false;
	}	

	/**
	 * A method that waits DIFS and attempts to transmit a packet
	 * @return whether or not we were able to successfully transmit the packet
	 */
	private boolean waitAndSendData(){

		try{ //Sleep the thread for DIFS
			Thread.sleep(0,50000); //Wait 50 microseconds ******Is that the correct DIFS wait ???*****
		}
		catch(InterruptedException e){ //If interrupted during sleep
			System.out.println("Interrupted while waiting DIFS "+e);

		}

		if(!theRF.inUse()){ //The channel is still idle
			this.theRF.transmit(dataToTrans.peek()); //transmit the frame
			System.out.println("Transmitting data!");
			return true; //We transmitted the frame so we are done
		}

		//The channel was no longer idle so we were unable to send the frame
		System.out.println("Channel was no longer idle after waiting DIFS");
		return false;
	}



	@Override
	public void run() {
		while(true){

			while(!dataToTrans.isEmpty()){ //while there is data to transmit
				if(!theRF.inUse()){ //If the channel is idle then begin otherwise keep waiting until it is idle
					if(waitAndSendData()){ //try to send the packet
						System.out.println("Sent data");
						if(waitForACK()){ //We rcvd an ACK for this packet
							dataToTrans.poll(); //This packet has been ACK'ed so we can get rid of it
							System.out.println("Removed old packet");
							break; //All done with current packet
						}
						//if no ACK after timeout there was a collision so call another routine
					}
					//waited DIFS and then the channel was no longer idle.
					else 
						break; //start waiting for channel to be idle again
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
