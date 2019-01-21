package com.sequenceiq.it.cloudbreak.newway.entity;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class AmbariEntity extends AbstractCloudbreakEntity<AmbariV2Request, Response, AmbariEntity> {

    public AmbariEntity(TestContext testContex) {
        super(new AmbariV2Request(), testContex);
    }

    public AmbariEntity() {
        super(AmbariEntity.class.getSimpleName().toUpperCase());
    }

    public AmbariEntity valid() {
        return withUserName("admin")
                .withPassword("admin1234")
                .withBlueprintName(MockCloudProvider.BLUEPRINT_DEFAULT_NAME)
                .withValidateRepositories(true);
    }

    public AmbariEntity withBlueprintId(Long blueprintId) {
        getRequest().setBlueprintId(blueprintId);
        return this;
    }

    public AmbariEntity withBlueprintName(String blueprintName) {
        getRequest().setBlueprintName(blueprintName);
        return this;
    }

    public AmbariEntity withGateway(String key) {
        GatewayEntity gatewayEntity = getTestContext().get(key);
        getRequest().setGateway(gatewayEntity.getRequest());
        return this;
    }

    public AmbariEntity withUserName(String userName) {
        getRequest().setUserName(userName);
        return this;
    }

    public AmbariEntity withPassword(String password) {
        getRequest().setPassword(password);
        return this;
    }

    public AmbariEntity withKerberos(String kerberos) {
        getRequest().setKerberosConfigName(kerberos);
        return this;
    }

    public AmbariEntity withValidateBlueprint(Boolean validateBlueprint) {
        getRequest().setValidateBlueprint(validateBlueprint);
        return this;
    }

    public AmbariEntity withValidateRepositories(Boolean validateRepositories) {
        getRequest().setValidateRepositories(validateRepositories);
        return this;
    }

    public AmbariEntity withAmbariStackDetails(String key) {
        AmbariStackDetailsEntity ambariStack = getTestContext().get(key);
        return withAmbariStackDetails(ambariStack);
    }

    public AmbariEntity withAmbariStackDetails(AmbariStackDetailsEntity ambariStackDetails) {
        getRequest().setAmbariStackDetails(ambariStackDetails.getRequest());
        return this;
    }

    public AmbariEntity withAmbariRepoDetails(String key) {
        AmbariRepoDetailsEntity ambariRepo = getTestContext().get(key);
        return withAmbariRepoDetails(ambariRepo);
    }

    public AmbariEntity withAmbariRepoDetails(AmbariRepoDetailsEntity ambariRepoDetailsJson) {
        getRequest().setAmbariRepoDetailsJson(ambariRepoDetailsJson.getRequest());
        return this;
    }

    public AmbariEntity withConfigStrategy(ConfigStrategy configStrategy) {
        getRequest().setConfigStrategy(configStrategy);
        return this;
    }

    public AmbariEntity withConnectedCluster(ConnectedClusterRequest connectedCluster) {
        getRequest().setConnectedCluster(connectedCluster);
        return this;
    }

    public AmbariEntity withAmbariSecurityMasterKey(String ambariSecurityMasterKey) {
        getRequest().setAmbariSecurityMasterKey(ambariSecurityMasterKey);
        return this;
    }

}
