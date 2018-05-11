package com.sequenceiq.it.cloudbreak.newway;

import java.util.Set;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.it.IntegrationTestContext;

public class Cluster extends Entity  {
    public static final String CLUSTER_REQUEST = "CLUSTER_REQUEST";

    private ClusterV2Request request;

    private String name;

    Cluster(String newId) {
        super(newId);
        this.request = new ClusterV2Request();
    }

    Cluster() {
        this(CLUSTER_REQUEST);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClusterV2Request getRequest() {
        return request;
    }

    public void setRequest(ClusterV2Request request) {
        this.request = request;
    }

    public Cluster withName(String name) {
        request.setName(name);
        this.name = name;
        return this;
    }

    public Cluster withAmbariRequest(AmbariV2Request ambariRequest) {
        request.setAmbari(ambariRequest);
        return this;
    }

    public Cluster withEmailNeeded(Boolean aBoolean) {
        request.setEmailNeeded(aBoolean);
        return this;
    }

    public Cluster withEmailTo(String parameters) {
        request.setEmailTo(parameters);
        return this;
    }

    public Cluster withExecutorType(ExecutorType executorType) {
        request.setExecutorType(executorType);
        return this;
    }

    public Cluster withFileSystem(FileSystemRequest fileSystemRequest) {
        request.setFileSystem(fileSystemRequest);
        return this;
    }

    public Cluster withLdapConfigName(String ldap) {
        request.setLdapConfigName(ldap);
        return this;
    }

    public Cluster withProxyConfigName(String proxy) {
        request.setProxyName(proxy);
        return this;
    }

    public Cluster withRdsConfigNames(Set<String> names) {
        request.setRdsConfigNames(names);
        return this;
    }

    public static Function<IntegrationTestContext, Cluster> getTestContextCluster(String key) {
        return (testContext) -> testContext.getContextParam(key, Cluster.class);
    }

    public static Function<IntegrationTestContext, Cluster> getTestContextCluster() {
        return getTestContextCluster(CLUSTER_REQUEST);
    }

    public static Function<IntegrationTestContext, Cluster> getNewCluster() {
        return (testContext) -> new Cluster();
    }

    public static Cluster request(String key) {
        return new Cluster(key);
    }

    public static Cluster request() {
        return new Cluster();
    }
}

