package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClusterEntity extends AbstractCloudbreakEntity<ClusterV2Request, ClusterResponse, ClusterEntity> {

    private static final String CLUSTER_REQUEST = "CLUSTER_REQUEST";

    public ClusterEntity(ClusterV2Request request, TestContext testContex) {
        super(request, testContex);
    }

    public ClusterEntity(TestContext testContex) {
        super(new ClusterV2Request(), testContex);
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

    public ClusterEntity withExecutorType(ExecutorType executorType) {
        getRequest().setExecutorType(executorType);
        return this;
    }

    public ClusterEntity withLdapConfigName(String ldap) {
        getRequest().setLdapConfigName(ldap);
        return this;
    }

    public ClusterEntity withProxyConfigName(String proxy) {
        getRequest().setProxyName(proxy);
        return this;
    }

    public ClusterEntity withRdsConfigNames(String... names) {
        getRequest().setRdsConfigNames(newHashSet(names));
        return this;
    }

    public ClusterEntity withRdsConfigNames(Set<String> names) {
        getRequest().setRdsConfigNames(names);
        return this;
    }

    public ClusterEntity withCloudStorage(CloudStorageRequest cloudStorage) {
        getRequest().setCloudStorage(cloudStorage);
        return this;
    }

    public ClusterEntity withSharedService(String datalakeClusterName) {
        SharedServiceRequest sharedServiceRequest = new SharedServiceRequest();
        sharedServiceRequest.setSharedCluster(datalakeClusterName);
        getRequest().setSharedService(sharedServiceRequest);
        return this;
    }
}

