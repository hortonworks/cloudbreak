package com.sequenceiq.it.mock.restito;

import com.ecwid.consul.json.GsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Call;
import com.xebialabs.restito.semantics.Condition;
import com.xebialabs.restito.semantics.Stub;

public abstract class RestitoStub extends Stub {

    public static final String DOCKER_API_ROOT = "/docker/v1.18";
    public static final String SWARM_API_ROOT = "/swarm/v1.18";
    public static final String CONSUL_API_ROOT = "/v1";
    public static final String AMBARI_API_ROOT = "/api/v1";
    private static ObjectMapper objectMapper = new ObjectMapper();
    private boolean initialized;

    public RestitoStub() {
        super(Condition.alwaysTrue(), Action.noop());
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public String convertToJson(Object object) {
        return GsonFactory.getGson().toJson(object);
    }

    public abstract Condition getCondition();

    public abstract Action getAction();

    @Override
    public boolean isApplicable(Call call) {
        if (!initialized) {
            alsoWhen(getCondition());
            alsoWhat(getAction());
            initialized = true;
        }
        return super.isApplicable(call);
    }
}
