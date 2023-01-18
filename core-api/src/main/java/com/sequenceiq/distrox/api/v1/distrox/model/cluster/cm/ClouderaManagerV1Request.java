package com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.product.ClouderaManagerProductV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository.ClouderaManagerRepositoryV1Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClouderaManagerV1Request implements JsonEntity {

    @Valid
    @Schema(description = ClusterModelDescription.CM_REPO_DETAILS)
    private ClouderaManagerRepositoryV1Request repository;

    @Valid
    @Schema(description = ClusterModelDescription.CM_PRODUCT_DETAILS)
    private List<ClouderaManagerProductV1Request> products;

    @Schema(description = ClusterModelDescription.CM_ENABLE_AUTOTLS, required = true)
    private Boolean enableAutoTls = Boolean.TRUE;

    public ClouderaManagerRepositoryV1Request getRepository() {
        return repository;
    }

    public void setRepository(ClouderaManagerRepositoryV1Request repository) {
        this.repository = repository;
    }

    public List<ClouderaManagerProductV1Request> getProducts() {
        return products;
    }

    public void setProducts(List<ClouderaManagerProductV1Request> products) {
        this.products = products;
    }

    public Boolean getEnableAutoTls() {
        return enableAutoTls;
    }

    public void setEnableAutoTls(Boolean enableAutoTls) {
        this.enableAutoTls = enableAutoTls;
    }

    public ClouderaManagerV1Request withRepository(ClouderaManagerRepositoryV1Request repository) {
        setRepository(repository);
        return this;
    }

    public ClouderaManagerV1Request withProducts(List<ClouderaManagerProductV1Request> products) {
        setProducts(products);
        return this;
    }

    public ClouderaManagerV1Request withEnableAutoTls(Boolean enableAutoTls) {
        setEnableAutoTls(enableAutoTls);
        return this;
    }
}
