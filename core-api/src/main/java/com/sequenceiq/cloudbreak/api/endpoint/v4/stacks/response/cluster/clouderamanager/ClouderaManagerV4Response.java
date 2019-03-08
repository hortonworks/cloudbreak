package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerV4Response implements JsonEntity {

    @Valid
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CM_REPO_DETAILS)
    private ClouderaManagerRepositoryV4Response repository;

    @Valid
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CM_PRODUCT_DETAILS)
    private List<ClouderaManagerProductV4Response> products;

    public ClouderaManagerRepositoryV4Response getRepository() {
        return repository;
    }

    public void setRepository(ClouderaManagerRepositoryV4Response repository) {
        this.repository = repository;
    }

    public List<ClouderaManagerProductV4Response> getProducts() {
        return products;
    }

    public void setProducts(List<ClouderaManagerProductV4Response> products) {
        this.products = products;
    }

    public ClouderaManagerV4Response withRepository(ClouderaManagerRepositoryV4Response repository) {
        setRepository(repository);
        return this;
    }

    public ClouderaManagerV4Response withProducts(List<ClouderaManagerProductV4Response> products) {
        setProducts(products);
        return this;
    }
}
