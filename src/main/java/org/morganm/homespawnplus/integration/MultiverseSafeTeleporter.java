/**
 * 
 */
package org.morganm.homespawnplus.integration;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.morganm.homespawnplus.HomeSpawnPlus;
import org.morganm.homespawnplus.strategy.EventType;
import org.morganm.homespawnplus.strategy.StrategyContext;
import org.morganm.homespawnplus.strategy.StrategyResult;
import org.morganm.homespawnplus.util.Debug;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.SafeTTeleporter;
import com.onarandombox.MultiverseCore.commands.MultiverseCommand;
import com.onarandombox.MultiverseCore.commands.TeleportCommand;
import com.onarandombox.MultiverseCore.enums.TeleportResult;

/**
 * @author morganm
 *
 */
public class MultiverseSafeTeleporter implements SafeTTeleporter {
	private final HomeSpawnPlus hsp;
	private final MultiverseCore multiverse;
	private SafeTTeleporter original;
	
	public MultiverseSafeTeleporter(HomeSpawnPlus hsp, MultiverseCore multiverse) {
		this.hsp = hsp;
		this.multiverse = multiverse;
	}
	
	public void install() {
		original = multiverse.getCore().getSafeTTeleporter();
		multiverse.getCore().setSafeTTeleporter(this);
	
		// turns out we have to hook TeleportCommand since it gets and keeps
		// a reference to safeTeleporter and therefore ignores ours.
//		TeleportCommand tpCmd = 
	}
	
	public void uninstall() {
		multiverse.getCore().setSafeTTeleporter(original);
	}
	
	@Override
	public Location getSafeLocation(Location l) {
		return original.getSafeLocation(l);
	}

	@Override
	public Location getSafeLocation(Location l, int tolerance, int radius) {
		return original.getSafeLocation(l, tolerance, radius);
	}

	/** Process HSP strategies related to a multiverse teleport event.
	 * 
	 * @param teleportee
	 * @param from
	 */
	public Location hspEvent(final Player teleportee, final Location from,
			boolean doTeleport)
	{
		Debug.getInstance().debug("in hspEvent");
		
		Location finalLoc = null;
		
		// be safe to make sure we never interfere with Multiverse teleport
		try {
			final Location newLoc = teleportee.getLocation();
			finalLoc = newLoc;	// default finalLoc is what MV set, unless we change it below
			Location to = teleportee.getLocation();

	    	// cross-world teleport event?
	    	if( from == null || !newLoc.getWorld().equals(from.getWorld()) ) {
				final StrategyContext context = new StrategyContext();
		    	context.setPlayer(teleportee);
		    	context.setSpawnEventType(EventType.MULTIVERSE_TELEPORT_CROSSWORLD);
		    	context.setLocation(to);
				StrategyResult result = hsp.getStrategyEngine().evaluateStrategies(context);
				
				if( result != null && result.isSuccess() ) {
					finalLoc = result.getLocation();
					
					// if HSP strategies gave us a new location, teleport the player there now
					if( !finalLoc.equals(newLoc) ) {
						if( doTeleport )
							hsp.getUtil().teleport(teleportee, result.getLocation(), TeleportCause.PLUGIN, context);
					}
				}
	    	}
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
		
		return finalLoc;
	}
	
	@Override
	public TeleportResult safelyTeleport(CommandSender teleporter,
			Entity teleportee, MVDestination d)
	{
		Location from = null;
		if( teleportee != null )
			from = teleportee.getLocation();
		
		// let Multiverse do it's business
		TeleportResult result = original.safelyTeleport(teleporter, teleportee, d);

		// now give HSP a chance to do something else
		if( teleportee instanceof Player )
			hspEvent((Player) teleportee, from, true);
		
		return result;
	}

	@Override
	public TeleportResult safelyTeleport(CommandSender teleporter,
			Entity teleportee, Location location, boolean safely)
	{
		Location from = null;
		if( teleportee != null )
			from = teleportee.getLocation();
		
		// let Multiverse do it's business
		TeleportResult result = original.safelyTeleport(teleporter, teleportee, location, safely);

		// now give HSP a chance to do something else
		if( teleportee instanceof Player )
			hspEvent((Player) teleportee, from, true);
		
		return result;
	}

	@Override
	public Location getSafeLocation(Entity e, MVDestination d) {
		return original.getSafeLocation(e, d);
	}

	@Override
	public Location findPortalBlockNextTo(Location l) {
		return original.findPortalBlockNextTo(l);
	}

}