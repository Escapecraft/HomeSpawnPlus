/**
 * 
 */
package org.morganm.homespawnplus.util;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/** General purpose teleportation routines.
 * 
 * @author morganm
 *
 */
public class Teleport {
	// class version: 1
	
	public final int FLAG_NO_WATER = 0x01; 
	
	/*
	private final static BlockFace[] directions = new BlockFace[] {
		BlockFace.UP,
		BlockFace.NORTH,
		BlockFace.WEST,
		BlockFace.SOUTH,
		BlockFace.EAST,
		BlockFace.DOWN
	};
	*/
	private static Teleport instance;
	
	private Logger log = Logger.getLogger(Teleport.class.toString());
	private String logPrefix = "";
	private final Debug debug = Debug.getInstance();
	private Teleport() {
	}
	
	public static Teleport getInstance() {
		if( instance == null )
			instance = new Teleport();
		return instance;
	}
	
	public void setLogger(Logger log) {
		this.log = log;
	}
	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix;
	}
	
	private boolean isSafeBlock(Block b, int flags) {
		final Block up = b.getRelative(BlockFace.UP);
		final Block down = b.getRelative(BlockFace.DOWN);
		final int bTypeId = b.getTypeId();
		final int upTypeId = up.getTypeId();
		final int downTypeId = down.getTypeId();
		if( bTypeId == 0 && upTypeId == 0
				&& (downTypeId != 10 && down.getTypeId() != 11)	// no lava underneath
				&& (downTypeId != 0)								// no air underneath
				&& (bTypeId != Material.FIRE.getId())				// not fire block
				&& (upTypeId != Material.FIRE.getId())			// not fire above
		){
			// if flags don't allow water, check that too
			if( (flags & FLAG_NO_WATER) > 0 ) {
				if( bTypeId == Material.WATER.getId()
						|| bTypeId == Material.STATIONARY_WATER.getId()
						|| downTypeId == Material.WATER.getId()
						|| downTypeId == Material.STATIONARY_WATER.getId() )
				{
					return false;
				}
			}
			
			// if we make it here, we've made it through all hazardous checks
			// so the block is considered safe to teleport to
			return true;
		}
		else
			return false;
		
	}
	
	private Location findSafeLocation2(final Location baseLocation, final int level,
			final Bounds bounds, final int flags)
	{
		debug.devDebug("findSafeLocation2(): level=",level,", baseLocation=",baseLocation,", flags=",flags);
		final World w = baseLocation.getWorld();
		final int baseX = baseLocation.getBlockX();
		final int baseY = baseLocation.getBlockY();
		final int baseZ = baseLocation.getBlockZ();
		
		int minX = baseX - level;
		int maxX = baseX + level;
		int minY = baseY - level;
		int maxY = baseY + level;
		int minZ = baseZ - level;
		int maxZ = baseZ + level;
		
		if( minY < bounds.minY )
			minY = bounds.minY;
		if( maxY > bounds.maxY )
			maxY = bounds.maxY;
		
		debug.devDebug("findSafeLocation2(): bounds.maxY=",bounds.maxY,", bounds.minY=",bounds.minY);
		debug.devDebug("findSafeLocation2(): maxY=",maxY,", minY=",minY);
		
		long startTime = System.currentTimeMillis();
		int checkedBlocks=0;
		for(int x = maxX; x >= minX; x--) {
			for(int y=maxY; y >= minY; y--) {
				for(int z=maxZ; z >= minZ; z--) {
					// we only check the level that we're at, at least one
					// of the axis must be at the current level
					if( x != maxX && x != minX
							&& y != maxY && y != minY 
							&& z != maxZ && z != minZ )
						continue;
					
					Block b = w.getBlockAt(x, y, z);
					if( isSafeBlock(b, flags) ) {
						debug.devDebug("findSafeLocation2(): found safe block ",b);
						return b.getLocation();
					}
					checkedBlocks++;
				}
			}
		}
		
		long totalTime = System.currentTimeMillis() - startTime;
		debug.devDebug("findSafeLocation2(): no safe location found at level ",level,", checked ",checkedBlocks," total blocks. Recursing to next level. (total time = ",totalTime,")");
		
		// we only recurse so far before we give up
		if( level+1 > bounds.maxRange ) {
			// check the highest Block at the given X/Z; if it's higher than the maxY
			// we've checked, then try there. This has the effect that if we try to
			// spawn deep into solid blocks and can't find a safe place, we'll spawn
			// at the top Y, much like the vanilla MC algorithm.
			// also note we check if it's lower than minY, because it's possible we
			// originally received a teleport request high up in the middle of the
			// sky, so we'll want to try again at the highest block level.
			// the mixY/minY checks prevent this from being infinitely recursive,
			// since this can only be true once.
	    	Location highest = w.getHighestBlockAt(baseLocation).getLocation();
	    	if( highest.getY() > maxY || highest.getY() < minY ) {
				debug.devDebug("findSafeLocation2(): hit maximum recursion distance ",bounds.maxRange,", moving to highest Y-block at ",highest.getY()," and trying again");
				return findSafeLocation2(highest, 0, bounds, flags);
	    	}
			debug.devDebug("findSafeLocation2(): hit maximum recursion distance ",bounds.maxRange,", returning null");
			return null;
		}
		
		return findSafeLocation2(baseLocation, level+1, bounds, flags);
	}
	
	/** Recursively look for 2 vertical safe air spots nearest the given location.
	 * 
	 * @param base
	 */
	/*
	private Location findSafeLocation(final Set<Location> alreadyTraversed, final int level, final Location location) {
		debug.devDebug("findSafeLocation(): level=",level,", location=",location);

		// okTraverse tracks block that we check at this level that it's OK
		// to traverse into for this pass.
		ArrayList<Location> okTraverse = new ArrayList<Location>(3);
		
		if( location == null )
			return null;
		final Block base = location.getBlock();

		final boolean blockAlreadyTraversed = alreadyTraversed.contains(location);
		debug.devDebug("findSafeLocation(): blockAlreadyTraversed=",blockAlreadyTraversed);
		
		if( !blockAlreadyTraversed && isSafeBlock(base, 0) ) {
			debug.devDebug("findSafeLocation(): isSafeBlock(location)=true, returning");
			return location;
		}
		
		// first try all the closest blocks before recursing further
		for(int i=0; i < directions.length; i++) {
			Block tryBlock = base.getRelative(directions[i]);
			Location tryLocation = tryBlock.getLocation();
			if( alreadyTraversed.contains(tryLocation) ) {
				continue;
			}
			okTraverse.add(tryLocation);
			alreadyTraversed.add(tryLocation);

			if( isSafeBlock(tryBlock, 0) ) {
				debug.devDebug("findSafeLocation(): found safeBlock at ",tryBlock);
				return tryLocation;
			}
		}

		// we only recurse so far before we give up
		if( level > 10 ) {
			debug.devDebug("findSafeLocation(): hit maximum recursion, returning null");
			return null;
		}

		// if we're here, none of them were safe, now recurse
		for(int i=0; i < directions.length; i++) {
			Location recurseLocation = base.getRelative(directions[i]).getLocation();
			if( alreadyTraversed.contains(recurseLocation) && !okTraverse.contains(recurseLocation) )
				continue;
			alreadyTraversed.add(recurseLocation);

			Location result = findSafeLocation(alreadyTraversed, level+1, recurseLocation);
			if( result != null ) {
				debug.devDebug("findSafeLocation(): found safe location through recursion at ",result);
				return result;
			}
		}
		
		return null;
	}
	*/
	
	/** Safely teleport a player to a location. Should avoid them being stuck in blocks,
	 * teleported over lava, etc.
	 * 
	 * @param p
	 * @param l
	 */
	public void safeTeleport(final Player p, final Location l, final TeleportCause cause) {
		p.teleport(safeLocation(l), cause);
	}
	
	/** Given a location, find the nearest "safe" location, ie. that won't suffocate a
	 * player, spawn them over lava, etc.
	 * 
	 * @param l the location to start searching from
	 * @param minY the minimum Y distance to check
	 */
	public Location safeLocation(Location l, Bounds bounds, int flags) {
		if( bounds == null )
			bounds = defaultBounds;
		
		Location target = findSafeLocation2(l, 0, bounds, flags);
		
		// if we didn't find a safe location, then just use the original location
		if( target == null ) {
			log.info(logPrefix+" safeLocation: couldn't find nearby safe location, using original location "+l);
			target = l;
		}
		else {
			// preserve pitch/yaw
			target.setPitch(l.getPitch());
			target.setYaw(l.getYaw());

			// adjust by 0.5 so we teleport to the middle of the block, not
			// the edge
			target.setX(target.getX()+0.5);
			target.setZ(target.getZ()+0.5);
			debug.devDebug("adjusted coordinates to middle. x=",target.getX(),", z=",target.getZ());
		}
		
		debug.debug("safeLocation(): target=",target);
		return target;
	}
	
	/** Given a location, find the nearest "safe" location, ie. that won't suffocate a
	 * player, spawn them over lava, etc.
	 * 
	 * @param l
	 */
	public Location safeLocation(Location l) {
		return safeLocation(l, defaultBounds, 0);
	}
	
	private final Bounds defaultBounds = new Bounds();
	public static class Bounds {
		// min/max Y bounds, can be used to prevent safe location from being
		// below or above a certain Y bound
		public int minY=0;
		public int maxY=255;
		
		// the range we check from the baseLocation. Checks get exponentially larger
		// as the volume of the cube grows, so don't go too large (max probably
		// 25 or so for reasonable performance).
		public int maxRange=10;
		
		public String toString() {
			return "minY="+minY
				+", maxY="+maxY
				+", maxRange="+maxRange;
		}
	}
}
