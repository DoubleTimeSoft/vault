package com.doubletimesoft.zion3d.image;

import java.io.DataInputStream;
import java.io.IOException;

class ChunkFactory
{
	static Chunk create( final DataInputStream stream ) throws IOException
	{
		final int length = stream.readInt();
		
		final byte[] fourcc = new byte[4];
		stream.readFully( fourcc );
		final String type  = new String( fourcc );
		
		final byte[] data = new byte[length];
		stream.readFully( data );
		
		final int crc = stream.readInt();
		
		if ( type.equals( "IHDR" ) )
		{
			return new IHDRChunk( type, data, crc );
		}
		else if ( type.equals( "IDAT" ) )
        {
            return new IDATChunk( type, data, crc );
        }
		
		return new Chunk( type, data, crc );
	}
}