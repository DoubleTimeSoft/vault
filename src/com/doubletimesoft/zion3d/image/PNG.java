package com.doubletimesoft.zion3d.image;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.doubletimesoft.zion3d.exception.Zion3DException;

public class PNG
{
	public void load( final InputStream stream )
	{
		try
		{
			decode( parse( stream ) );
		}
		catch ( final IOException e )
		{
			throw new Zion3DException( e );
		}
        catch ( final DataFormatException e )
        {
            throw new Zion3DException( e );
        }
	}

	public void flipVertical()
	{
		final byte[] buffer = new byte[getScanlineWidthInBytes()];
		
		int maxy = mHeight >>> 1;
		
		if ( 1 == ( mHeight & 1 ) )
		{
			++maxy;
		}
		
		for ( int y = 0; y < maxy; ++y )
		{
			final int topOffset = y * buffer.length;
			final int bottomOffset = (mHeight - y - 1) * buffer.length;
			
			// copy top scanline to buffer
			System.arraycopy( mImage, topOffset, buffer, 0, buffer.length );
			
			// copy bottom scanline to top
			System.arraycopy( mImage, bottomOffset, mImage, topOffset, buffer.length );
			
			// copy buffered top scanline to bottom
			System.arraycopy( buffer, 0, mImage, bottomOffset, buffer.length );
		}
	}
	
	public PNG getSubimage( final int x, final int y, final int width, final int height )
	{
	    final PNG png = new PNG();
	    
	    png.mWidth = width;
	    png.mHeight = height;
	    png.mDepth = mDepth;
	    png.mNumComponents = mNumComponents;
	    png.mType = mType;
	    
        final int srcScanlineWidthInBytes = getScanlineWidthInBytes();
        final int destScanlineWidthInBytes = png.getScanlineWidthInBytes();
	    png.mImage = new byte[png.mHeight * destScanlineWidthInBytes];
	    
	    int srcOffset = y * srcScanlineWidthInBytes + x * mNumComponents * (mDepth / 8);
	    int destOffset = 0;
	    
	    for ( int i = 0; i < height; ++i )
	    {
	        System.arraycopy( mImage, srcOffset, png.mImage, destOffset, destScanlineWidthInBytes );
	        srcOffset += srcScanlineWidthInBytes;
	        destOffset += destScanlineWidthInBytes;
	    }
	    
	    return png;
	}
	
	public int getWidth()
	{
		return mWidth;
	}
	
	public int getHeight()
	{
		return mHeight;
	}
	
	public int getDepth()
	{
		return mDepth;
	}
	
	public Type getType()
	{
		return mType;
	}
	
	public byte[] getImage()
	{
	    return mImage;
	}
	
	public int getNumComponents()
	{
		return mNumComponents;
	}
	
	public int getScanlineWidthInBytes()
	{
		return mWidth * mNumComponents * (mDepth / 8);
	}
	
	@Override
	public String toString()
	{
		return mWidth + "x" + mHeight + ":" + mDepth + " " + mType;
	}

    private byte[] parse( final InputStream stream ) throws IOException
    {
        final DataInputStream dataStream = new DataInputStream( stream );
        
        readSignature( dataStream );
        
        Chunk chunk = null;
        byte[] compressedData = new byte[0];
        
        while ( !( chunk = ChunkFactory.create( dataStream ) ).mType.equals( "IEND" ) )
        {
        	if ( chunk.isCritical() )
        	{
        		if ( chunk instanceof IHDRChunk )
        		{
        			final IHDRChunk ihdrChunk = (IHDRChunk)chunk;
        			mWidth = ihdrChunk.mWidth;
        			mHeight = ihdrChunk.mHeight;
        			mDepth = ihdrChunk.mDepth;
        			mType = ihdrChunk.mImageType;
        			setNumComponents();
        		}
        		else if ( chunk instanceof IDATChunk )
        		{
        		    compressedData = grow( compressedData, chunk.mData );
        		}
        	}
        }
        
        return compressedData;
    }

    private void decode( final byte[] compressedData ) throws DataFormatException
    {
        final Inflater inflater = new Inflater();
        inflater.setInput( compressedData );
        final byte[] scanlineBuffer = new byte[getScanlineWidthInBytes() + 1];
        int[] filteredScanline = new int[scanlineBuffer.length];
        int[] previousFilteredScanline = new int[scanlineBuffer.length];
        mImage = new byte[mHeight * getScanlineWidthInBytes()];
        int imageOffset = 0;
        
        while ( !inflater.finished() )
        {
            inflater.inflate( scanlineBuffer );
            
            toIntArray( filteredScanline, scanlineBuffer );
            
            reconstruct( previousFilteredScanline, filteredScanline, imageOffset );
            
            imageOffset += filteredScanline.length - 1;
            
            final int[] tmp = previousFilteredScanline;
            previousFilteredScanline = filteredScanline;
            filteredScanline = tmp;
        }
    }
    
	private static void toIntArray( final int[] filteredScanline, final byte[] scanlineBuffer )
	{
		filteredScanline[0] = scanlineBuffer[0];
		for ( int i = 1; i < scanlineBuffer.length; ++i )
		{
			filteredScanline[i] = ( scanlineBuffer[i] + 256 ) & 255;
		}
	}

	private void reconstruct( final int[] previousFilteredScanline,
	                          final int[] filteredScanline, 
	                          int imageOffset )
	{
	    switch ( filteredScanline[0] )
	    {
		    case 0:
		    {
    	        for ( int i = 1; i < filteredScanline.length; ++i )
    	        {
    	            filteredScanline[i] = filterNone( filteredScanline[i] );
    	        }
    	        break;		    	
		    }
		    
    	    case 1:
    	    {
    	        for ( int i = 1; i < filteredScanline.length; ++i )
    	        {
    	        	if ( i - mNumComponents < 1 )
    	        	{
    	        		filteredScanline[i] = filterLeft( filteredScanline[i], 0 );
    	        	}
    	        	else
    	        	{
    	        		filteredScanline[i] = filterLeft( filteredScanline[i],
    	            		                          	  filteredScanline[i-mNumComponents] );
    	        	}
    	        }
    	        break;
    	    }
    	    
            case 2:
            {
                for ( int i = 1; i < filteredScanline.length; ++i )
                {
                    filteredScanline[i] = filterUp( filteredScanline[i],
                    		                        previousFilteredScanline[i] );
                }
                break;
            }
            
            case 3:
            {
                for ( int i = 1; i < filteredScanline.length; ++i )
                {
    	        	if ( i - mNumComponents < 1 )
    	        	{
    	        		filteredScanline[i] = filterAverage( filteredScanline[i],
                                                             0,
                                                             previousFilteredScanline[i] );    	        		
    	        	}
    	        	else
    	        	{
    	        		filteredScanline[i] = filterAverage( filteredScanline[i],
    	        				                             filteredScanline[i-mNumComponents],
                    		                                 previousFilteredScanline[i] );
    	        	}
                }
                break;
            }
            
            case 4:
            {
                for ( int i = 1; i < filteredScanline.length; ++i )
                {
    	        	if ( i - mNumComponents < 1 )
    	        	{
                        filteredScanline[i] = filterPaeth( filteredScanline[i],
	                                                       0,
	                                                       previousFilteredScanline[i],
	                                                       0 );    	        		
    	        	}
    	        	else
    	        	{
                        filteredScanline[i] = filterPaeth( filteredScanline[i],
                    	   	                               filteredScanline[i-mNumComponents],
                    		                               previousFilteredScanline[i],
                    		                               previousFilteredScanline[i-mNumComponents] );
    	        	}
                }
                break;
            }
	    }
	    
	    for ( int i = 1; i < filteredScanline.length; ++i )
	    {
	    	mImage[imageOffset++] = (byte)filteredScanline[i];
	    }
	}
	
	private static int filterNone( final int x )
	{
		return x;
	}
	
	private static int filterLeft( final int x, final int a )
	{
		return (x + a) & 255;
	}

	private static int filterUp( final int x, final int b )
	{
		return (x + b) & 255;
	}
	
	private static int filterAverage( final int x, final int a, final int b )
	{
		return (x + ( ( a + b ) >>> 1 ) ) & 255;
	}
	
	private static int filterPaeth( final int x, final int a, final int b, final int c )
	{
		return ( x + paethPredictor( a, b, c ) ) & 255;
	}
	
	private static int paethPredictor( final int a, final int b, final int c )
	{
	    final int p = a + b - c;
	    final int pa = Math.abs( p - a );
	    final int pb = Math.abs( p - b );
	    final int pc = Math.abs( p - c );
	    int pr;
	    
        if ( pa <= pb && pa <= pc )
        {
            pr = a;
        }
        else if ( pb <= pc )
        {
            pr = b;
        }
        else 
        {
            pr = c;
        }
        
        return pr;
	}
	
	private static byte[] grow( byte[] original, final byte[] growth )
	{
	    final int offset = original.length;
	    original = Arrays.copyOf( original, original.length + growth.length );
	    System.arraycopy( growth, 0, original, offset, growth.length );
	    return original;
	}
	
	private static void readSignature( final DataInputStream stream ) throws IOException
	{
		byte[] signature = new byte[8];			
		stream.readFully( signature );
		
		if ( ! Arrays.equals( signature, SIGNATURE ) )
		{
			throw new Zion3DException( "Not a PNG" );
		}
	}
	
	private void setNumComponents()
	{
	    switch ( mType )
	    {
    	    case GREYSCALE:
    	    {
    	    	mNumComponents = 1;
    	    	break;
    	    }
    	    
    	    case GREYSCALE_ALPHA:
    	    {
    	    	mNumComponents = 2;
    	    	break;
    	    }
    	    
    	    case TRUECOLOR:
    	    {
    	    	mNumComponents = 3;
    	    	break;
    	    }
    	    
    	    case TRUECOLOR_ALPHA:
    	    {
    	    	mNumComponents = 4;
    	    	break;
    	    }

    	    case INDEXED:    	    
    	    default:
    	    {
    		    throw new Zion3DException( "Unknown image component count" );
    	    }
	    }
	}
		
	private static final byte[] SIGNATURE = { (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A };
	private int mWidth;
	private int mHeight;
	private int mDepth;
	private int mNumComponents;
	private Type mType;
	private byte[] mImage;
}