package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;

public class Ambari extends Entity  {
    public static final String AMBARI_REQUEST = "AMBARI_REQUEST";

    private AmbariV2Request request;

    Ambari(String newId) {
        super(newId);
        this.request = new AmbariV2Request();
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
        request.setBlueprintId(blueprintId);
        return this;
    }

    public Ambari withBlueprintName(String name) {
        request.setBlueprintName(name);
        return this;
    }

    public Ambari withAmbariRepoDetailsJson(AmbariRepoDetailsJson ambariRepoDetailsJson) {
        request.setAmbariRepoDetailsJson(ambariRepoDetailsJson);
        return this;
    }

    public Ambari withAmbariStackDetails(AmbariStackDetailsJson ambariStackDetailsJson) {
        request.setAmbariStackDetails(ambariStackDetailsJson);
        return this;
    }

    public Ambari withConfigStrategy(ConfigStrategy configStrategy) {
        request.setConfigStrategy(configStrategy);
        return this;
    }

    public Ambari withConnectedCluster(ConnectedClusterRequest connectedClusterRequest) {
        request.setConnectedCluster(connectedClusterRequest);
        return this;
    }

    public Ambari withGateway(GatewayJson gatewayJson) {
        request.setGateway(gatewayJson);
        return this;
    }

    public Ambari withKerberos(String kerberosConfigName) {
        request.setKerberosConfigName(kerberosConfigName);
        return this;
    }

    public Ambari withPassword(String password) {
        request.setPassword(password);
        return this;
    }

    public Ambari withUsername(String username) {
        request.setUserName(username);
        return this;
    }

    public Ambari withValidateBlueprint(boolean validate) {
        request.setValidateBlueprint(validate);
        return this;
    }

    public static Ambari request(String key) {
        return new Ambari(key);
    }

    public static Ambari request() {
        return new Ambari();
    }
}

