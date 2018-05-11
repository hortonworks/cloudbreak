package com.sequenceiq.cloudbreak.api.model.stack;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ManagementPackEntry;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.StackRepoDetailsJson;

public class StackDescriptor {

    private String version;

    private String minAmbari;

    private StackRepoDetailsJson repo;

    private List<ManagementPackEntry> mpacks;

    private AmbariInfoJson ambari;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMinAmbari() {
        return minAmbari;
    }

    public void setMinAmbari(String minAmbari) {
        this.minAmbari = minAmbari;
    }

    public StackRepoDetailsJson getRepo() {
        return repo;
    }

    public void setRepo(StackRepoDetailsJson repo) {
        this.repo = repo;
    }

    public AmbariInfoJson getAmbari() {
        return ambari;
    }

    public void setAmbari(AmbariInfoJson ambari) {
        this.ambari = ambari;
    }

    public List<ManagementPackEntry> getMpacks() {
        return mpacks;
    }

    public void setMpacks(List<ManagementPackEntry> mpacks) {
        this.mpacks = mpacks;
    }
}
