package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseImageV4Response extends ImageV4Response {

    private List<AmbariStackDetailsV4Response> hdpStacks;

    private List<AmbariStackDetailsV4Response> hdfStacks;

    private List<ClouderaManagerStackDetailsV4Response> cdhStacks;

    private AmbariRepositoryV4Response ambariRepo;

    private ClouderaManagerRepositoryV4Response clouderaManagerRepo;

    public List<AmbariStackDetailsV4Response> getHdpStacks() {
        return hdpStacks;
    }

    public void setHdpStacks(List<AmbariStackDetailsV4Response> hdpStacks) {
        this.hdpStacks = hdpStacks;
    }

    public List<AmbariStackDetailsV4Response> getHdfStacks() {
        return hdfStacks;
    }

    public void setHdfStacks(List<AmbariStackDetailsV4Response> hdfStacks) {
        this.hdfStacks = hdfStacks;
    }

    public List<ClouderaManagerStackDetailsV4Response> getCdhStacks() {
        return cdhStacks;
    }

    public void setCdhStacks(List<ClouderaManagerStackDetailsV4Response> cdhStacks) {
        this.cdhStacks = cdhStacks;
    }

    public AmbariRepositoryV4Response getAmbariRepo() {
        return ambariRepo;
    }

    public void setAmbariRepo(AmbariRepositoryV4Response ambariRepo) {
        this.ambariRepo = ambariRepo;
    }

    public ClouderaManagerRepositoryV4Response getClouderaManagerRepo() {
        return clouderaManagerRepo;
    }

    public void setClouderaManagerRepo(ClouderaManagerRepositoryV4Response clouderaManagerRepo) {
        this.clouderaManagerRepo = clouderaManagerRepo;
    }
}
