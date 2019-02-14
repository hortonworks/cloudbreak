package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClusterEntity extends AbstractCloudbreakEntity<ClusterV4Request, ClusterV4Response, ClusterEntity> {

    private static final String CLUSTER_REQUEST = "CLUSTER_REQUEST";

    public ClusterEntity(ClusterV4Request request, TestContext testContex) {
        super(request, testContex);
    }

    public ClusterEntity(TestContext testContex) {
        super(new ClusterV4Request(), testContex);
    }

    public ClusterEntity() {
        super(ClusterEntity.class.getSimpleName());
    }

    public ClusterEntity valid() {
        return withAmbari(getTestContext().init(AmbariEntity.class));
    }

    public ClusterEntity withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public ClusterEntity withAmbari(String key) {
        AmbariEntity ambari = getTestContext().get(key);
        return withAmbari(ambari);
    }

    public ClusterEntity withAmbari(AmbariEntity ambariRequest) {
        getRequest().setAmbari(ambariRequest.getRequest());
        return this;
    }

    public ClusterEntity withLdapConfigName(String ldap) {
        getRequest().setLdapName(ldap);
        return this;
    }

    public ClusterEntity withProxyConfigName(String proxy) {
        getRequest().setProxyName(proxy);
        return this;
    }

    public ClusterEntity withRdsConfigNames(String... names) {
        getRequest().setDatabases(newHashSet(names));
        return this;
    }

    public ClusterEntity withRdsConfigNames(Set<String> names) {
        getRequest().setDatabases(names);
        return this;
    }

    public ClusterEntity withCloudStorage(CloudStorageV4Request cloudStorage) {
        getRequest().setCloudStorage(cloudStorage);
        return this;
    }

    public ClusterEntity withGateway(String key) {
        GatewayEntity gatewayEntity = getTestContext().get(key);
        getRequest().setGateway(gatewayEntity.getRequest());
        return this;
    }

    public ClusterEntity withKerberosKey(String kerberos) {
        KerberosEntity kerberosEntity = getTestContext().get(kerberos);
        getRequest().setKerberosName(kerberosEntity.getName());
        return this;
    }

    public ClusterEntity withKerberos() {
        return withKerberosKey(KerberosEntity.class.getSimpleName());
    }

    public ClusterEntity withKerberos(String kerberos) {
        getRequest().setKerberosName(kerberos);
        return this;
    }

    public ClusterEntity withDatabase(String databaseName) {
        getRequest().getDatabases().add(databaseName);
        return this;
    }
}

