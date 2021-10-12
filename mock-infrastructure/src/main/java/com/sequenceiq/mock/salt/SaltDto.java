package com.sequenceiq.mock.salt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

public class SaltDto {

    private String mockUuid;

    private SaltAction saltAction;

    private List<FileDistributonDto> fileDistributonDtos = new ArrayList<>();

    private Map<String, Multimap<String, String>> grains = new HashMap<>();

    private List<RunResponseDto> runResponsDtos = new ArrayList<>();

    private List<Pillar> pillars = new ArrayList<>();

    private List<Minion> minions = new ArrayList<>();

    public SaltDto(String mockUuid) {
        this.mockUuid = mockUuid;
    }

    public String getMockUuid() {
        return mockUuid;
    }

    public void setMockUuid(String mockUuid) {
        this.mockUuid = mockUuid;
    }

    public SaltAction getSaltAction() {
        return saltAction;
    }

    public void setSaltAction(SaltAction saltAction) {
        this.saltAction = saltAction;
    }

    public Map<String, Multimap<String, String>> getGrains() {
        return grains;
    }

    public void setGrains(Map<String, Multimap<String, String>> grains) {
        this.grains = grains;
    }

    public List<RunResponseDto> getRunResponses() {
        return runResponsDtos;
    }

    public List<FileDistributonDto> getFileDistributonDtos() {
        return fileDistributonDtos;
    }

    public List<Pillar> getPillars() {
        return pillars;
    }

    public List<Minion> getMinions() {
        return minions;
    }

    public void setMinions(List<Minion> minions) {
        this.minions = minions;
    }
}
