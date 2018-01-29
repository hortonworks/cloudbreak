package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;

import java.util.Set;

public class Ambari extends Entity  {
    public static final String AMBARI_REQUEST = "AMBARI_REQUEST";

    private AmbariV2Request request;

    Ambari(String newId) {
        super(newId);
        setRequest(new AmbariV2Request());
    }

    Ambari() {
        this(AMBARI_REQUEST);
    }

    public AmbariV2Request getRequest() {
        return request;
    }

    public void setRequest(AmbariV2Request request) {
        this.request = request;
    }

    public Ambari withBlueprintId(Long blueprintId) {
        getRequest().setBlueprintId(blueprintId);
        return this;
    }

    public Ambari withBlueprintName(String name) {
        getRequest().setBlueprintName(name);
        return this;
    }

    public Ambari withBlueprintInputs(Set<BlueprintInputJson> inputs) {
        getRequest().setBlueprintInputs(inputs);
        return this;
    }

    public Ambari withBlueprintCustomProperties(String properties) {
        getRequest().setBlueprintCustomProperties(properties);
        return this;
    }

    public Ambari withAmbariDatabaseDetails(AmbariDatabaseDetailsJson ambariDatabaseDetailsJson) {
        getRequest().setAmbariDatabaseDetails(ambariDatabaseDetailsJson);
        return this;
    }

    public Ambari withAmbariRepoDetailsJson(AmbariRepoDetailsJson ambariRepoDetailsJson) {
        getRequest().setAmbariRepoDetailsJson(ambariRepoDetailsJson);
        return this;
    }

    public Ambari withAmbariStackDetails(AmbariStackDetailsJson ambariStackDetailsJson) {
        getRequest().setAmbariStackDetails(ambariStackDetailsJson);
        return this;
    }

    public Ambari withConfigStrategy(ConfigStrategy configStrategy) {
        getRequest().setConfigStrategy(configStrategy);
        return this;
    }

    public Ambari withConnectedCluster(ConnectedClusterRequest connectedClusterRequest) {
        getRequest().setConnectedCluster(connectedClusterRequest);
        return this;
    }

    public Ambari withEnableSecurity(boolean enableSecurity) {
        getRequest().setEnableSecurity(enableSecurity);
        return this;
    }

    public Ambari withGateway(GatewayJson gatewayJson) {
        getRequest().setGateway(gatewayJson);
        return this;
    }

    public Ambari withKerberos(KerberosRequest kerberosRequest) {
        getRequest().setKerberos(kerberosRequest);
        return this;
    }

    public Ambari withPassword(String password) {
        getRequest().setPassword(password);
        return this;
    }

    public Ambari withUsername(String username) {
        getRequest().setUserName(username);
        return this;
    }

    public Ambari withValidateBlueprint(boolean validate) {
        getRequest().setValidateBlueprint(validate);
        return this;
    }

    public static Ambari request(String key) {
        return new Ambari(key);
    }

    public static Ambari request() {
        return new Ambari();
    }
}

