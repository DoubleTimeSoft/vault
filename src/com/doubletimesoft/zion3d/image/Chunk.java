package com.doubletimesoft.zion3d.image;


class Chunk
{
	Chunk( final String type,
           final byte[] data,
           final int crc )
	{
		mType  = type;
		mData = data;
		mCRC = crc;
	}
	
	boolean isCritical()
	{
		return Character.isUpperCase( mType.charAt( 0 ) );
	}
	
	final String mType;
	final byte[] mData;
	final int mCRC;
}