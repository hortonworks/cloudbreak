package com.sequenceiq.it.cloudbreak.newway;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RdsConfigs {

    private Set<RdsConfigEntity> rdsEntries;

    public RdsConfigs() {
        rdsEntries = new LinkedHashSet<>();
    }

    public void addRds(RdsConfigEntity rds) {
        rdsEntries.add(rds);
    }

    public Set<RdsConfigEntity> getRdsAsSet() {
        return rdsEntries;
    }

    public List<RdsConfigEntity> getRdsAsList() {
        return new ArrayList<>(rdsEntries);
    }

}
