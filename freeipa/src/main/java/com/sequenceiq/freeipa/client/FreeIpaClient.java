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

    public RPCResponse<User> userAdd(String user, String firstName, String lastName) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                "givenname", firstName,
                "sn", lastName
        );
        return invoke("user_add", flags, params, User.class);
    }

    // TODO update password expiration aand unpack response
    //Request: {
    //    "id": 0,
    //    "method": "user_mod/1",
    //    "params": [
    //        [
    //            "dhan"
    //        ],
    //        {
    //            "krbpasswordexpiration": {
    //                "__datetime__": "20380101000000Z"
    //            },
    //            "userpassword": "adminadmin1",
    //            "version": "2.230"
    //        }
    //    ]
    //}
    //ipa: INFO: Response: {
    //    "error": null,
    //    "id": 0,
    //    "principal": "admin@IPATEST.LOCAL",
    //    "result": {
    //        "result": {
    //            "gidnumber": [
    //                "1866200061"
    //            ],
    //            "givenname": [
    //                "David"
    //            ],
    //            "has_keytab": true,
    //            "has_password": true,
    //            "homedirectory": [
    //                "/home/dhan"
    //            ],
    //            "krbcanonicalname": [
    //                "dhan@IPATEST.LOCAL"
    //            ],
    //            "krbpasswordexpiration": [
    //                {
    //                    "__datetime__": "20190520200340Z"
    //                }
    //            ],
    //            "krbprincipalname": [
    //                "dhan@IPATEST.LOCAL"
    //            ],
    //            "loginshell": [
    //                "/bin/sh"
    //            ],
    //            "mail": [
    //                "dhan@ipatest.local"
    //            ],
    //            "memberof_group": [
    //                "ipausers",
    //                "grp1"
    //            ],
    //            "nsaccountlock": false,
    //            "sn": [
    //                "Han"
    //            ],
    //            "uid": [
    //                "dhan"
    //            ],
    //            "uidnumber": [
    //                "1866200061"
    //            ]
    //        },
    //        "summary": "Modified user \"dhan\"",
    //        "value": "dhan"
    //    },
    //    "version": "4.6.4"
    //}
    public RPCResponse<Object> userSetPassword(String user, String password) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                "userpassword", password,
                // TODO: figure out password expiration story. for now, set expiration way in the future
                "setattr", "krbPasswordExpiration=20380101000000Z"
        );
        // TODO The password policy is applied automatically when the password is changed. We
        // should change the password policy so that we get reasonable password expirations (i.e., not
        // force the user to immediately change their passwords in ipa after changing their password
        // in through the API
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
