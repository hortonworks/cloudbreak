package com.sequenceiq.cloudbreak.common.mappable;

import java.util.Map;

public abstract class MappableBase implements Mappable {

    @Override
    public Map<String, Object> asMap() {
        return defaultMap();
    }
}
