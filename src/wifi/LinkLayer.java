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
	
	public static int diagLevel;
	public static boolean slotRandom;
	public static int beaconInterval;
	public static int statusCode;
	
	public static final int SUCCESS						=1;
	public static final int UNSPECIFIED_ERROR			=2;
	public static final int RF_INIT_FAILED				=3;
	public static final int TX_DELIVERED				=4;
	public static final int TX_FAILED					=5;
	public static final int BAD_BUF_SIZE				=6;
	public static final int BAD_ADDRESS					=7;
	public static final int BAD_MAC_ADDRESS				=8;
	public static final int ILLEGAL_ARGUMENT			=9;
	public static final int INSUFFICIENT_BUFFER_SPACE	=10;
	private static long RFClockOffset = 0; //The amount of clock offset

	private HashMap<Short, Integer> sequenceMap; //maps mac addresses to the current sequence number

	//Data shared with threads
	private Vector<byte[]> dataToTrans; //Outgoing data app->transmit
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
		LinkLayer.statusCode = SUCCESS;
		this.ourMAC = ourMAC;
		this.output = output;      
		this.theRF = new RF(null, null);
		
		sequenceMap = new HashMap<Short, Integer>();

		//Shared between sender and recvr
		this.rcvdACK = new ConcurrentLinkedQueue<byte[]>();
		this.acksToSend = new ConcurrentLinkedQueue<byte[]>();

		LinkLayer.diagLevel = 0;
		LinkLayer.slotRandom = true;
		LinkLayer.beaconInterval = 5; //Default to interval of 5 seconds
		
		//The sender thread
		this.dataToTrans = new Vector<byte[]>();
		Sender sender = new Sender(this.theRF, this.dataToTrans, this.rcvdACK, this.acksToSend, this.output);
		(new Thread(sender)).start();

		//The receiver thread
		this.dataRcvd = new Vector<byte[]>();
		Receiver recvr = new Receiver(this.theRF, this.dataRcvd, this.ourMAC, this.rcvdACK, this.acksToSend, this.output);
		(new Thread(recvr)).start();

		output.println("LinkLayer initialized using a random MAC address:"+this.ourMAC);
	}

	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 */
	public int send(short dest, byte[] data, int len) {
		if(dataToTrans.size() > 3) //Don't queue more than 4 packets
			return 0;
			
		output.println("LinkLayer: Trying to send "+len+" bytes to "+dest);

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
		else{
			LinkLayer.statusCode = LinkLayer.TX_FAILED;
			return -1;
		}
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
		return theRF.clock() + RFClockOffset;
	}

	public static void updateClock(long time){
		if(time > clock())
			RFClockOffset = time - clock();
		
	}
	/**
	 * Returns a current status code.  See docs for full description.
	 */
	public int status() {
		return statusCode;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		//output.println("LinkLayer: Sending command "+cmd+" with value "+val);
		
		switch(cmd){
			case 0:
				output.println("Diagnostic level: " + diagLevel);
				output.print("Collision Window Slot Choice:");
				if(slotRandom) output.println("Random.");
					else output.println("Fixed.");
				output.println("Beacon Interval: " + beaconInterval + " seconds.");
				output.println("Commands available:\n\t0)Print Commands\n\t1)Set Diagnostic Level\n\t2)Slot Random/Fixed\n\t3)Beacon Frame Interval");
				break;
			case 1:
				output.println("Setting diagnostic level to "+ val);
				LinkLayer.diagLevel = val;
				break;
			case 2:
				if(val==0){
					output.println("Setting Slot Window to Random.");
					slotRandom = true;
				}else if (val==1){
					output.println("Setting Slot Window to Fixed.");
					slotRandom = false;
				}else{
					output.println("Expecting 0 for random or 1 for fixed.  Try again.");
				}
				break;
			case 3:
				output.println("Setting beacon interval to "+val+" seconds.");
				LinkLayer.beaconInterval = val;
				break;
		}
		return 0;
	}
}
