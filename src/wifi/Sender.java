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
	private final int TIMEOUT = 500; //This is completely made up need better number
	private final int DIFS = 50/1000; 

	Sender(RF rfLayer, ConcurrentLinkedQueue<byte[]> data){
		this.theRF = rfLayer;
		this.dataToTrans = data;
	}
	
	/**
	 * A method that waits DIFS and attempts to transmit a packet
	 * @return whether or not we were able to successfully transmit the packet
	 */
	private boolean waitAndSendData(){
		//*******Wait DIFS*******
		if(!theRF.inUse()){ //The channel is still idle
			this.theRF.transmit(dataToTrans.poll()); //retrieve, transmit, and remove frame from queue
			System.out.println("Transmitting data!");
			return true; //We transmitted the frame so we are done
		}
		//The channel was no longer idle so we were unable to send the frame
		System.out.println("Channel was no longer idle after waiting DIFS");
		return false;
	}
	
	/**
	 * This function waits a certain interval for an ACK 
	 * @return Whether or not the ACK was received before the timeout
	 */
	private boolean waitForACK(){
		return true;
	}

	@Override
	public void run() {
		while(true){

			while(!dataToTrans.isEmpty()){ //while there is data to transmit
				System.out.println("Is data?? "+ PacketManipulator.isDataPacket(dataToTrans.peek()));
				if(!theRF.inUse()){ //If the channel is idle - Left side of FSD
					if(waitAndSendData()){ //try to send the packet
						// Wait for ACK
						//if no ACK after timeout there was a collision so call another routine
					}
					//waited DIFS and then the channel was no longer idle.
					else //we couldn't transmit the data
						break; //start waiting for channel to be idle again
				}
				
				
				
				try{ //Sleep the thread a bit before checking for idle channel
					Thread.sleep(200); //Wait .2 second
				}
				catch(InterruptedException e){ //If interrupted during sleep
					System.out.println("Interrupted while waiting for idle channel "+e);

				}
			}

			try{ //Sleep the thread a bit before checking again for new data
				Thread.sleep(200); //Wait .2 second
			}
			catch(InterruptedException e){ //If interrupted during sleep
				System.out.println("Interrupted while waiting for data to brodcast "+e);

			}
		}

	}

}
