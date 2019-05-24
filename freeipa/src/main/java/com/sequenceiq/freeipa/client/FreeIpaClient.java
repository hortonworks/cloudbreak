package com.sequenceiq.freeipa.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public RPCResponse<User> userShow(String user) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of();
        return invoke("user_show", flags, params, User.class);
    }

    public RPCResponse<Set<User>> userFindAll() throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, User.class);
        return invoke("user_find", flags, params, type);
    }

    public RPCResponse<User> userAdd(String user, String firstName, String lastName, String password) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                "givenname", firstName,
                "sn", lastName,
                "userpassword", password,
                "setattr", "krbPasswordExpiration=20380101000000Z"
        );
        return invoke("user_add", flags, params, User.class);
    }

    public void userSetPassword(String user, String password) throws FreeIpaClientException {
        // FreeIPA expires any password that is set by another user. Work around this by
        // performing a separate API call to set the password expiration into the future
        userMod(user, "userpassword", password);
        userMod(user, "setattr", "krbPasswordExpiration=20380101000000Z");
    }

    public RPCResponse<User> userMod(String user, String key, Object value) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                key, value
        );
        return invoke("user_mod", flags, params, User.class);
    }

    // TODO unpack response into something meaningful
    //ipa: INFO: Response: {
    //    "error": null,
    //    "id": 0,
    //    "principal": "admin@IPATEST.LOCAL",
    //    "result": {
    //        "result": {
    //            "cn": [
    //                "testgrp1"
    //            ],
    //            "dn": "cn=testgrp1,cn=groups,cn=accounts,dc=ipatest,dc=local",
    //            "gidnumber": [
    //                "790600009"
    //            ],
    //            "ipauniqueid": [
    //                "a5e6ca24-774f-11e9-abeb-02864e36b814"
    //            ],
    //            "objectclass": [
    //                "top",
    //                "groupofnames",
    //                "nestedgroup",
    //                "ipausergroup",
    //                "ipaobject",
    //                "posixgroup"
    //            ]
    //        },
    //        "summary": "Added group \"testgrp1\"",
    //        "value": "testgrp1"
    //    },
    //    "version": "4.6.4"
    //}
    public RPCResponse<Object> groupAdd(String group) throws FreeIpaClientException {
        List<String> flags = List.of(group);
        Map<String, Object> params = Map.of();
        return invoke("group_add", flags, params, Object.class);
    }

    // TODO unpack response into something meaningful
    // NOTE: API may partially succeed/fail
    //ipa: INFO: Response: {
    //    "error": null,
    //    "id": 0,
    //    "principal": "admin@IPATEST.LOCAL",
    //    "result": {
    //        "completed": 1,
    //        "failed": {
    //            "member": {
    //                "group": [],
    //                "user": [
    //                    [
    //                        "joenobody",
    //                        "This entry is already a member"
    //                    ]
    //                ]
    //            }
    //        },
    //        "result": {
    //            "cn": [
    //                "testgrp1"
    //            ],
    //            "dn": "cn=testgrp1,cn=groups,cn=accounts,dc=ipatest,dc=local",
    //            "gidnumber": [
    //                "790600009"
    //            ],
    //            "member_user": [
    //                "joenobody",
    //                "dhan"
    //            ]
    //        }
    //    },
    //    "version": "4.6.4"
    //}
    // TODO the response to this API call not currently deserializable
    public RPCResponse<Object> groupAddMembers(String group, Set<String> users) throws FreeIpaClientException {
        List<String> flags = List.of(group);
        Map<String, Object> params = Map.of(
                "user", users
        );
        return invoke("group_add_member", flags, params, Object.class);
    }

    public <T> RPCResponse<T> invoke(String method, List<String> flags, Map<String, Object> params, Type resultType) throws FreeIpaClientException {
        Map<String, Object> parameterMap = new HashMap<>();
        if (params != null && !params.isEmpty()) {
            parameterMap.putAll(params);
        }
        parameterMap.put("version", apiVersion);

        LOGGER.debug("Issuing JSON-RPC request:\n\n method: {}\n flags: {}\n params: {}\n", method, flags, parameterMap);
        ParameterizedType type = TypeUtils
                .parameterize(RPCResponse.class, resultType);
        try {
            RPCResponse<T> response = (RPCResponse<T>) jsonRpcHttpClient.invoke(method, List.of(flags, parameterMap), type);
            LOGGER.debug("Response object: {}", response);
            return response;
        } catch (Throwable throwable) {
            LOGGER.error("Invoke FreeIpa failed", throwable);
            throw new FreeIpaClientException("Invoke FreeIpa failed", throwable);
        }
    }
}
