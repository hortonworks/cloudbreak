package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager;

import java.util.List;
import java.util.StringJoiner;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClouderaManagerV4Response implements JsonEntity {

    @Valid
    @ApiModelProperty(ClusterModelDescription.CM_REPO_DETAILS)
    private ClouderaManagerRepositoryV4Response repository;

    @Valid
    @ApiModelProperty(ClusterModelDescription.CM_PRODUCT_DETAILS)
    private List<ClouderaManagerProductV4Response> products;

    @ApiModelProperty(ClusterModelDescription.CM_ENABLE_AUTOTLS)
    private Boolean enableAutoTls = Boolean.FALSE;

    @ApiModelProperty(ClusterModelDescription.CM_ENABLE_AUTOTLS)
    private Boolean enableCMHA = Boolean.FALSE;

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

    public Boolean getEnableAutoTls() {
        return enableAutoTls;
    }

    public void setEnableAutoTls(Boolean enableAutoTls) {
        this.enableAutoTls = enableAutoTls == null ? Boolean.FALSE : enableAutoTls;
    }

    public void setEnableCMHA(Boolean enableCMHA) {
        this.enableCMHA = enableCMHA;
    }

    public ClouderaManagerV4Response withRepository(ClouderaManagerRepositoryV4Response repository) {
        setRepository(repository);
        return this;
    }

    public ClouderaManagerV4Response withProducts(List<ClouderaManagerProductV4Response> products) {
        setProducts(products);
        return this;
    }

    public ClouderaManagerV4Response withTlsEnabled(Boolean enabled) {
        setEnableAutoTls(enabled);
        return this;
    }

    public ClouderaManagerV4Response withCMHAEnabled(Boolean enabled) {
        setEnableCMHA(enabled);
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClouderaManagerV4Response.class.getSimpleName() + "[", "]")
                .add("repository=" + repository)
                .add("products=" + products)
                .add("enableAutoTls=" + enableAutoTls)
                .add("enableCMHA=" + enableCMHA)
                .toString();
    }
}
