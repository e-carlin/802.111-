package wifi;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * A static class to construct and de-construct network order packets (byte arrays)
 * @author Evan Carlin and Ethan Russell
 * @version 04/03/2016
 *
 */
public class PacketBuilder {
	
	private static int SIZE_BUF = 10; //There are always 10 bytes of non-data info in a packet (Ex. src address, checksum...)
	/*
	 * Constructs network ordered data packets
	 * The order in which each element is put in the buffer is important
	 */
	public static byte[] buildDataPacket(short dest, short source, byte[] data, int len){
		//For creating packet to transmit
		//Network order byte array
		ByteBuffer buffer = ByteBuffer.allocate(SIZE_BUF+len);
		
		//Will need to use something like a BitSet here to be able to manipulate individual bits in the control part of frame
		buffer.put(new byte[2]);
		
		buffer.putShort(dest); //add the destination MAC address
		buffer.putShort(source); //Our MAC address
		buffer.put(data); //add data ????? is it in network byte order????
		
		//Make a real CRC. All 1's for now
		byte[] crc = {1,1,1,1};
		buffer.put(crc);
		

		byte[] toSend = buffer.array(); //the array to send
		return toSend;
	}
	
}
