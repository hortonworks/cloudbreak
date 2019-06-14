package com.sequenceiq.freeipa.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sequenceiq.freeipa.client.model.Ca;
import com.sequenceiq.freeipa.client.model.Group;
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

    public User userShow(String user) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of();
        return (User) invoke("user_show", flags, params, User.class).getResult();
    }

    public Optional<User> userFind(String user) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                "uid", user,
                "all", true
        );
        ParameterizedType type = TypeUtils
                .parameterize(List.class, User.class);
        List<User> foundUsers = (List<User>) invoke("user_find", flags, params, type).getResult();
        if (foundUsers.size() > 1) {
            LOGGER.error("Found more than 1 user with uid {}.", user);
        }
        if (foundUsers.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(foundUsers.get(0));
        }
    }

    public Set<User> userFindAll() throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0,
                "all", true
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, User.class);
        return (Set<User>) invoke("user_find", flags, params, type).getResult();
    }

    public User userAdd(String user, String firstName, String lastName, String password) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                "givenname", firstName,
                "sn", lastName,
                "userpassword", password,
                "setattr", "krbPasswordExpiration=20380101000000Z"
        );
        return (User) invoke("user_add", flags, params, User.class).getResult();
    }

    public void userSetPassword(String user, String password) throws FreeIpaClientException {
        // FreeIPA expires any password that is set by another user. Work around this by
        // performing a separate API call to set the password expiration into the future
        userMod(user, "userpassword", password);
        userMod(user, "setattr", "krbPasswordExpiration=20380101000000Z");
    }

    public User userMod(String user, String key, Object value) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                key, value
        );
        return (User) invoke("user_mod", flags, params, User.class).getResult();
    }

    public Group groupAdd(String group) throws FreeIpaClientException {
        List<String> flags = List.of(group);
        Map<String, Object> params = Map.of();
        return (Group) invoke("group_add", flags, params, Group.class).getResult();
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

    public RPCResponse<Object> groupRemoveMembers(String group, Set<String> users) throws FreeIpaClientException {
        List<String> flags = List.of(group);
        Map<String, Object> params = Map.of(
                "user", users
        );
        return invoke("group_remove_member", flags, params, Object.class);
    }

    public Set<Group> groupFindAll() throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0,
                "all", true
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Group.class);
        return (Set<Group>) invoke("group_find", flags, params, type).getResult();
    }

    public String getRootCertificate() throws FreeIpaClientException {
        List<String> flags = List.of("ipa");
        Map<String, Object> params = Map.of();
        RPCResponse<Ca> response = invoke("ca_show", flags, params, Ca.class);
        return response.getResult().getCertificate();
    }

    public void addPasswordExpirationPermission(String permission) throws FreeIpaClientException {
        List<String> flags = List.of(permission);
        Map<String, Object> params = Map.of(
                "attrs", List.of("krbpasswordexpiration"),
                "ipapermright", List.of("write"),
                "type", "user"
        );
        invoke("permission_add", flags, params, Object.class);
    }

    public void addPermissionToPrivilege(String privilege, String permission) throws FreeIpaClientException {
        List<String> flags = List.of(privilege);
        Map<String, Object> params = Map.of(
                "permission", List.of(permission)
        );
        invoke("privilege_add_permission", flags, params, Object.class);
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
            String message = String.format("Invoke FreeIpa failed: %s", throwable.getLocalizedMessage());
            LOGGER.error(message, throwable);
            throw new FreeIpaClientException(message, throwable);
        }
    }
}
