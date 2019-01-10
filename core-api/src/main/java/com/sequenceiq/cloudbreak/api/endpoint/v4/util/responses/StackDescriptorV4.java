package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ManagementPackEntry;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.StackRepoDetailsJson;

public class StackDescriptorV4 {

    private String version;

    private String minAmbari;

    private StackRepoDetailsJson repo;

    private Map<String, List<ManagementPackEntry>> mpacks;

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

    public Map<String, List<ManagementPackEntry>> getMpacks() {
        return mpacks;
    }

    public void setMpacks(Map<String, List<ManagementPackEntry>> mpacks) {
        this.mpacks = mpacks;
    }
}
