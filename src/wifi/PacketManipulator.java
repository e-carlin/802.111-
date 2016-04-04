package wifi;

import java.nio.ByteBuffer;

import java.util.Arrays;
import java.util.BitSet;

/**
 * A static class to construct and de-construct network order packets (byte arrays)
 * *****These methods have only been lightly tested and need to double checked to make sure they are working*****
 * @author Evan Carlin and Ethan Russell
 * @version 04/03/2016
 *
 */
public class PacketManipulator {
	
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
	/*
	 * A method that extracts the destination MAC address from an 802.11~ packet
	 */
	public static short getDestAddr(byte[] data){
		int dest = 0;
		for(int i=2; i<4;i++){ //First two bytes are destination MAC address
			dest =  ((dest << 8) + (data[i] & 0xff));
		}
		return (short)dest;
		
	}
	
	/*
	 * A method that extracts the data from an 802.11~ packet
	 * ****This method has not been tested at all not sure if it works******
	 */
	public static byte[] getData(byte[] recvdData){
		
		byte[] data = new byte[recvdData.length-10]; //-6 -4 = -10 (6 bytes for control and addressing, 4 for CRC)
		for(int i=6; i<recvdData.length-4;i++){ //6 bytes to length-4 (eliminate control, addressing, and CRC) is where data can lie
			data[i-6] = recvdData[i];
		}
		
		return data;
	}
	
}
