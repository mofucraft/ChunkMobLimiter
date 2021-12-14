package page.nafuchoco.chunkmoblimiter;

import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import static org.bukkit.entity.EntityType.*;

public final class ChunkMobLimiter extends JavaPlugin implements Listener {
    // リミット対象にしないもの
    private static final Set<EntityType> EXCLUDE_ENTITY = Set.of(
            DROPPED_ITEM,
            EXPERIENCE_ORB,
            AREA_EFFECT_CLOUD,
            PAINTING,
            ARROW,
            SNOWBALL,
            FIREBALL,
            SMALL_FIREBALL,
            ENDER_PEARL,
            ENDER_SIGNAL,
            SPLASH_POTION,
            THROWN_EXP_BOTTLE,
            ITEM_FRAME,
            WITHER_SKULL,
            PRIMED_TNT,
            FALLING_BLOCK,
            FIREWORK,
            SPECTRAL_ARROW,
            SHULKER_BULLET,
            DRAGON_FIREBALL,
            ARMOR_STAND,
            EVOKER_FANGS,
            BOAT,
            MINECART,
            MINECART_CHEST,
            MINECART_FURNACE,
            MINECART_TNT,
            MINECART_HOPPER,
            MINECART_MOB_SPAWNER,
            ENDER_CRYSTAL,
            TRIDENT,
            GLOW_ITEM_FRAME,
            FISHING_HOOK,
            LIGHTNING,
            PLAYER
    );

    private static ChunkMobLimiter instance;
    private ChunkMobLimiterConfig config;

    public static ChunkMobLimiter getInstance() {
        if (instance == null)
            instance = (ChunkMobLimiter) Bukkit.getServer().getPluginManager().getPlugin("ChunkMobLimiter");
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        config = new ChunkMobLimiterConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent event) {
        event.setCancelled(checkLimit(event.getEntity()));
    }

    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        if (config.isCheckChunkLoad()) {
            // TODO: 2021/12/14
            /*
            val chunk = event.getChunk();

            // チャンク内に存在するリミット対象のエンティティの一覧を取得する
            val entityTypeList = Arrays.stream(chunk.getEntities())
                    .map(e -> e.getType())
                    .filter(type -> !EXCLUDE_ENTITY.contains(type))
                    .distinct()
                    .collect(Collectors.toList());

            */
        }
    }

    @EventHandler
    public void onChunkUnloadEvent(ChunkUnloadEvent event) {
        // TODO: 2021/12/14
        if (config.isCheckChunkUnload()) {
        }
    }

    @EventHandler
    public void onEntityBreedEvent(EntityBreedEvent event) {
        if (config.isLimitBreeding()) {
            event.setCancelled(checkLimit(event.getEntity()));
        }
    }

    public boolean checkLimit(Entity entity) {
        val entityType = entity.getType();
        val chunk = entity.getLocation().getChunk();
        var limitConfig = config.getLimits().get(entityType.toString());

        // 除外対象であるかの確認
        if (!EXCLUDE_ENTITY.contains(entityType)) {
            // エンティティが属するワールドが除外対象の場合は常時falseを返す
            if (config.getExcludeWorld().contains(chunk.getWorld().getName()))
                return false;

            // 対象のエンティティが所属するグループからリミット値の一番小さいものを取得する
            limitConfig = config.getGroupLimits().stream()
                    .filter(group -> group.getGroupEntityList().contains(entityType.toString()))
                    .sorted(Comparator.comparingInt(group -> group.getLimit()))
                    .findFirst().orElse(null);

            // 個別のリミット設定がない場合はデフォルトを適用する
            if (limitConfig == null)
                limitConfig = config.getLimits().get("default");

            // リミット値が-1の場合は常時falseを返す
            if (limitConfig.getLimit() == -1)
                return false;

            // チャンク内の対象となるエンティティの数を計算する
            var targetEntityInChunkNumber = Arrays.stream(chunk.getEntities())
                    .filter(target -> !EXCLUDE_ENTITY.contains(target.getType()))
                    .count();

            // チャンク内にリミット数以上エンティティが存在する場合trueを返す
            if (targetEntityInChunkNumber > limitConfig.getLimit())
                return true;
        }

        // いずれにも当てはまらない場合false
        return false;
    }
}
