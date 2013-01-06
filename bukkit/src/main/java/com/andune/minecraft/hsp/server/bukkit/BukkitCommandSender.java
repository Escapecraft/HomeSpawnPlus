/**
 * 
 */
package com.andune.minecraft.hsp.server.bukkit;

import com.andune.minecraft.hsp.server.api.CommandSender;

/**
 * @author morganm
 *
 */
public class BukkitCommandSender implements CommandSender {
    private org.bukkit.command.CommandSender bukkitSender;
    
    public BukkitCommandSender(org.bukkit.command.CommandSender bukkitSender) {
        this.bukkitSender = bukkitSender;
    }

    @Override
    public void sendMessage(String message) {
        bukkitSender.sendMessage(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        bukkitSender.sendMessage(messages);
    }

    @Override
    public String getName() {
        return bukkitSender.getName();
    }

    public org.bukkit.command.CommandSender getBukkitSender() {
        return bukkitSender;
    }
}