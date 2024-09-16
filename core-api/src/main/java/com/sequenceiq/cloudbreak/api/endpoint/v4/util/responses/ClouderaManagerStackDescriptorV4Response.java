package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;

public class ClouderaManagerStackDescriptorV4Response {

    private String version;

    private String os;

    private ClouderaManagerStackRepoDetailsV4Response repository;

    private ClouderaManagerInfoV4Response clouderaManager;

    private List<ClouderaManagerProductV4Response> products = new ArrayList<>();

    private List<String> productDefinitions = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public ClouderaManagerStackRepoDetailsV4Response getRepository() {
        return repository;
    }

    public void setRepository(ClouderaManagerStackRepoDetailsV4Response repository) {
        this.repository = repository;
    }

    public ClouderaManagerInfoV4Response getClouderaManager() {
        return clouderaManager;
    }

    public void setClouderaManager(ClouderaManagerInfoV4Response clouderaManager) {
        this.clouderaManager = clouderaManager;
    }

    public List<ClouderaManagerProductV4Response> getProducts() {
        return products;
    }

    public void setProducts(List<ClouderaManagerProductV4Response> products) {
        this.products = products;
    }

    public List<String> getProductDefinitions() {
        return productDefinitions;
    }

    public void setProductDefinitions(List<String> productDefinitions) {
        this.productDefinitions = productDefinitions;
    }
}
