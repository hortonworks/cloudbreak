package com.sequenceiq.it.cloudbreak.newway.dto;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;

@Prototype
public class ClusterTestDto extends AbstractCloudbreakTestDto<ClusterV4Request, ClusterV4Response, ClusterTestDto> {

    private static final String CLUSTER_REQUEST = "CLUSTER_REQUEST";

    public ClusterTestDto(ClusterV4Request request, TestContext testContex) {
        super(request, testContex);
    }

    public ClusterTestDto(TestContext testContex) {
        super(new ClusterV4Request(), testContex);
    }

    public ClusterTestDto() {
        super(ClusterTestDto.class.getSimpleName());
    }

    public ClusterTestDto valid() {
        return withAmbari(getTestContext().init(AmbariTestDto.class))
                .getCloudProvider().cluster(this);
    }

    public ClusterTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public ClusterTestDto withUserName(String userName) {
        getRequest().setUserName(userName);
        return this;
    }

    public ClusterTestDto withPassword(String password) {
        getRequest().setPassword(password);
        return this;
    }

    public ClusterTestDto withAmbari() {
        AmbariTestDto ambari = getTestContext().get(AmbariTestDto.class);
        return withAmbari(ambari);
    }

    public ClusterTestDto withAmbari(String key) {
        AmbariTestDto ambari = getTestContext().get(key);
        return withAmbari(ambari);
    }

    public ClusterTestDto withAmbari(AmbariTestDto ambariRequest) {
        getRequest().setAmbari(ambariRequest.getRequest());
        return this;
    }

    public ClusterTestDto withClouderaManager() {
        ClouderaManagerTestDto clouderaManager = getTestContext().get(ClouderaManagerTestDto.class);
        return withClouderaManager(clouderaManager);
    }

    public ClusterTestDto withClouderaManager(String key) {
        ClouderaManagerTestDto clouderaManager = getTestContext().get(key);
        return withClouderaManager(clouderaManager);
    }

    public ClusterTestDto withClouderaManager(ClouderaManagerTestDto clouderaManagerTestDto) {
        getRequest().setCm(clouderaManagerTestDto.getRequest());
        getRequest().setAmbari(null);
        return this;
    }

    public ClusterTestDto withLdapConfig() {
        LdapTestDto ldap = getTestContext().get(LdapTestDto.class);
        getRequest().setLdapName(ldap.getName());
        return this;
    }

    public ClusterTestDto withLdapConfigName(String ldap) {
        getRequest().setLdapName(ldap);
        return this;
    }

    public ClusterTestDto withProxyConfigName(String proxy) {
        getRequest().setProxyName(proxy);
        return this;
    }

    public ClusterTestDto withRdsConfigNames(String... names) {
        getRequest().setDatabases(newHashSet(names));
        return this;
    }

    public ClusterTestDto withRdsConfigNames(Set<String> names) {
        getRequest().setDatabases(names);
        return this;
    }

    public ClusterTestDto withCloudStorage(CloudStorageV4Request cloudStorage) {
        getRequest().setCloudStorage(cloudStorage);
        return this;
    }

    public ClusterTestDto withGateway(String key) {
        GatewayTestDto gatewayEntity = getTestContext().get(key);
        getRequest().setGateway(gatewayEntity.getRequest());
        return this;
    }

    public ClusterTestDto withKerberosKey(String kerberos) {
        KerberosTestDto kerberosEntity = getTestContext().get(kerberos);
        getRequest().setKerberosName(kerberosEntity.getName());
        return this;
    }

    public ClusterTestDto withKerberos() {
        return withKerberosKey(KerberosTestDto.class.getSimpleName());
    }

    public ClusterTestDto withKerberos(String kerberos) {
        getRequest().setKerberosName(kerberos);
        return this;
    }

    public ClusterTestDto withDatabase(String databaseName) {
        getRequest().getDatabases().add(databaseName);
        return this;
    }

    public ClusterTestDto withBlueprintName(String blueprintName) {
        getRequest().setBlueprintName(blueprintName);
        return this;
    }

    public ClusterTestDto withValidateBlueprint(Boolean validateBlueprint) {
        getRequest().setValidateBlueprint(validateBlueprint);
        return this;
    }
}

