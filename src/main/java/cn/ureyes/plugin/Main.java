package cn.ureyes.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.io.File;

public final class Main extends JavaPlugin {

    private FileConfiguration messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();
        getServer().getPluginManager().registerEvents(new DeathListener(), this);

        // 注册重载命令
        getCommand("killmessagereload").setExecutor(new ReloadCommand());
        getLogger().info(ChatColor.GREEN + "死亡消息插件已加载！");
    }

    // 新增重载命令类
    private class ReloadCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("killmessage.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                return true;
            }

            try {
                reloadConfig();
                loadMessages();
                sender.sendMessage(ChatColor.GREEN + "配置文件重载成功！");
                getLogger().info("配置文件已通过命令重载");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "配置文件重载失败！请检查控制台日志");
                getLogger().severe("配置文件重载时发生错误：");
                e.printStackTrace();
            }
            return true;
        }
    }

    // 修改后的配置加载方法
    private void loadMessages() {
        try {
            // 如果配置文件不存在则创建
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            File messagesFile = new File(getDataFolder(), "messages.yml");
            if (!messagesFile.exists()) {
                saveResource("messages.yml", false);
                getLogger().info("默认配置文件已生成");
            }

            // 加载配置
            messages = YamlConfiguration.loadConfiguration(messagesFile);

        } catch (Exception e) {
            getLogger().severe("加载配置文件时发生错误！");
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        super.reloadConfig();
        loadMessages();
    }

    public class DeathListener implements Listener {

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            String reason = getDeathReason(player);
            String message = messages.getString("death-format")
                    .replace("{player}", player.getName())
                    .replace("{reason}", reason);
            event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        private String getDeathReason(Player player) {
            EntityDamageEvent damageEvent = player.getLastDamageCause();
            if (damageEvent == null) return getMsg("unknown", "未知原因");

            DamageCause cause = damageEvent.getCause();
            switch (cause) {
                case ENTITY_ATTACK: return handleEntityAttack(damageEvent);
                case PROJECTILE: return handleProjectile(damageEvent);
                case FALL: return getMsg("reasons.fall");
                case FIRE: case FIRE_TICK: return getMsg("reasons.fire");
                case LAVA: return getMsg("reasons.lava");
                case DROWNING: return getMsg("reasons.drowning");
                case BLOCK_EXPLOSION: case ENTITY_EXPLOSION: return getMsg("reasons.explosion");
                case VOID: return getMsg("reasons.void");
                case SUFFOCATION: return getMsg("reasons.suffocation");
                case STARVATION: return getMsg("reasons.starvation");
                case POISON: return getMsg("reasons.poison");
                case WITHER: return getMsg("reasons.wither");
                case LIGHTNING: return getMsg("reasons.lightning");
                case MAGIC: return getMsg("reasons.magic");
                case CONTACT: return getMsg("reasons.contact");
                case HOT_FLOOR: return getMsg("reasons.hot_floor");
                case DRYOUT: return getMsg("reasons.dryout");
                case FREEZE: return getMsg("reasons.freeze");
                case SONIC_BOOM: return getMsg("reasons.sonic_boom");
                default: return getMsg("unknown_with_cause").replace("{cause}", cause.name());
            }
        }

        private String handleEntityAttack(EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent)) return getMsg("attacks.melee");

            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            String attacker = getAttackerName(damager);

            if (damager instanceof Player) {
                return getMsg("attacks.player").replace("{attacker}", ((Player) damager).getName());
            }
            return getMsg("attacks.mob").replace("{attacker}", attacker);
        }

        private String handleProjectile(EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent)) return getMsg("attacks.projectile");

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            String projectileType = getProjectileType(projectile.getType());

            if (projectile instanceof Projectile) {
                ProjectileSource source = ((Projectile) projectile).getShooter();
                if (source instanceof Entity) {
                    Entity shooter = (Entity) source;
                    String shooterName = getAttackerName(shooter);

                    if (shooter instanceof Player) {
                        return getMsg("attacks.player_projectile")
                                .replace("{attacker}", ((Player) shooter).getName())
                                .replace("{projectile}", projectileType);
                    }
                    return getMsg("attacks.mob_projectile")
                            .replace("{attacker}", shooterName)
                            .replace("{projectile}", projectileType);
                }
            }
            return getMsg("attacks.projectile") + " (" + projectileType + ")";
        }

        private String getAttackerName(Entity entity) {
            // 优先获取自定义名称
            if (entity instanceof LivingEntity) {
                String customName = ((LivingEntity) entity).getCustomName();
                if (customName != null && !customName.isEmpty()) {
                    return customName;
                }
            }

            // 检查配置文件中的名称映射
            String typeName = entity.getType().name();
            String configPath = "entities." + typeName;
            return messages.getString(configPath, typeName.toLowerCase());
        }

        private String getProjectileType(EntityType type) {
            return messages.getString("projectiles." + type.name(), type.name().toLowerCase());
        }

        private String getMsg(String path) {
            return messages.getString(path, "未知原因");
        }

        private String getMsg(String path, String def) {
            return messages.getString(path, def);
        }
    }
}