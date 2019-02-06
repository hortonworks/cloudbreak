package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

public class MinionStatus {

    private List<String> down;

    private List<String> up;

    public List<String> getDown() {
        return down;
    }

    public void setDown(List<String> down) {
        this.down = down;
    }

    public List<String> getUp() {
        return up;
    }

    public void setUp(List<String> up) {
        this.up = up;
    }

    @Override
    public String toString() {
        return "MinionStatus{"
                + "down=" + down
                + ", up=" + up
                + '}';
    }
}