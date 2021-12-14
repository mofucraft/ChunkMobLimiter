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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChunkMobLimiterConfig {
    private boolean checkChunkLoad = false;
    private boolean checkChunkUnload = false;
    private boolean limitBreeding = false;
    private List<String> targetWorld = new ArrayList<>();
    private Map<String, LimitConfig> limits = new HashMap<>();

    private ChunkMobLimiter instance;
    private FileConfiguration config;

    public ChunkMobLimiterConfig() {
        instance = ChunkMobLimiter.getInstance();
        instance.reloadConfig();
    }

    public void reloadConfig() {
        config = instance.getConfig();

        checkChunkLoad = config.getBoolean("properties.checkChunkLoad", false);
        checkChunkUnload = config.getBoolean("properties.checkChunkUnload", false);
        limitBreeding = config.getBoolean("properties.limitBreeding", false);
        targetWorld = config.getStringList("targetWorld");

        val limitSection = config.getConfigurationSection("limits");
        limits = LimitConfig.parseLimitConfig(limitSection).stream()
                .collect(Collectors.toUnmodifiableMap(
                        limitConfig -> limitConfig.getEntityType(),
                        limitConfig -> limitConfig
                ));
    }

    public boolean isCheckChunkLoad() {
        return checkChunkLoad;
    }

    public boolean isCheckChunkUnload() {
        return checkChunkUnload;
    }

    public boolean isLimitBreeding() {
        return limitBreeding;
    }

    public List<String> getTargetWorld() {
        return targetWorld;
    }

    public Map<String, LimitConfig> getLimits() {
        return limits;
    }

    public List<LimitConfig> getGroupLimits() {
        return limits.values().stream().filter(LimitConfig::isGroup).collect(Collectors.toList());
    }

    public List<LimitConfig> getEntityGroupLimits() {
        return limits.values().stream().filter(limit -> limit.getEntityType().startsWith("$")).collect(Collectors.toList());
    }


    public static class LimitConfig {
        private String entityType;
        private boolean group;
        private int limit;
        private List<String> groupEntityList;

        public static List<LimitConfig> parseLimitConfig(ConfigurationSection section) {
            val path = section.getName();
            return section.getKeys(false).stream()
                    .map(key -> {
                        val groupList = section.getStringList(path + "." + key + ".entities");
                        val group = !groupList.isEmpty();
                        val limit = section.getInt(path + "." + key + ".limit", -1);
                        return new LimitConfig(key, group, limit, groupList);
                    })
                    .collect(Collectors.toList());
        }

        public LimitConfig(String entityType, boolean group, int limit, List<String> groupEntityList) {
            this.entityType = entityType;
            this.group = group;
            this.limit = limit;
            this.groupEntityList = groupEntityList;
        }

        public String getEntityType() {
            return entityType;
        }

        public boolean isGroup() {
            return group;
        }

        public int getLimit() {
            return limit;
        }

        public List<String> getGroupEntityList() {
            return groupEntityList;
        }
    }
}
