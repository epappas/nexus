/**
 * 
 */
package com.evalon.chrysalis.memory.flakes;

import com.evalon.chrysalis.util.serialization.ByteString;

import java.nio.ByteBuffer;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface ByteDrop
{
	int length();
	
	int capacity();
	
	ByteDrop write(int b);
	
	ByteDrop writeByte(int v);
	
	ByteDrop writeUnsignedByte(int v);
	
	ByteDrop write(int offset, int b);
	
	ByteDrop writeUnsignedByte(int offset, int v);
	
	ByteDrop write(byte[] b);
	
	ByteDrop write(int offset, byte[] b);
	
	ByteDrop write(byte[] b, int off, int len);
	
	ByteDrop writeBoolean(boolean v);
	
	ByteDrop writeBoolean(int offset, boolean v);
	
	ByteDrop writeShort(int v);
	
	ByteDrop writeShort(int offset, int v);
	
	ByteDrop writeUnsignedShort(int v);
	
	ByteDrop writeUnsignedShort(int offset, int v);
	
	ByteDrop writeCompactShort(int v);
	
	ByteDrop writeCompactUnsignedShort(int v);
	
	ByteDrop writeChar(int v);
	
	ByteDrop writeChar(int offset, int v);
	
	/**
	 * @param v
	 *            24-bit integer to write
	 */
	ByteDrop writeInt24(int v);
	
	ByteDrop writeInt24(int offset, int v);
	
	ByteDrop writeInt(int v);
	
	ByteDrop writeInt(int offset, int v);
	
	ByteDrop writeUnsignedInt(long v);
	
	ByteDrop writeUnsignedInt(int offset, long v);
	
	ByteDrop writeCompactInt(int v);
	
	ByteDrop writeCompactUnsignedInt(long v);
	
	/**
	 * @param v
	 *            48-bit long to write
	 */
	ByteDrop writeInt48(long v);
	
	ByteDrop writeInt48(int offset, long v);
	
	ByteDrop writeLong(long v);
	
	ByteDrop writeLong(int offset, long v);
	
	ByteDrop writeCompactLong(long v);
	
	ByteDrop writeFloat(float v);
	
	ByteDrop writeFloat(int offset, float v);
	
	ByteDrop writeDouble(double v);
	
	ByteDrop writeDouble(int offset, double v);
	
	ByteDrop writeCompactDouble(double v);
	
	ByteDrop writeBytes(String s);
	
	ByteDrop writeBytes(CharSequence s);
	
	ByteDrop writeBytes(int offset, CharSequence s);
	
	ByteDrop writeChars(String s);
	
	ByteDrop writeChars(CharSequence s);
	
	ByteDrop writeChars(int offset, CharSequence s);
	
	ByteDrop writeUTF(String s);
	
	ByteDrop write(ByteBuffer bb);
	
	ByteDrop append(CharSequence s);
	
	ByteDrop append(CharSequence s, int start, int end);
	
	ByteDrop append(byte[] str);
	
	ByteDrop append(byte[] str, int offset, int len);
	
	ByteDrop append(boolean b);
	
	ByteDrop append(char c);
	
	ByteDrop append(int i);
	
	ByteDrop append(long l);
	
	ByteDrop appendTime(long timeInMS);
	
	ByteDrop append(float f);
	
	ByteDrop append(float f, int precision);
	
	ByteDrop append(double d);
	
	ByteDrop append(double d, int precision);
	
	void readFully(byte[] b);
	
	void readFully(byte[] b, int off, int len);
	
	int skipBytes(int n);
	
	boolean readBoolean();
	
	boolean readBoolean(int offset);
	
	byte readByte();
	
	byte readByte(int offset);
	
	int readUnsignedByte();
	
	int readUnsignedByte(int offset);
	
	short readShort();
	
	short readShort(int offset);
	
	int readUnsignedShort();
	
	int readUnsignedShort(int offset);
	
	short readCompactShort();
	
	int readCompactUnsignedShort();
	
	char readChar();
	
	char readChar(int offset);
	
	/**
	 * @return a 24-bit integer value.
	 */
	int readInt24();
	
	/**
	 * @param offset
	 *            of start.
	 * @return a 24-bit integer value.
	 */
	int readInt24(int offset);
	
	int readInt();
	
	int readInt(int offset);
	
	long readUnsignedInt();
	
	long readUnsignedInt(int offset);
	
	int readCompactInt();
	
	long readCompactUnsignedInt();
	
	long readLong();
	
	long readLong(int offset);
	
	/**
	 * @return read a 48 bit long value.
	 */
	long readInt48();
	
	/**
	 * @return read a 48 bit long value.
	 */
	long readInt48(int offset);
	
	long readCompactLong();
	
	float readFloat();
	
	float readFloat(int offset);
	
	double readDouble();
	
	double readDouble(int offset);
	
	double readCompactDouble();
	
	String readLine();
	
	void readByteString(ByteString as);
	
	int readByteString(int offset, ByteString as);
	
	void readByteString(StringBuilder sb);
	
	int readByteString(int offset, StringBuilder sb);
	
	String readByteString();
	
	void readChars(StringBuilder sb);
	
	String readChars();
	
	String readUTF();
	
	String readUTF(int offset);
	
	void read(ByteBuffer bb);
	
}
