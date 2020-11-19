package com.sequenceiq.mock.salt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

public class SaltDto {

    private String mockUuid;

    private List<Minion> minions;

    private Map<String, Multimap<String, String>> grains  = new HashMap<>();

    public SaltDto(String mockUuid) {
        this.mockUuid = mockUuid;
    }

    public String getMockUuid() {
        return mockUuid;
    }

    public void setMockUuid(String mockUuid) {
        this.mockUuid = mockUuid;
    }

    public List<Minion> getMinions() {
        return minions;
    }

    public void setMinions(List<Minion> minions) {
        this.minions = minions;
    }

    public Map<String, Multimap<String, String>> getGrains() {
        return grains;
    }

    public void setGrains(Map<String, Multimap<String, String>> grains) {
        this.grains = grains;
    }
}
