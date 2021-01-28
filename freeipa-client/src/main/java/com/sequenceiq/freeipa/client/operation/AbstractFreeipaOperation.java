package com.sequenceiq.freeipa.client.operation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.cloudbreak.client.RPCResponse;

public abstract class AbstractFreeipaOperation<R> {

    public abstract String getOperationName();

    public abstract Optional<R> invoke(FreeIpaClient freeipaClient) throws FreeIpaClientException;

    public Map<String, Object> getOperationParamsForBatchCall() {
        Map<String, Object> operationParams = Maps.newHashMap();
        operationParams.put("method", getOperationName());
        operationParams.put("params", List.of(getFlags(), getParams()));
        return operationParams;
    }

    public RPCResponse rpcInvoke(FreeIpaClient freeipaClient, Class<R> clazz) throws FreeIpaClientException {
        return freeipaClient.invoke(getOperationName(), getFlags(), getParams(), clazz);
    }

    protected R invoke(FreeIpaClient freeipaClient, Class<R> clazz) throws FreeIpaClientException {
        return (R) rpcInvoke(freeipaClient, clazz).getResult();
    }

    protected List<Object> getFlags() {
        return List.of();
    }

    protected Map<String, Object> getParams() {
        return Map.of();
    }

    protected Map sensitiveMap() {
        return new HashMap() {
            @Override
            public String toString() {
                return "__sensitive__";
            }
        };
    }

    protected Map sensitiveMap(Object key, Object value) {
        Map result = sensitiveMap();
        result.put(key, value);
        return result;
    }
}
