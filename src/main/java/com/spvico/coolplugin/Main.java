package com.spvico.coolplugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    
    private Map<UUID, List<ArmorStand>> playerSwords = new HashMap<>();
    private Map<UUID, Boolean> isAttacking = new HashMap<>();
    private Map<UUID, Location> attackTargets = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        getLogger().info("§6CoolPlugin enabled! Made by Spvico!");
        
        // Start sword animation
        new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                angle += 3;
                for (UUID playerId : playerSwords.keySet()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || !player.isOnline()) continue;
                    
                    updateSwords(player, angle);
                }
            }
        }.runTaskTimer(this, 0, 2);
    }
    
    private void updateSwords(Player player, double angle) {
        if (!playerSwords.containsKey(player.getUniqueId())) {
            createSwords(player);
        }
        
        List<ArmorStand> swords = playerSwords.get(player.getUniqueId());
        Location center = player.getLocation().add(0, 2.5, 0);
        int swordCount = swords.size();
        double radius = 1.5 + (swordCount * 0.3);
        
        for (int i = 0; i < swordCount; i++) {
            ArmorStand sword = swords.get(i);
            double currentAngle = angle + (i * 360.0 / swordCount);
            double rad = Math.toRadians(currentAngle);
            
            double x = Math.cos(rad) * radius;
            double z = Math.sin(rad) * radius;
            
            Location newLoc = center.clone().add(x, 0, z);
            newLoc.setDirection(new Vector(x, 0, z));
            
            sword.teleport(newLoc);
        }
    }
    
    private void createSwords(Player player) {
        if (!player.isOp()) return;
        
        List<ArmorStand> swords = new ArrayList<>();
        int swordCount = 0;
        
        // Count swords in inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().toString().endsWith("_SWORD")) {
                swordCount++;
            }
        }
        
        if (swordCount == 0) {
            player.sendMessage(ChatColor.RED + "You need swords in your inventory!");
            return;
        }
        
        for (int i = 0; i < swordCount; i++) {
            ArmorStand sword = (ArmorStand) player.getWorld().spawnEntity(
                player.getLocation(), EntityType.ARMOR_STAND
            );
            
            sword.setVisible(false);
            sword.setGravity(false);
            sword.setInvulnerable(true);
            sword.setSmall(true);
            sword.setArms(true);
            sword.setBasePlate(false);
            sword.setMarker(true);
            
            // Create a diamond sword for display
            ItemStack displaySword = new ItemStack(Material.DIAMOND_SWORD);
            sword.getEquipment().setItemInMainHand(displaySword);
            
            swords.add(sword);
        }
        
        playerSwords.put(player.getUniqueId(), swords);
        player.sendMessage(ChatColor.GREEN + "Floating swords created! " + swordCount + " swords circling you!");
    }
    
    @EventHandler
    public void onPlayerAttack(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp() || !playerSwords.containsKey(player.getUniqueId())) return;
        
        if (player.isSneaking()) {
            // Spread attack
            spreadSwords(player);
        } else {
            // Target attack
            attackTarget(player);
        }
    }
    
    private void spreadSwords(Player player) {
        List<ArmorStand> swords = playerSwords.get(player.getUniqueId());
        Location center = player.getLocation();
        
        for (int i = 0; i < swords.size(); i++) {
            ArmorStand sword = swords.get(i);
            double angle = (i * 360.0 / swords.size());
            double rad = Math.toRadians(angle);
            
            double x = Math.cos(rad) * 10;
            double z = Math.sin(rad) * 10;
            
            Location target = center.clone().add(x, 0, z);
            sword.teleport(target);
            
            // Damage nearby entities
            for (Entity entity : target.getWorld().getNearbyEntities(target, 3, 3, 3)) {
                if (entity instanceof LivingEntity && entity != player) {
                    ((LivingEntity) entity).damage(6.0, player);
                    target.getWorld().spawnParticle(Particle.CRIT, target, 10);
                }
            }
        }
        
        player.sendMessage(ChatColor.YELLOW + "Swords spread attack!");
    }
    
    private void attackTarget(Player player) {
        Location target = player.getTargetBlock(null, 150).getLocation().add(0.5, 0.5, 0.5);
        List<ArmorStand> swords = playerSwords.get(player.getUniqueId());
        
        for (ArmorStand sword : swords) {
            sword.teleport(target);
            
            // Damage entities at target
            for (Entity entity : target.getWorld().getNearbyEntities(target, 4, 4, 4)) {
                if (entity instanceof LivingEntity && entity != player) {
                    ((LivingEntity) entity).damage(8.0, player);
                    target.getWorld().playSound(target, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                }
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "Swords attacking target!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("reloadswords") && sender instanceof Player) {
            Player player = (Player) sender;
            if (player.isOp()) {
                // Remove old swords
                if (playerSwords.containsKey(player.getUniqueId())) {
                    for (ArmorStand sword : playerSwords.get(player.getUniqueId())) {
                        sword.remove();
                    }
                }
                playerSwords.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Swords reloaded!");
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void onDisable() {
        // Clean up
        for (List<ArmorStand> swords : playerSwords.values()) {
            for (ArmorStand sword : swords) {
                sword.remove();
            }
        }
    }
}