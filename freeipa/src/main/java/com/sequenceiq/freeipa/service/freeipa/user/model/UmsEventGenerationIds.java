package com.sequenceiq.freeipa.service.freeipa.user.model;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

public class UmsEventGenerationIds {

    private Map<String, String> eventGenerationIds = ImmutableMap.of();

    public Map<String, String> getEventGenerationIds() {
        return eventGenerationIds;
    }

    public void setEventGenerationIds(Map<String, String> eventGenerationIds) {
        this.eventGenerationIds = ImmutableMap.copyOf(eventGenerationIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UmsEventGenerationIds that = (UmsEventGenerationIds) o;

        return Objects.equals(eventGenerationIds, that.eventGenerationIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventGenerationIds);
    }

    @Override
    public String toString() {
        return "UmsEventGenerationIds{"
                + "eventGenerationIds=" + eventGenerationIds
                + '}';
    }
}
