package wifi;
import java.util.Vector;

import rf.RF;

public class Sender implements Runnable {
	private RF theRF;
	private Vector<Short> dataToTrans;
	
	Sender(RF rfLayer, Vector<Short> data){
		this.theRF = rfLayer;
		this.dataToTrans = data;
	}

	@Override
	public void run() {
		while(true){
			byte[] test = {0,1};
			this.theRF.transmit(test);
			
			double wait = Math.random() * 500;
			try{ //Needed for thread sleep
				Thread.sleep((long) wait); //Wait a random time between 1-7 sec.
			}
			catch(InterruptedException e){ //If interrupted during sleep
				System.out.println("Broadcast Interrupted "+e);

			}
		}
		
	}

}
