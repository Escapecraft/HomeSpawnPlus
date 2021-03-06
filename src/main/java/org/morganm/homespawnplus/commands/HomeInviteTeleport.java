/*******************************************************************************
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (c) 2012 Mark Morgan.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * Contributors:
 *     Mark Morgan - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.morganm.homespawnplus.commands;

import java.util.Date;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.morganm.homespawnplus.HomeSpawnPlus;
import org.morganm.homespawnplus.command.BaseCommand;
import org.morganm.homespawnplus.config.ConfigOptions;
import org.morganm.homespawnplus.entity.Home;
import org.morganm.homespawnplus.i18n.HSPMessages;
import org.morganm.homespawnplus.manager.WarmupRunner;
import org.morganm.homespawnplus.storage.StorageException;

/** Cooldown, warmup and teleport logic structured similar to Home command.
 * 
 * @author morganm
 *
 */
public class HomeInviteTeleport extends BaseCommand {
	private static final String OTHER_WORLD_PERMISSION = HomeSpawnPlus.BASE_PERMISSION_NODE + ".command.homeinvitetp.otherworld";

	@Override
	public String[] getCommandAliases() { return new String[] {"hit", "hitp", "homeinvitetp"}; }
	
	@Override
	public String getUsage() {
		return	util.getLocalizedMessage(HSPMessages.CMD_HOME_INVITE_TELEPORT_USAGE);
	}

	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.command.Command#execute(org.bukkit.entity.Player, org.bukkit.command.Command, java.lang.String[])
	 */
	@Override
	public boolean execute(final Player p, Command command, String[] args) {
		if( !isEnabled() || !hasPermission(p) )
			return true;
		
		if( args.length < 1 ) {
			return false;
		}
		
		Location l = null;
		org.morganm.homespawnplus.entity.HomeInvite homeInvite = null;
		Home theHome = null;
		
		if( args.length == 1 ) {
			try {
				int id = Integer.parseInt(args[0]);
				homeInvite = plugin.getStorage().getHomeInviteDAO().findHomeInviteById(id);
			}
			catch(NumberFormatException e) {
				util.sendMessage(p, "Error: Expected id number, got \""+args[0]+"\"");
				return false;			// send command usage
			}
		}
		else if( args.length == 2 ) {
			// find the player
			Player targetPlayer = Bukkit.getPlayer(args[0]);
			OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(args[0]);
			String targetPlayerName = null;
			if( targetPlayer != null )
				targetPlayerName = targetPlayer.getName();
			else if( targetOfflinePlayer != null )
				targetPlayerName = targetOfflinePlayer.getName();
			else
				util.sendMessage(p, "Could not find player \""+args[0]+"\"");
			
			if( targetPlayerName != null ) {
				// now find the home with the name given for that player
				org.morganm.homespawnplus.entity.Home home = plugin.getStorage().getHomeDAO().findHomeByNameAndPlayer(args[1], targetPlayerName);
				
				// now look for the HomeInvite for that home with this player as the invitee
				homeInvite = plugin.getStorage().getHomeInviteDAO().findInviteByHomeAndInvitee(home, p.getName());
			}
		}
		else {
			return false;		// send command usage
		}
		
		if( homeInvite != null ) {
			// store home for future use in player messages
			theHome = homeInvite.getHome();
			
			if( theHome == null
					|| (theHome.getPlayerName() == null && theHome.getWorld() == null) ) {
				util.sendLocalizedMessage(p, HSPMessages.NO_HOME_INVITE_FOUND);
				try {
					plugin.getStorage().getHomeInviteDAO().deleteHomeInvite(homeInvite);
				} catch(Exception e) {
					log.log(Level.INFO, "Error deleting homeInvite", e);
				}
				return true;
			}
			
			// if we're not the invited player, we can't use this invite id
			if( !p.getName().equals(homeInvite.getInvitedPlayer()) )
				homeInvite = null;
			
			// check for expiry of the invite
			Date expires = null;
			if( homeInvite != null )
				expires = homeInvite.getExpires();
			if( expires != null && expires.compareTo(new Date()) < 0 ) {
				deleteHomeInvite(homeInvite);
				homeInvite = null;
			}
			
			// check if it's a bedhome and we're not allowed to teleport to bedhomes
    		final boolean allowBedHomeInvites = plugin.getConfig().getBoolean(ConfigOptions.HOME_INVITE_ALLOW_BEDHOME, true);
    		if( !allowBedHomeInvites && homeInvite.getHome().isBedHome() ) {
				deleteHomeInvite(homeInvite);
				homeInvite = null;
    		}
			
			// if homeInvite is still non-null at this point, then we're allowed to use it
			if( homeInvite != null ) {
				// BUG: EBEAN cascading is not working, the @OneToOne entity attached
				// to homeInvite has the id set, but not the attributes.
				debug.devDebug("HomeInviteTeleport: home=",homeInvite.getHome());
				l = homeInvite.getHome().getLocation();
			}
		}
		
		
    	if( l != null ) {
    		// make sure it's on the same world, or if not, that we have cross-world home perms
    		if( !p.getWorld().getName().equals(l.getWorld().getName()) &&
    				!plugin.hasPermission(p, OTHER_WORLD_PERMISSION) ) {
				util.sendLocalizedMessage(p, HSPMessages.CMD_HOME_NO_OTHERWORLD_PERMISSION);
    			return true;
    		}
    		
			String cooldownName = getCooldownName(getCommandName(), Integer.toString(homeInvite.getId()));
			if( plugin.getConfig().getBoolean(ConfigOptions.HOME_INVITE_USE_HOME_COOLDOWN, true) )
				cooldownName = getCooldownName("home", Integer.toString(homeInvite.getId()));
			String warmupName = getCommandName();
			if( plugin.getConfig().getBoolean(ConfigOptions.HOME_INVITE_USE_HOME_WARMUP, true) )
				warmupName = "home";
			
    		debug.debug("homeInviteTeleport command running cooldown check, cooldownName=",cooldownName);
    		if( !cooldownCheck(p, cooldownName) )
    			return true;
    		
			if( hasWarmup(p, warmupName) ) {
	    		final Location finalL = l;
	    		final Home finalHome = theHome;
	    		final String placeString = "home of "+ homeInvite.getHome().getPlayerName();
				doWarmup(p, new WarmupRunner() {
					private boolean canceled = false;
					private String cdName;
					private String wuName;
					
					public void run() {
						if( !canceled ) {
							util.sendLocalizedMessage(p, HSPMessages.CMD_WARMUP_FINISHED,
									"name", getWarmupName(), "place", placeString);
							doHomeTeleport(p, finalL, cdName, finalHome);
						}
					}

					public void cancel() {
						canceled = true;
					}

					public void setPlayerName(String playerName) {}
					public void setWarmupId(int warmupId) {}
					public WarmupRunner setCooldownName(String cd) { cdName = cd; return this; }
					public WarmupRunner setWarmupName(String warmupName) { wuName = warmupName; return this; }
					public String getWarmupName() { return wuName; }
				}.setCooldownName(cooldownName).setWarmupName(warmupName));
			}
			else {
				doHomeTeleport(p, l, cooldownName, theHome);
			}
    	}
    	else
			util.sendLocalizedMessage(p, HSPMessages.NO_HOME_INVITE_FOUND);
    	
		return true;
	}
	
	/** Do a teleport to the home including costs, cooldowns and printing
	 * departure and arrival messages. Is used from both warmups and sync /home.
	 * 
	 * @param p
	 * @param l
	 */
	private void doHomeTeleport(Player p, Location l, String cooldownName,
			org.morganm.homespawnplus.entity.Home home)
	{
		String homeName = "default";
		String playerName = "unknown";
		if( home != null ) {
			if( home.getName() != null )
				homeName = home.getName();
			playerName = home.getPlayerName();
		}
		
		if( applyCost(p, true, cooldownName) ) {
    		if( plugin.getConfig().getBoolean(ConfigOptions.TELEPORT_MESSAGES, false) )
    			util.sendLocalizedMessage(p, HSPMessages.CMD_HOME_INVITE_TELEPORTING,
    					"home", homeName,
    					"player", playerName);
    		
    		util.teleport(p, l, TeleportCause.COMMAND);
		}
	}

	private void deleteHomeInvite(final org.morganm.homespawnplus.entity.HomeInvite hi) {
		// it's expired, so delete it. we ignore any error here since it doesn't
		// affect the outcome of the rest of the command.
		try {
			plugin.getStorage().getHomeInviteDAO().deleteHomeInvite(hi);
		}
		catch(StorageException e) {
			log.log(Level.WARNING, "Caught exception: "+e.getMessage(), e);
		}
	}
}
