package com.sequenceiq.it.cloudbreak.newway;

import java.util.Set;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.RdsConfigs;
import com.sequenceiq.it.IntegrationTestContext;

public class Cluster extends Entity  {
    public static final String CLUSTER_REQUEST = "CLUSTER_REQUEST";

    private ClusterV2Request request;

    private String name;

    Cluster(String newId) {
        super(newId);
        setRequest(new ClusterV2Request());
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
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public Cluster withAmbariRequest(AmbariV2Request ambariRequest) {
        getRequest().setAmbari(ambariRequest);
        return this;
    }

    public Cluster withEmailNeeded(Boolean aBoolean) {
        getRequest().setEmailNeeded(aBoolean);
        return this;
    }

    public Cluster withEmailTo(String parameters) {
        getRequest().setEmailTo(parameters);
        return this;
    }

    public Cluster withExecutorType(ExecutorType executorType) {
        getRequest().setExecutorType(executorType);
        return this;
    }

    public Cluster withFileSystem(FileSystemRequest fileSystemRequest) {
        getRequest().setFileSystem(fileSystemRequest);
        return this;
    }

    public Cluster withLdapConfigName(String ldap) {
        getRequest().setLdapConfigName(ldap);
        return this;
    }

    public Cluster withRdsConfigIds(Set<Long> ids) {
        if (getRequest().getRdsConfigs() == null) {
            getRequest().setRdsConfigs(new RdsConfigs());
        }
        getRequest().getRdsConfigs().setIds(ids);
        return this;
    }

    public Cluster withRdsConfigJsons(Set<RDSConfigRequest> rdsConfigRequests) {
        if (getRequest().getRdsConfigs() == null) {
            getRequest().setRdsConfigs(new RdsConfigs());
        }
        getRequest().getRdsConfigs().setConfigs(rdsConfigRequests);
        return this;
    }

    public static Function<IntegrationTestContext, Cluster> getTestContextCluster(String key) {
        return (testContext)->testContext.getContextParam(key, Cluster.class);
    }

    public static Function<IntegrationTestContext, Cluster> getTestContextCluster() {
        return getTestContextCluster(CLUSTER_REQUEST);
    }

    public static Function<IntegrationTestContext, Cluster> getNewCluster() {
        return (testContext)->new Cluster();
    }

    public static Cluster request(String key) {
        return new Cluster(key);
    }

    public static Cluster request() {
        return new Cluster();
    }
}

