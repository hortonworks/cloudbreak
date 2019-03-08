package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;

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
        String clusterDefinitionName = getTestParameter().getWithDefault(CommonCloudParameters.CLUSTER_DEFINITION_NAME,
                getCloudProvider().getDefaultClusterDefinitionName());
        return withUserName("admin")
                .withPassword("Admin123")
                .withClusterDefinitionName(clusterDefinitionName)
                .withAmbari(getTestContext().init(AmbariEntity.class));
    }

    public ClusterEntity withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public ClusterEntity withUserName(String userName) {
        getRequest().setUserName(userName);
        return this;
    }

    public ClusterEntity withPassword(String password) {
        getRequest().setPassword(password);
        return this;
    }

    public ClusterEntity withAmbari() {
        AmbariEntity ambari = getTestContext().get(AmbariEntity.class);
        return withAmbari(ambari);
    }

    public ClusterEntity withAmbari(String key) {
        AmbariEntity ambari = getTestContext().get(key);
        return withAmbari(ambari);
    }

    public ClusterEntity withAmbari(AmbariEntity ambariRequest) {
        getRequest().setAmbari(ambariRequest.getRequest());
        return this;
    }

    public ClusterEntity withClouderaManager() {
        ClouderaManagerEntity clouderaManager = getTestContext().get(ClouderaManagerEntity.class);
        return withClouderaManager(clouderaManager);
    }

    public ClusterEntity withClouderaManager(String key) {
        ClouderaManagerEntity clouderaManager = getTestContext().get(key);
        return withClouderaManager(clouderaManager);
    }

    public ClusterEntity withClouderaManager(ClouderaManagerEntity clouderaManagerEntity) {
        getRequest().setCm(clouderaManagerEntity.getRequest());
        getRequest().setAmbari(null);
        return this;
    }

    public ClusterEntity withLdapConfig() {
        LdapConfigTestDto ldap = getTestContext().get(LdapConfigTestDto.class);
        getRequest().setLdapName(ldap.getName());
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
        KerberosTestDto kerberosEntity = getTestContext().get(kerberos);
        getRequest().setKerberosName(kerberosEntity.getName());
        return this;
    }

    public ClusterEntity withKerberos() {
        return withKerberosKey(KerberosTestDto.class.getSimpleName());
    }

    public ClusterEntity withKerberos(String kerberos) {
        getRequest().setKerberosName(kerberos);
        return this;
    }

    public ClusterEntity withDatabase(String databaseName) {
        getRequest().getDatabases().add(databaseName);
        return this;
    }

    public ClusterEntity withClusterDefinitionName(String clusterDefinitionName) {
        getRequest().setClusterDefinitionName(clusterDefinitionName);
        return this;
    }

    public ClusterEntity withValidateClusterDefinition(Boolean validateClusterDefinition) {
        getRequest().setValidateClusterDefinition(validateClusterDefinition);
        return this;
    }
}

