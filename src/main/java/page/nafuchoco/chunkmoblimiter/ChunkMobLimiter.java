/*
 * Copyright 2021 NAFU_at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.chunkmoblimiter;

import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Comparator;

public final class ChunkMobLimiter extends JavaPlugin implements Listener {
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
        getLogger().info("Config Loaded! " + config.toString());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent event) {
        event.setCancelled(checkLimit(event.getEntity()));
        if (config.isDebug())
            getLogger().info("[Debug] Entity Spawn Event (isCancelled: " + event.isCancelled() + ")");
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

        // エンティティがMob以外の場合falseを返す
        // または エンティティが属するワールドがターゲット以外の場合は常時falseを返す
        if (!(entity instanceof Mob)
                || !config.getTargetWorld().contains(chunk.getWorld().getName()))
            return false;

        // 個別リミットの設定がない場合、エンティティクラスグループからリミット値の一番小さいものを取得する
        if (limitConfig == null) {
            limitConfig = config.getEntityGroupLimits().stream()
                    .filter(limit -> getEntityClass(limit.getEntityType()).isInstance(entity))
                    .sorted(Comparator.comparingInt(ChunkMobLimiterConfig.LimitConfig::getLimit))
                    .findFirst().orElse(null);
        }

        // 個別リミット設定，エンティティクラスグループ設定がない場合、対象のエンティティが所属するグループからリミット値の一番小さいものを取得する
        if (limitConfig == null) {
            limitConfig = config.getGroupLimits().stream()
                    .filter(group -> group.getGroupEntityList().contains(entityType.toString()))
                    .sorted(Comparator.comparingInt(ChunkMobLimiterConfig.LimitConfig::getLimit))
                    .findFirst().orElse(null);
        }

        // いずれのリミット設定もない場合はデフォルトを適用する
        if (limitConfig == null)
            limitConfig = config.getLimits().get("default");

        // リミット値が-1の場合は常時falseを返す
        if (limitConfig.getLimit() == -1)
            return false;

        // チャンク内の対象となるエンティティの数を計算する
        var targetEntityInChunkNumber = Arrays.stream(chunk.getEntities())
                .filter(Mob.class::isInstance)
                .count();

        if (config.isDebug())
            getLogger().info("[Debug] Check Limit(EntityType:" + entityType
                    + ", LimitType: " + limitConfig.getEntityType()
                    + ", Limit: " + limitConfig.getLimit()
                    + ", ChunkCount: " + targetEntityInChunkNumber
                    + ", isLimit: " + (targetEntityInChunkNumber > limitConfig.getLimit()) + ")");

        // チャンク内にリミット数以上エンティティが存在する場合trueを返す
        if (targetEntityInChunkNumber > limitConfig.getLimit())
            return true;

        // いずれにも当てはまらない場合false
        return false;
    }

    private Class getEntityClass(String classname) {
        String name = classname;
        if (classname.startsWith("$"))
            name = classname.substring(1);

        try {
            return Class.forName("org.bukkit.entity." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
