/**
 * 
 */
package org.morganm.homespawnplus.strategies;

import org.morganm.homespawnplus.strategy.StrategyMode;
import org.morganm.homespawnplus.strategy.ModeStrategy;

/**
 * @author morganm
 *
 */
public class ModeHomeAny extends ModeStrategy {

	@Override
	public String getStrategyConfigName() {
		return "modeHomeAny";
	}

	@Override
	protected StrategyMode getMode() {
		return StrategyMode.MODE_HOME_ANY;
	}

}
