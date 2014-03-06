package com.doubletimesoft.zion3d.image;


class IDATChunk extends Chunk
{
    IDATChunk( final String type, final byte[] data, final int crc )
    {
        super( type, data, crc );
    }
}