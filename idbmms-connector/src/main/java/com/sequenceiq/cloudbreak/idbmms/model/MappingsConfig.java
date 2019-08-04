package com.sequenceiq.cloudbreak.idbmms.model;

import java.util.Collections;
import java.util.Map;

/**
 * A POJO representing an immutable IDBroker Mapping Management Service mappings config.
 */
public class MappingsConfig {

    /**
     * The version of the mappings.
     */
    private final long mappingsVersion;

    /**
     * A map of actor workloadUsernames to cloud provider roles. Includes mappings for data access services.
     */
    private final Map<String, String> actorMappings;

    /**
     * A map of group names to cloud provider roles.
     */
    private final Map<String, String> groupMappings;

    public MappingsConfig(long mappingsVersion, Map<String, String> actorMappings, Map<String, String> groupMappings) {
        this.mappingsVersion = mappingsVersion;
        this.actorMappings = actorMappings == null ? Collections.emptyMap() : Collections.unmodifiableMap(actorMappings);
        this.groupMappings = groupMappings == null ? Collections.emptyMap() : Collections.unmodifiableMap(groupMappings);
    }

    public long getMappingsVersion() {
        return mappingsVersion;
    }

    public Map<String, String> getActorMappings() {
        return actorMappings;
    }

    public Map<String, String> getGroupMappings() {
        return groupMappings;
    }

}
