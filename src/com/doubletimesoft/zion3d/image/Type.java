package com.doubletimesoft.zion3d.image;

public enum Type
{
	GREYSCALE( 0 ), // Each pixel is a greyscale sample
	TRUECOLOR( 2 ), // Each pixel is an R,G,B triple
	INDEXED( 3 ), // Each pixel is a palette index; a PLTE chunk shall appear.
	GREYSCALE_ALPHA( 4 ), // Each pixel is a greyscale sample followed by an alpha sample
	TRUECOLOR_ALPHA( 6 ); // Each pixel is an R,G,B triple followed by an alpha sample.
	
	private Type( final int value )
	{
		mValue = value;
	}
	
	int value()
	{
		return mValue;
	}
	
	static Type fromValue( final int value )
	{
		for ( final Type type : values() )
		{
			if ( value == type.mValue )
			{
				return type;
			}
		}
		
		return null;
	}
	
	private final int mValue;
}