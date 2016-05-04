package wifi;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author richards
 */
public class LinkLayer implements Dot11Interface {
	private static final int WINDOW_SIZE = 100; //TODO: choose window size
	private static RF theRF;           // You'll need one of these eventually
	private short ourMAC;       // Our MAC address
	private PrintWriter output; // The output stream we'll write to
	private static long offset = 0; //The amount of clock offset

	private HashMap<Short, Integer> sequenceMap; //maps mac addresses to the current sequence number

	//Data shared with threads
	private ConcurrentLinkedQueue<byte[]> dataToTrans; //Outgoing data app->transmit
	private ConcurrentLinkedQueue<byte[]> rcvdACK;
	private Vector<byte[]> dataRcvd; //Incoming data recv->app
	private ConcurrentLinkedQueue<byte[]> acksToSend;

	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output;      
		this.theRF = new RF(null, null);

		sequenceMap = new HashMap<Short, Integer>();

		//Shared between sender and recvr
		this.rcvdACK = new ConcurrentLinkedQueue<byte[]>();
		this.acksToSend = new ConcurrentLinkedQueue<byte[]>();

		//The sender thread
		this.dataToTrans = new ConcurrentLinkedQueue<byte[]>();
		Sender sender = new Sender(this.theRF, this.dataToTrans, this.rcvdACK, this.acksToSend, this.output);
		(new Thread(sender)).start();

		//The receiver thread
		this.dataRcvd = new Vector<byte[]>();
		Receiver recvr = new Receiver(this.theRF, this.dataRcvd, this.ourMAC, this.rcvdACK, this.acksToSend, this.output, null);
		(new Thread(recvr)).start();

		output.println("LinkLayer initialized using a random MAC address:"+this.ourMAC);
	}

	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 */
	public int send(short dest, byte[] data, int len) {
		output.println("LinkLayer: Sending "+len+" bytes to "+dest);

		//add the packet to the shared Vector
		Integer currentSeqNumber = sequenceMap.get(dest);
		if(currentSeqNumber == null){
			int newSeqNumber = 0; //always start with 0;
			sequenceMap.put(dest, newSeqNumber);
			currentSeqNumber = newSeqNumber;
		}
		//Construct the data packet
		byte[] toSend = PacketManipulator.buildDataPacket(dest, this.ourMAC, data, len, currentSeqNumber);

		currentSeqNumber = (currentSeqNumber + toSend.length) % WINDOW_SIZE;
		sequenceMap.put(dest, currentSeqNumber);

		boolean successAdding = dataToTrans.add(toSend);
		if(successAdding) //success adding to the vector
			return len;
		else
			return -1;
	}

	/**
	 * Recv method blocks until data arrives, then writes info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
		output.println("LinkLayer: Blocking on recv()");
		while(this.dataRcvd.isEmpty()){ //While there is no new data rcvd (block)

			try{ 
				Thread.sleep(100); //Wait
			}
			catch(InterruptedException e){ //If interrupted during sleep
				output.println("Interrupted while blocking in recv() "+e);

			}
		}

		//There is new info so process it
		byte[] dataRcvd = this.dataRcvd.get(0);

		//add the info to the transmission object
		short destAddr = PacketManipulator.getDestAddr(dataRcvd);
		t.setDestAddr(destAddr);

		short sourceAddr = PacketManipulator.getSourceAddr(dataRcvd);
		t.setSourceAddr(sourceAddr);

		byte[] data = PacketManipulator.getData(dataRcvd);
		t.setBuf(data); 

		this.dataRcvd.remove(0); //delete the packet we just processed

		return data.length;
	}

	/**
	 * This function allows for updating the clock value as well as adding an offset so that it is synced with other clocks
	 * @param time If -1 the method returns the current clock time (with offset) if != -1 then it will try to update the clock
	 * time and give back the new clock time. It only updates the clock time if it would be greater than current clock time
	 * @return the clock time
	 */
	public static long clock(){
		return theRF.clock() + offset;
	}

	public static void updateClock(long time){
		System.out.println("Beacon time = "+time);
		System.out.println("Curr time   = "+clock());
		if(time > clock())
			offset = time - clock();
		System.out.println("Updated time= "+clock()+"\n");
		
	}
	/**
	 * Returns a current status code.  See docs for full description.
	 */
	public int status() {
		output.println("LinkLayer: Faking a status() return value of 0");
		return 0;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		output.println("LinkLayer: Sending command "+cmd+" with value "+val);
		return 0;
	}
}
