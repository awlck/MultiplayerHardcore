package de.diepixelecke.mphc;

import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

public class DamageSourceFinder {
    public static String describeSource(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            Entity damageSource = ((EntityDamageByEntityEvent) event).getDamager();
            if (damageSource instanceof Projectile)
                return describeProjectileSource(((Projectile) damageSource).getShooter());
            if (damageSource instanceof AreaEffectCloud)
                return describeProjectileSource(((AreaEffectCloud) damageSource).getSource());
            return damageSource.getName();
        } else if (event instanceof EntityDamageByBlockEvent) {
            Block damageSource = ((EntityDamageByBlockEvent) event).getDamager();
            assert damageSource != null;
            return damageSource.getType().toString();
        } else return "";
    }

    private static String describeProjectileSource(ProjectileSource source) {
        if (source instanceof LivingEntity) {
            return ((LivingEntity) source).getName();
        } else if (source instanceof BlockProjectileSource) {
            return ((BlockProjectileSource) source).getBlock().getType().toString();
        } else return source.getClass().getName();
    }
}
