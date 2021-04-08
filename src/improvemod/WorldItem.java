package improvemod;

import com.wurmonline.client.renderer.PickableUnit;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.wurmonline.client.game.inventory.InventoryMetaItem;

public class WorldItem 
{
	Pattern mFltPattern = Pattern.compile( "([0-9]+[.][0-9]+)" );
	long mItemId;
	String mNextTool;
	boolean mDamaged;
	
	public WorldItem( PickableUnit pItem ) 
	{
		this.mItemId = pItem.getId();
		this.mNextTool = null;
		this.mDamaged = false;
	}
	
	public String getNextTool() 
	{
		return this.mNextTool;
	}
	
	public boolean equals(PickableUnit pItem) 
	{
		return ( pItem.getId() == this.mItemId );
	}
	
	public boolean getDamaged() 
	{
		return this.mDamaged;
	}
	
	public void setDamaged( boolean pValue ) 
	{
		this.mDamaged = pValue;
	}
	
	public boolean validTool( InventoryMetaItem pTool ) 
	{
		if( this.mNextTool == null ) return false;
		return pTool.getBaseName().contains( this.mNextTool );
	}
	
	public void parseMessage( String pMessage ) 
	{
		// Parse improving messages and update item state accordingly
		if( pMessage.contains( "damage the" ) ) 
		{
			this.mDamaged = true;
		}
		else if( pMessage.contains( "before you try to improve" ) )
		{
			this.mDamaged = true;
		}
		else if( pMessage.contains( "with a log" ) || pMessage.contains( "more log" ) )
		{
    		this.mNextTool = "log";
		}
		else if( pMessage.contains( "with a rock shards" ) || pMessage.contains( "more rock shards" ) )
		{
			this.mNextTool = "rock shards";
		}
		else if( pMessage.contains( "with a string" ) || pMessage.contains( "more string" ) )
		{
			this.mNextTool = "string";
    	}
		else if( pMessage.contains( "use a mallet" ) )
		{
    		this.mNextTool = "mallet";
    	}
    	else if( pMessage.contains( "use a file" ) )
    	{
    		this.mNextTool = "file";
    	}
    	else if( pMessage.contains( "want to polish" ) )
    	{
    		this.mNextTool = "pelt";
    	}
    	else if( pMessage.contains( "carve away" ) )
    	{
    		this.mNextTool = "carving knife";
    	}
    	else if( pMessage.contains( "with a stone chisel" ) ) 
    	{
    		this.mNextTool = "stone chisel";
    	}
    	else if( pMessage.contains( "some stains" ) ) 
    	{
    		this.mNextTool = "water";
    	}
    	else if( pMessage.contains( "must be backstitched" ) || pMessage.contains( "by slipstitching" ) )
    	{
    		this.mNextTool = "needle";
    	}
    	else if( pMessage.contains( "be cut away" ) )
    	{
    		this.mNextTool = "scissors";
    	}
		
		// Parse damage value of item descriptions
		String[] lMsg_parts = pMessage.split( ", Dam: " );
		if( lMsg_parts.length > 1) 
		{
			Matcher lMatch = mFltPattern.matcher( lMsg_parts[1] );
			if( lMatch.find()) this.mDamaged = (Float.parseFloat( lMatch.group() ) > 0 );
		}
	}
}