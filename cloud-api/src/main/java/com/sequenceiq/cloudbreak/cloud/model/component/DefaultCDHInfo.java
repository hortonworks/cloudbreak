package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.type.Versioned;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultCDHInfo implements Versioned {

    private String version;

    private ClouderaManagerDefaultStackRepoDetails repo;

    private List<ClouderaManagerProduct> parcels;

    private List<String> csd;

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ClouderaManagerDefaultStackRepoDetails getRepo() {
        if (repo != null) {
            repo.setCdhVersion(version);
        }
        return repo;
    }

    public void setRepo(ClouderaManagerDefaultStackRepoDetails repo) {
        this.repo = repo;
    }

    public List<ClouderaManagerProduct> getParcels() {
        if (parcels == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(parcels);
    }

    public void setParcels(List<ClouderaManagerProduct> parcels) {
        this.parcels = parcels;
    }

    public List<String> getCsd() {
        if (csd == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(csd);
    }

    public void setCsd(List<String> csd) {
        this.csd = csd;
    }

    @Override
    public String toString() {
        return "StackInfo{"
                + "version='" + version + '\''
                + ", repo=" + repo
                + '}';
    }
}
