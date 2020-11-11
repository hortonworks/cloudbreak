package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MinionStatusSaltResponse {

    @JsonProperty("return")
    private List<MinionStatus> result;

    public List<String> upMinions() {
        return result.stream()
                .flatMap(minionStatus -> minionStatus.getUp().stream())
                .collect(Collectors.toList());
    }

    public List<String> downMinions() {
        return result.stream()
                .flatMap(minionStatus -> minionStatus.getDown().stream())
                .collect(Collectors.toList());
    }

    public List<MinionStatus> getResult() {
        return result;
    }

    public void setResult(List<MinionStatus> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "MinionStatusSaltResponse{"
                + "result=" + result
                + '}';
    }
}
