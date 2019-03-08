package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClouderaManagerV4Request implements JsonEntity {

    @Valid
    @ApiModelProperty(ClusterModelDescription.CM_REPO_DETAILS)
    private ClouderaManagerRepositoryV4Request repository;

    @Valid
    @ApiModelProperty(ClusterModelDescription.CM_PRODUCT_DETAILS)
    private List<ClouderaManagerProductV4Request> products;

    public ClouderaManagerRepositoryV4Request getRepository() {
        return repository;
    }

    public void setRepository(ClouderaManagerRepositoryV4Request repository) {
        this.repository = repository;
    }

    public List<ClouderaManagerProductV4Request> getProducts() {
        return products;
    }

    public void setProducts(List<ClouderaManagerProductV4Request> products) {
        this.products = products;
    }

    public ClouderaManagerV4Request withRepository(ClouderaManagerRepositoryV4Request repository) {
        setRepository(repository);
        return this;
    }

    public ClouderaManagerV4Request withProducts(List<ClouderaManagerProductV4Request> products) {
        setProducts(products);
        return this;
    }
}
