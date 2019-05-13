package com.sequenceiq.freeipa.client;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.User;

public class FreeIpaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClient.class);

    private static final String DEFAULT_API_VERSION = "2.213";

    private JsonRpcHttpClient jsonRpcHttpClient;

    private String apiVersion;

    public FreeIpaClient(JsonRpcHttpClient jsonRpcHttpClient) {
        this(jsonRpcHttpClient, DEFAULT_API_VERSION);
    }

    public FreeIpaClient(JsonRpcHttpClient jsonRpcHttpClient, String apiVersion) {
        this.jsonRpcHttpClient = jsonRpcHttpClient;
        this.apiVersion = apiVersion;
    }

    public RPCResponse<User> userShow(String user, Map<String, String> params) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        return invoke("user_show", flags, params, User.class);
    }

    public <T> RPCResponse<T> invoke(String method, List<String> flags, Map<String, String> params, Class<T> resultType) throws FreeIpaClientException {
        Map<String, String> parameterMap = new HashMap<>();
        if (params != null && !params.isEmpty()) {
            parameterMap.putAll(params);
        }
        parameterMap.put("version", apiVersion);

        LOGGER.debug("Issuing JSON-RPC request:\n\n method: {}\n flags: {}\n params: {}\n", method, flags, parameterMap);
        ParameterizedType type = TypeUtils
                .parameterize(RPCResponse.class, resultType);
        try {
            LOGGER.debug("User object: {}", jsonRpcHttpClient.invoke(method, List.of(flags, parameterMap), Object.class));
            return (RPCResponse<T>) jsonRpcHttpClient.invoke(method, List.of(flags, parameterMap), type);
        } catch (Throwable throwable) {
            LOGGER.error("Invoke FreeIpa failed", throwable);
            throw new FreeIpaClientException("Invoke FreeIpa failed", throwable);
        }
    }
}
