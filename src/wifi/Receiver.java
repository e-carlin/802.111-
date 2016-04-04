package wifi;

import java.util.Vector;
import rf.RF;

public class Receiver implements Runnable {

	private RF theRF;
	private Vector<byte[]> dataRcvd;
	
	Receiver(RF rf, Vector<byte[]> data){
		this.theRF = rf;
		this.dataRcvd = data;
	}
	@Override
	public void run() {
		
		while(true){
			dataRcvd.add(this.theRF.receive()); //block until a packet is received
		}

	}

}
