package com.sequenceiq.it.cloudbreak.newway.entity;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class AmbariEntity extends AbstractCloudbreakEntity<AmbariV2Request, Response, AmbariEntity> {

    public AmbariEntity(AmbariV2Request request, TestContext testContex) {
        super(request, testContex);
    }

    public AmbariEntity(TestContext testContex) {
        super(new AmbariV2Request(), testContex);
    }

    public AmbariEntity valid() {
        return withUserName("admin")
                .withPassword("admin1234")
                .withBlueprintName(MockCloudProvider.BLUEPRINT_DEFAULT_NAME)
                .withValidateRepositories(true)
                .withAmbariStackDetails(new AmbariStackDetailsJson());
    }

    public AmbariEntity withBlueprintId(Long blueprintId) {
        getRequest().setBlueprintId(blueprintId);
        return this;
    }

    public AmbariEntity withBlueprintName(String blueprintName) {
        getRequest().setBlueprintName(blueprintName);
        return this;
    }

    public AmbariEntity withGateway(GatewayJson gateway) {
        getRequest().setGateway(gateway);
        return this;
    }

    public AmbariEntity withEnableSecurity(Boolean enableSecurity) {
        getRequest().setEnableSecurity(enableSecurity);
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

    public AmbariEntity withKerberos(KerberosRequest kerberos) {
        getRequest().setKerberos(kerberos);
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

    public AmbariEntity withAmbariStackDetails(AmbariStackDetailsJson ambariStackDetails) {
        getRequest().setAmbariStackDetails(ambariStackDetails);
        return this;
    }

    public AmbariEntity withAmbariRepoDetailsJson(AmbariRepoDetailsJson ambariRepoDetailsJson) {
        getRequest().setAmbariRepoDetailsJson(ambariRepoDetailsJson);
        return this;
    }

    public AmbariEntity withAmbariDatabaseDetails(AmbariDatabaseDetailsJson ambariDatabaseDetails) {
        getRequest().setAmbariDatabaseDetails(ambariDatabaseDetails);
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
