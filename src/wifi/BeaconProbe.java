package wifi;
import java.util.TimerTask;

public class BeaconProbe extends TimerTask{

	@Override
	public void run() {
		if(LinkLayer.diagLevel >= 1) LinkLayer.output.println("Creating Beacon");
		byte[] beaconPacket = PacketManipulator.buildBeaconPacket((short)LinkLayer.BROADCAST_ADDR, LinkLayer.ourMAC);
		LinkLayer.sender.dataToTrans.insertElementAt(beaconPacket, 0); //add to head of data vector for sending right away
	}

}
