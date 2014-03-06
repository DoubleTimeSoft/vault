package com.doubletimesoft.zion3d.image;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.doubletimesoft.zion3d.exception.Zion3DException;

class IHDRChunk extends Chunk
{
	IHDRChunk( final String type, final byte[] data, final int crc ) throws IOException
	{
		super( type, data, crc );
		
		final DataInputStream dataStream =
	        new DataInputStream( new ByteArrayInputStream( data ) );
		
		mWidth = dataStream.readInt();
		mHeight = dataStream.readInt();
		mDepth = dataStream.readByte();
		mImageType = Type.fromValue( dataStream.readByte() );
		
		if ( 0 != dataStream.readByte() )
		{
			throw new Zion3DException( "Compression method must be zero" );
		}
		
        if ( 0 != dataStream.readByte() )
        {
            throw new Zion3DException( "Filter method must be zero" );
        }
        
        if ( 1 == dataStream.readByte() )
        {
            throw new Zion3DException( "Interlacing not supported" );
        }
	}

	final int mWidth;
	final int mHeight;
	final int mDepth;
	final Type mImageType;
}