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
package org.morganm.homespawnplus.strategies;

import org.bukkit.Location;
import org.morganm.homespawnplus.strategy.BaseStrategy;
import org.morganm.homespawnplus.strategy.StrategyContext;
import org.morganm.homespawnplus.strategy.StrategyException;
import org.morganm.homespawnplus.strategy.StrategyFactory;
import org.morganm.homespawnplus.strategy.StrategyResult;

/** Strategy to return the nearest home or spawn, whichever is closer.
 * 
 * @author morganm
 *
 */
public class NearestHomeOrSpawn extends BaseStrategy {
	private final HomeNearestHome nearestHome;
	private final SpawnNearestSpawn nearestSpawn;
	
	public NearestHomeOrSpawn() throws StrategyException {
		nearestHome = (HomeNearestHome) StrategyFactory.newStrategy(HomeNearestHome.class);
		nearestSpawn = (SpawnNearestSpawn) StrategyFactory.newStrategy(SpawnNearestSpawn.class);
	}
	
	@Override
	public StrategyResult evaluate(StrategyContext context) {
		StrategyResult homeResult = nearestHome.evaluate(context);
		StrategyResult spawnResult = nearestSpawn.evaluate(context);
		
		Location homeLocation;
		Location spawnLocation;
		
		// if either one is null, return the other
		if( homeResult == null )
			return spawnResult;
		else {
			homeLocation = homeResult.getLocation();
			if( homeLocation == null )
				return spawnResult;
		}
		if( spawnResult == null )
			return homeResult;
		else {
			spawnLocation = spawnResult.getLocation();
			if( spawnLocation == null )
				return homeResult;
		}

		double homeDistance = context.getEventLocation().distance(homeLocation);
		double spawnDistance = context.getEventLocation().distance(spawnLocation);
		
		// otherwise, compare the results and return the closer one
		if( homeDistance < spawnDistance )
			return homeResult;
		else
			return spawnResult;
	}

	@Override
	public String getStrategyConfigName() {
		return "nearestHomeOrSpawn";
	}

}
