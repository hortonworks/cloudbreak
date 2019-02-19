package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;

public class Ambari extends Entity  {

    private static final String AMBARI_REQUEST = "AMBARI_REQUEST";

    private AmbariV4Request request;

    Ambari(String newId) {
        super(newId);
        this.request = new AmbariV4Request();
    }

    Ambari() {
        this(AMBARI_REQUEST);
    }

    public AmbariV4Request getRequest() {
        return request;
    }

    public void setRequest(AmbariV4Request request) {
        this.request = request;
    }

    public Ambari withBlueprintName(String name) {
        request.setClusterDefinitionName(name);
        return this;
    }

    public Ambari withAmbariRepoDetailsJson(AmbariRepositoryV4Request ambariRepoDetailsJson) {
        request.setRepository(ambariRepoDetailsJson);
        return this;
    }

    public Ambari withAmbariStackDetails(StackRepositoryV4Request ambariStackDetailsJson) {
        request.setStackRepository(ambariStackDetailsJson);
        return this;
    }

    public Ambari withConfigStrategy(ConfigStrategy configStrategy) {
        request.setConfigStrategy(configStrategy);
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

    public Ambari withValidateClusterDefinition(boolean validate) {
        request.setValidateClusterDefinition(validate);
        return this;
    }

    public static Ambari request(String key) {
        return new Ambari(key);
    }

    public static Ambari request() {
        return new Ambari();
    }
}

