package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseImageV4Response extends ImageV4Response {

    private List<ClouderaManagerStackDetailsV4Response> cdhStacks;

    private ClouderaManagerRepositoryV4Response clouderaManagerRepo;

    public List<ClouderaManagerStackDetailsV4Response> getCdhStacks() {
        return cdhStacks;
    }

    public void setCdhStacks(List<ClouderaManagerStackDetailsV4Response> cdhStacks) {
        this.cdhStacks = cdhStacks;
    }

    public ClouderaManagerRepositoryV4Response getClouderaManagerRepo() {
        return clouderaManagerRepo;
    }

    public void setClouderaManagerRepo(ClouderaManagerRepositoryV4Response clouderaManagerRepo) {
        this.clouderaManagerRepo = clouderaManagerRepo;
    }
}
