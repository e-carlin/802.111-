package wifi;
import java.util.Arrays;
import java.util.Vector;

import rf.RF;

/**
 * Builds packets and sends them whenever data is supplied
 * @author Evan Carlin and Ethan Russell
 * @version 04//03/2016
 *
 */
public class Sender implements Runnable {
	private RF theRF;
	private Vector<byte[]> dataToTrans;

	Sender(RF rfLayer, Vector<byte[]> data){
		this.theRF = rfLayer;
		this.dataToTrans = data;
	}

	@Override
	public void run() {
		while(true){

			while(!dataToTrans.isEmpty()){ //while there is data to transmit
				if(!theRF.inUse()){ //while the channel is idle
					System.out.println("Sending "+ Arrays.toString(dataToTrans.get(0))); //***TESTING
					this.theRF.transmit(dataToTrans.get(0)); //transmit the data
					dataToTrans.removeElementAt(0); //Delete the packet because it has been sent
				}
				try{ //Sleep the thread a bit before checking for idle channel
					Thread.sleep(200); //Wait .5 second
				}
				catch(InterruptedException e){ //If interrupted during sleep
					System.out.println("Interrupted while waiting for idle channel "+e);

				}
			}

			try{ //Sleep the thread a bit before checking again for new data
				Thread.sleep(200); //Wait .5 second
			}
			catch(InterruptedException e){ //If interrupted during sleep
				System.out.println("Interrupted while waiting for data to brodcast "+e);

			}
		}

	}

}
