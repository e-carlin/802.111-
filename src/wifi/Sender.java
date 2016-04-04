package wifi;
import java.util.Arrays;
import java.util.Vector;

import rf.RF;

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
			System.out.println("Sending "+ Arrays.toString(dataToTrans.get(0)));
			this.theRF.transmit(dataToTrans.get(0));
			dataToTrans.removeElementAt(0);
			}
			
			try{ //Needed for thread sleep
				Thread.sleep(500); //Wait .5 second
			}
			catch(InterruptedException e){ //If interrupted during sleep
				System.out.println("Broadcast Interrupted "+e);

			}
		}
		
	}

}
