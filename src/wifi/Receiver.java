package wifi;

import java.util.Vector;
import rf.RF;


/**
 * A class that listens for data on the RF layer and passes it back to the LinkLayer recv()
 * @author Evan Carlin and Ethan Russell
 *
 */
public class Receiver implements Runnable {

	private RF theRF;
	private Vector<byte[]> dataRcvd;
	private short ourMAC;

	Receiver(RF rf, Vector<byte[]> data, short ourMAC){
		this.theRF = rf;
		this.dataRcvd = data;
		this.ourMAC = ourMAC;
	}
	@Override
	public void run() {

		while(true){
			byte[] data = this.theRF.receive(); //block until a packet is received
			 //Check to make sure we are the desired destination or -1 for a broadcast message
			if(PacketManipulator.getDestAddr(data) == this.ourMAC || PacketManipulator.getDestAddr(data) == -1)
				dataRcvd.add(data);
		}

	}

}
