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

import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.morganm.homespawnplus.command.BaseCommand;
import org.morganm.homespawnplus.i18n.HSPMessages;
import org.morganm.homespawnplus.storage.StorageException;

/**
 * @author morganm
 *
 */
public class HomeInviteDelete extends BaseCommand {

	@Override
	public String[] getCommandAliases() { return new String[] {"hid"}; }

	@Override
	public String getUsage() {
		return	util.getLocalizedMessage(HSPMessages.CMD_HOME_INVITE_DELETE_USAGE);
	}

	/* (non-Javadoc)
	 * @see org.morganm.homespawnplus.command.Command#execute(org.bukkit.entity.Player, org.bukkit.command.Command, java.lang.String[])
	 */
	@Override
	public boolean execute(Player p, Command command, String[] args) {
		if( !defaultCommandChecks(p) )
			return true;
		
		if( args.length < 1 ) {
			util.sendLocalizedMessage(p, HSPMessages.ERROR_ID_NUMBER_REQUIRED,
					"input", "null");
			return true;
		}
		
		int id = -1;
		try {
			id = Integer.parseInt(args[0]);
		}
		catch(NumberFormatException e) {
			util.sendLocalizedMessage(p, HSPMessages.ERROR_ID_NUMBER_REQUIRED,
					"input", args[0]);
			return true;
		}
		org.morganm.homespawnplus.entity.HomeInvite hi = plugin.getStorage().getHomeInviteDAO().findHomeInviteById(id);
		
		// make sure we found an object and that the home is owned by the player
		if( hi != null && p.getName().equals(hi.getHome().getPlayerName()) ) {
			try {
				org.morganm.homespawnplus.entity.Home h = hi.getHome();
				String invitee = hi.getInvitedPlayer();
				plugin.getStorage().getHomeInviteDAO().deleteHomeInvite(hi);
				util.sendLocalizedMessage(p, HSPMessages.HOMEINVITE_DELETED,
						"id", id,
						"home", h.getName(),
						"invitee", invitee);
			}
			catch(StorageException e) {
				util.sendLocalizedMessage(p, HSPMessages.GENERIC_ERROR);
				log.log(Level.WARNING, "Caught exception in /"+getCommandName()+": "+e.getMessage(), e);
			}
		}
		else {
			util.sendLocalizedMessage(p, HSPMessages.HOMEINVITE_ID_NOT_FOUND,
					"id", args[0]);
		}
		
		return true;
	}

}
