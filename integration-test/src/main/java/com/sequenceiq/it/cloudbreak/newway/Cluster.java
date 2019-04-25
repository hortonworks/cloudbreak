package com.sequenceiq.it.cloudbreak.newway;

import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.it.IntegrationTestContext;

public class Cluster extends Entity {

    private static final String CLUSTER_REQUEST = "CLUSTER_REQUEST";

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    private ClusterV4Request request;

    private String name;

    Cluster(String newId) {
        super(newId);
        request = new ClusterV4Request();
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

    public ClusterV4Request getRequest() {
        return request;
    }

    public void setRequest(ClusterV4Request request) {
        this.request = request;
    }

    public Cluster withName(String name) {
        request.setName(name);
        this.name = name;
        return this;
    }

    public Cluster withAmbariRequest(AmbariV4Request ambariRequest) {
        request.setAmbari(ambariRequest);
        return this;
    }

    public Cluster withKerberos(String kerberosConfigName) {
        request.setKerberosName(kerberosConfigName);
        return this;
    }

    public Cluster withLdapConfigName(String ldap) {
        request.setLdapName(ldap);
        return this;
    }

    public Cluster withProxyConfigName(String proxy) {
        request.setProxyName(proxy);
        return this;
    }

    public Cluster withRdsConfigNames(Set<String> names) {
        request.setDatabases(names);
        return this;
    }

    public Cluster withUsername(String username) {
        request.setUserName(username);
        return this;
    }

    public Cluster withPassword(String password) {
        request.setPassword(password);
        return this;
    }

    public Cluster withCloudStorage(CloudStorageV4Request cloudStorage) {
        request.setCloudStorage(cloudStorage);
        return this;
    }

    public Cluster withBlueprintName(String blueprintName) {
        request.setBlueprintName(blueprintName);
        return this;
    }

    public Cluster withValidateBlueprint(Boolean validateBlueprint) {
        request.setValidateBlueprint(validateBlueprint);
        return this;
    }

    public static Function<IntegrationTestContext, Cluster> getTestContextCluster(String key) {
        return testContext -> testContext.getContextParam(key, Cluster.class);
    }

    public static Function<IntegrationTestContext, Cluster> getTestContextCluster() {
        return getTestContextCluster(CLUSTER_REQUEST);
    }

    public static Function<IntegrationTestContext, Cluster> getNewCluster() {
        return testContext -> new Cluster();
    }

    public static Cluster request(String key) {
        return new Cluster(key);
    }

    public static Cluster request() {
        return new Cluster();
    }
}

