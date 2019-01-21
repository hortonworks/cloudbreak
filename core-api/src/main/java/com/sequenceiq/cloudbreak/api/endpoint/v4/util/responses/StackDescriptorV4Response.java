package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.StackRepoDetailsV4Response;

public class StackDescriptorV4Response {

    private String version;

    private String minAmbari;

    private StackRepoDetailsV4Response repo;

    private Map<String, List<ManagementPackV4Entry>> mpacks;

    private AmbariInfoV4Response ambari;

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

    public StackRepoDetailsV4Response getRepo() {
        return repo;
    }

    public void setRepo(StackRepoDetailsV4Response repo) {
        this.repo = repo;
    }

    public AmbariInfoV4Response getAmbari() {
        return ambari;
    }

    public void setAmbari(AmbariInfoV4Response ambari) {
        this.ambari = ambari;
    }

    public Map<String, List<ManagementPackV4Entry>> getMpacks() {
        return mpacks;
    }

    public void setMpacks(Map<String, List<ManagementPackV4Entry>> mpacks) {
        this.mpacks = mpacks;
    }
}
