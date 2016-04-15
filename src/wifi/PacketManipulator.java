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
	

	
	private static int SIZE_CONTROL = 2; //2 bytes of control
	private static int SIZE_ADDR = 2; //2 bytes of address
	private static int SIZE_CRC = 4; //4 bytes of CRC
	private static int MIN_SIZE_BUF = SIZE_CONTROL + SIZE_ADDR*2 + SIZE_CRC; //There are always 10 bytes of non-data info in a packet (Ex. src address, checksum...)

	/**
	 * Constructs network ordered data packets
	 * The order in which each element is put in the buffer is important
	 * @param dest the destination MAC address
	 * @param source the source MAC address
	 * @param data the data to be transmitted
	 * @param len the length of data (number of bytes)
	 * @return the fully constructed packet
	 */
	public static byte[] buildDataPacket(short dest, short source, byte[] data, int len, int sequenceNum){
		ByteBuffer buffer = ByteBuffer.allocate(MIN_SIZE_BUF+len); //Min 10 bytes of control, address, and CRC + len of data
		int controlBits = 0b000_00000;
		int controlMask = 0b111_00000;
		int seqMSBMask = 0x0F;
		int seqMSB = sequenceNum >> 8;
		
		byte firstByte = (byte)((controlBits & controlMask) + (seqMSB & seqMSBMask));
		byte secondByte = (byte)(0xFF & sequenceNum);
		buffer.put(new byte[] {firstByte, secondByte});
			
		buffer.putShort(dest); //add the destination MAC address
		buffer.putShort(source); //Our MAC address
		buffer.put(data); //add data

		//Make a real CRC. All 1's for now - CRC32 example
		byte[] crc = {1,1,1,1};
		buffer.put(crc);


		byte[] toSend = buffer.array(); //the array to send
		return toSend;
	}
	
	/**
	 * Constructs ack  packet
	 * @param dest the destination MAC address
	 * @param source the source MAC address
	 * @return the fully constructed packet
	 */
	public static byte[] buildACKPacket(short dest, short source, int sequenceNum){
		ByteBuffer buffer = ByteBuffer.allocate(MIN_SIZE_BUF); //10 bytes of control, address, and CRC
		
		int controlBits = 0b001_00000; //ack
		int controlMask = 0b111_00000;
		int seqMSBMask = 0x0F;
		int seqMSB = sequenceNum >> 8;
		
		byte firstByte = (byte)((controlBits & controlMask) + (seqMSB & seqMSBMask));
		byte secondByte = (byte)(0xFF & sequenceNum);
		buffer.put(new byte[] {firstByte, secondByte});
			
		buffer.putShort(dest); //add the destination MAC address
		buffer.putShort(source); //Our MAC address

		//Make a real CRC. All 1's for now - CRC32 example
		byte[] crc = {1,1,1,1};
		buffer.put(crc);


		byte[] toSend = buffer.array(); //the array to send
		return toSend;
	}
	
	/**
	 * sets an existing packet retry bit to 1
	 * @param prevPacket existing packet
	 * @return modified packet
	 */
	public static byte[] setRetryBit(byte[] prevPacket){
		Byte retryMask = 0b0001_0000;
		prevPacket[0] |= retryMask;
		return prevPacket;
	}
	
	/**
	 * A method that extracts the destination adress from a packet
	 * @param data the packet we want to extract address from
	 * @return the destination adress in the packet (bytes 2-4)
	 */
	public static short getDestAddr(byte[] data){
		int dest = 0;
		for(int i=2; i<4;i++){ //Bytes 2-4 are destination MAC address
			dest =  ((dest << 8) + (data[i] & 0xff));
		}
		return (short)dest;

	}

	/**
	 * Extracts the source address from a packet
	 * @param data the packet we want to extract the address from
	 * @return the source address in the packet (bytes 4-6)
	 */
	public static short getSourceAddr(byte[] data){
		int dest = 0;
		for(int i=4; i<6;i++){ //Bytes 4-6 are destination MAC address
			dest =  ((dest << 8) + (data[i] & 0xff));
		}
		return (short)dest;

	}

	/**
	 * A method that extracts the data from a packet
	 * ***Test more***
	 * @param recvdData The packet we want to extract the data from
	 * @return the data extracted from the packet (bytes 8 - recvdData.len-4) 
	 */
	public static byte[] getData(byte[] recvdData){

		byte[] data = new byte[recvdData.length-MIN_SIZE_BUF]; //-6 -4 = -10 (6 bytes for control and addressing, 4 for CRC)
		for(int i=6; i<recvdData.length-4;i++){ //6 bytes to length-4 (eliminate control, addressing, and CRC) is where data can lie
			data[i-6] = recvdData[i];
		}

		return data;
	}
	
	/**
	 * ****** I think this works??*****
	 * This function reads the Frame Type bits of the Control field to determine
	 * if the packet is a data packet
	 * @param recvdData The packet to examine
	 * @return Whether or not the packet is a data packet
	 */
	
	public static boolean isDataPacket(byte[] recvdData){
		
		byte typeData = 0b000_0000;
		
		if( (recvdData[0] & 0b1110_0000) == typeData ) //if the type of rcvd packet is data
			return true;
		else
			return false;
	}
	
	public static boolean isACKPacket(byte[] recvdData){
		
		byte typeACK = 0b0010_0000;
		if((recvdData[0] & 0b1110000) == typeACK)
			return true;
		else
			return false;
	}

}
