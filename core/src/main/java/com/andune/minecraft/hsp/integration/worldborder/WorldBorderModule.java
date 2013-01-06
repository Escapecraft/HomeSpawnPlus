/**
 * 
 */
package com.andune.minecraft.hsp.integration.worldborder;

import javax.inject.Inject;


import com.andune.minecraft.hsp.Initializable;
import com.andune.minecraft.hsp.server.api.Location;
import com.andune.minecraft.hsp.server.api.Plugin;

/**
 * @author morganm
 *
 */
public class WorldBorderModule implements Initializable {
    private WorldBorderIntegration worldBorder;
    
    @Inject
    public WorldBorderModule(Plugin plugin) {
        this.worldBorder = new WorldBorderIntegration(plugin);
    }

    @Override
    public void init() throws Exception {
        worldBorder.init();
    }

    @Override
    public void shutdown() throws Exception {
        worldBorder = null;
    }

    @Override
    public int getInitPriority() {
        return 9;
    }

    public boolean isEnabled() {
        return worldBorder.isEnabled();
    }
    
    public String getVersion() {
        return worldBorder.getVersion();
    }

    public BorderData getBorderData(String worldName) {
        return worldBorder.getBorderData(worldName);
    }
    
    public static interface BorderData
    {
        public boolean insideBorder(Location l);
        public double getX();
        public double getZ();
        public int getRadius();
//        public Location getCorner1();
//        public Location getCorner2();
    }
}