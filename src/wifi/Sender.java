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
		// TODO Auto-generated method stub
		
	}

}
