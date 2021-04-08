package improvemod;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;

import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.ToolBelt;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.game.World;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.inventory.InventoryMetaWindowManager;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.shared.constants.PlayerAction;

public class ImproveMod implements WurmClientMod, Initable, PreInitable 
{
	public static Logger LOGGER = Logger.getLogger( "ImproveMod" );
	public static ArrayList<WorldItem> mWorldItems = new ArrayList<WorldItem>();
	public static WorldItem mSelectedWorldItem = null;
    public static HeadsUpDisplay mHud;
    public static World mWorld;
    public static int mQueueSlots = 3;
    
    public static boolean handleInput(final String pCommand, final String[] pData) 
    {
    	// Check the command is an i2improve request
    	if( !pCommand.equalsIgnoreCase( "improveitems" ) ) return false;
    	
    	// Fetch world and client
    	mWorld = mHud.getWorld();
    	WurmClientBase lClient = mWorld.getClient();
        if ( mWorld.getClient().isMouseUnavailable()) return true;
        
        // Apply i2improve to inventory items
        long[] lIds = mHud.getCommandTargetsFrom( lClient.getXMouse(), lClient.getYMouse() );
        if( lIds != null && lIds.length > 0 ) 
        {
        	ImproveMod.improveInventoryItems( lIds );
        	return true;
        }
    	
        // Apply i2improve to world items
        PickableUnit lHovered = mWorld.getCurrentHoveredObject();
        if( lHovered != null ) 
        {
        	ImproveMod.improveWorldItem( lHovered );
        	return true;
        }
		return true;
    }
    
    public static void handleMessage( String pContext, String pMessage ) 
    {
    	if( !pContext.equalsIgnoreCase( ":event" ) ) return;
    	
    	// If looking at item description, try to capture world item
    	if( pMessage.contains( "the signature of its maker" ) ) 
    	{
    		PickableUnit lHovered = mHud.getWorld().getCurrentHoveredObject();
    		if( lHovered != null )
			{
    			mSelectedWorldItem = ImproveMod.getWorldItem( lHovered );
			}
    	}
    	
    	if( mSelectedWorldItem != null ) 
    	{
    		mSelectedWorldItem.parseMessage( pMessage );
    	}
    }
    
    public static void improveInventoryItems(long[] pTargetIds) 
    {
    	// Set max queue
        float lML = mWorld.getPlayer().getSkillSet().getSkill( "Mind Logic" ).getValue();
        if ( lML >= 40 )
        {
        	mQueueSlots = (int)lML/ 10 + 1;
        }
    	
    	// Get objects given target IDs
    	if( pTargetIds.length == 0 ) return;
    	InventoryMetaItem[] lTargets =  ImproveMod.getObjectsFromIDs( pTargetIds );
    		
    	// Get object with lowest quality
    	float lMinQuality = 1000;
    	InventoryMetaItem lTarget = null;
    	
    	List<InventoryMetaItem> lList = new ArrayList<InventoryMetaItem>();
    	for( InventoryMetaItem lTgt : lTargets ) 
    	{
    		if( lTgt != null ) 
    		{
    			lList.add( lTgt );
    		}
    	}

    	lList.sort( Comparator.comparing(InventoryMetaItem::getQuality) );
//    	lList.sort( Comparator.comparing(InventoryMetaItem::getQuality).reversed() );
    	
    	int lActionsSent = 0;
    	for ( int j = 0; j < lList.size(); j ++ )
    	{
    		if ( lActionsSent >= mQueueSlots )
    		{
    			return;
    		}
    		
    		// Repair target if damaged
    		InventoryMetaItem lItem = lList.get( j );
    		if ( ( lItem.getDamage() > 0 ) && ( lActionsSent < mQueueSlots ) )
    		{
    			mHud.sendAction( PlayerAction.REPAIR, lItem.getId() );
    			lActionsSent ++;
    		}
    		
    		// Look for valid tool on tool belt
    		ToolBelt lBelt = mHud.getToolBelt();
    		for( int i = 0; i < lBelt.getSlotCount(); i++ ) 
    		{
        		// Get tool from tool belt
        		InventoryMetaItem lTool = lBelt.getItemInSlot( i );
        		if( lTool == null ) continue;
        		
        		if( lTool.getType() == lItem.getImproveIconId() ) 
        		{
        			// Repair tool if damaged
        			if ( ( ( lTool.getDamage() > 1F ) && ( lActionsSent < mQueueSlots ) ) && (  !( lTool.getImproveIconId() == 602 ) || !( lTool.getImproveIconId() == 610 ) || !( lTool.getImproveIconId() == 606 ) || !( lTool.getImproveIconId() == 620 ) || !( lTool.getImproveIconId() == 640 ) ) )
        			{
        				mHud.sendAction( PlayerAction.REPAIR, lTool.getId() );
            			lActionsSent ++;
        			}
        			// Improve Item
        			if ( lActionsSent < mQueueSlots )
        			{
        				long[] lIds = { lItem.getId() };
        				mHud.getWorld().getServerConnection().sendAction( lTool.getId(), lIds, PlayerAction.IMPROVE);
        				lActionsSent ++;
        				i = lBelt.getSlotCount();
        			}
        		}
        	}
    	}
    }
    
    public static void improveWorldItem( PickableUnit pItem ) 
    {
    	// Find corresponding world item
    	mSelectedWorldItem = ImproveMod.getWorldItem( pItem );
    	
    	// If item damaged, first repair
    	long[] lIds = { pItem.getId() };
    	if( mSelectedWorldItem.getDamaged() ) 
    	{
    		mHud.sendAction(PlayerAction.REPAIR, pItem.getId() );
    		mSelectedWorldItem.setDamaged( false );
    	}
    	
    	// Look for valid tool on tool belt
    	ToolBelt lBelt = mHud.getToolBelt();
    	for( int i = 0; i < lBelt.getSlotCount(); i ++ ) 
    	{
    		// Get tool from tool belt
    		InventoryMetaItem lTool = lBelt.getItemInSlot( i );
    		if( lTool == null ) continue;
    		
    		// If tool matches required improve item
    		if( mSelectedWorldItem.validTool( lTool ) ) 
    		{
    			mHud.getWorld().getServerConnection().sendAction( lTool.getId(), lIds, PlayerAction.IMPROVE );
    			return;
    		}
    	}
    	
    	// If no tool match, use active item
    	mHud.sendAction(PlayerAction.IMPROVE, lIds );
    }
    
    public static long[] getTargetIDs() 
    {
    	World lWorld = mHud.getWorld();
    	WurmClientBase lClient = lWorld.getClient();
    	
    	// If mouse not available, return null
        if ( lWorld.getClient().isMouseUnavailable() ) return null;
        
        // Get targets from mouse coordinates
        PickableUnit lHovered = lWorld.getCurrentHoveredObject();
        long[] lIds = mHud.getCommandTargetsFrom( lClient.getXMouse(), lClient.getYMouse() );
        if ( lIds == null && lHovered != null ) lIds = new long[] { lHovered.getId() };
        return lIds;
    }
    
    public static InventoryMetaItem[] getObjectsFromIDs( long[] pIds ) 
    {
    	InventoryMetaWindowManager lManager = mHud.getWorld().getInventoryManager();
    	InventoryMetaItem[] lRet = new InventoryMetaItem[pIds.length];
    	
    	// List inventories, starting with player inventory
    	ArrayList<InventoryMetaWindowView> lInventories = new ArrayList<>();
    	lInventories.add( lManager.getPlayerInventory() );
		
		// Add other inventories to list
    	try 
    	{
    		Map<Long, InventoryMetaWindowView> lExtraInvs = ReflectionUtil.getPrivateField( lManager, ReflectionUtil.getField( InventoryMetaWindowManager.class, "inventoryWindows" ) );
    		lInventories.addAll( new ArrayList<>( lExtraInvs.values() ) );
    	}
    	catch( Exception ex ) 
    	{
    		mHud.consoleOutput( "ImproveMod: Error accessing extra inventory windows." );
    	}
    	
    	// Look for items in inventories
    	for(InventoryMetaWindowView lInventory: lInventories) 
    	{
    		for(int i = 0; i < pIds.length; i ++) 
    		{
        		InventoryMetaItem lItem = lInventory.getItem( pIds[i] );
        		if( lItem != null) lRet[i] = lItem;
        	}
    	}
    	
    	// Return list of items
    	return lRet;
    }
    
    public static WorldItem getWorldItem( PickableUnit pItem ) 
    {
    	// Find corresponding world item
    	WorldItem lItem = null;
    	for( WorldItem tItem: mWorldItems ) 
    	{
    		if( tItem.equals( pItem ) ) 
    		{
    			lItem = tItem;
    			break;
    		}
    	}
    	
    	// If corresponding world item not found, create new one
    	if( lItem == null ) 
    	{ 
    		lItem = new WorldItem( pItem );
    		mWorldItems.add( lItem );
    	}
    	
    	return lItem;
    }
    
    @Override
    public void preInit() 
    {

    }
    
    @Override
    public void init() 
    {
        // Inject console handler
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();
            
            // Add console command handler
	        CtClass ctWurmConsole = classPool.getCtClass( "com.wurmonline.client.console.WurmConsole" );
	        ctWurmConsole.getMethod( "handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z" ).insertBefore(
	                "if (improvemod.ImproveMod.handleInput($1,$2)) return true;"
	        );
	        
	        // Add chat messages handler
	        CtClass ctWurmChat = classPool.getCtClass( "com.wurmonline.client.renderer.gui.ChatPanelComponent" );
	        ctWurmChat.getMethod( "addText", "(Ljava/lang/String;Ljava/lang/String;FFFZ)V" ).insertBefore(
	                "improvemod.ImproveMod.handleMessage($1,$2);"
	        );
        }
        catch(Throwable e)
        {
        	LOGGER.log( Level.SEVERE, "Error loading ImproveMod", e.getMessage() );
        }
        
        // Hook HUD init to setup our stuff
        HookManager.getInstance().registerHook( "com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> (proxy, method, args) -> {
			method.invoke(proxy, args);
			mHud = (HeadsUpDisplay) proxy;
			return null;
		});
    }
}