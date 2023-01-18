package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClouderaManagerV4Request implements JsonEntity {
    @Valid
    @Schema(description = ClusterModelDescription.CM_REPO_DETAILS)
    private ClouderaManagerRepositoryV4Request repository;

    @Valid
    @Schema(description = ClusterModelDescription.CM_PRODUCT_DETAILS)
    private List<ClouderaManagerProductV4Request> products;

    @Schema(description = ClusterModelDescription.CM_ENABLE_AUTOTLS, required = true)
    private Boolean enableAutoTls = Boolean.TRUE;

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

    public Boolean getEnableAutoTls() {
        return enableAutoTls;
    }

    public void setEnableAutoTls(Boolean enableAutoTls) {
        this.enableAutoTls = enableAutoTls;
    }

    public ClouderaManagerV4Request withRepository(ClouderaManagerRepositoryV4Request repository) {
        setRepository(repository);
        return this;
    }

    public ClouderaManagerV4Request withProducts(List<ClouderaManagerProductV4Request> products) {
        setProducts(products);
        return this;
    }

    public ClouderaManagerV4Request withEnableAutoTls(Boolean enableAutoTls) {
        setEnableAutoTls(enableAutoTls);
        return this;
    }

    @Override
    public String toString() {
        return "ClouderaManagerV4Request{" +
                "repository=" + repository +
                ", products=" + products +
                ", enableAutoTls=" + enableAutoTls +
                '}';
    }
}
