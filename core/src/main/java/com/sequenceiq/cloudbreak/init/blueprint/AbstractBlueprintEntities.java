package com.sequenceiq.cloudbreak.init.blueprint;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBlueprintEntities {

    private Map<String, String> defaults = new HashMap<>();

    public Map<String, String> getDefaults() {
        return this.defaults;
    }
}
