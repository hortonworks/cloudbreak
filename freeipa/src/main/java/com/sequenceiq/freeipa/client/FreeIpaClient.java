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
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.client.model.DnsZoneList;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.client.model.User;

public class FreeIpaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClient.class);

    private static final String DEFAULT_API_VERSION = "2.230";

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

    public Set<Role> findAllRole() throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Role.class);
        return (Set<Role>) invoke("role_find", flags, params, type).getResult();
    }

    public Set<Host> findAllHost() throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Host.class);
        return (Set<Host>) invoke("host_find", flags, params, type).getResult();
    }

    public Host deleteHost(String fqdn) throws FreeIpaClientException {
        Map<String, Object> params = Map.of("updatedns", true);
        return (Host) invoke("host_del", List.of(fqdn), params, Host.class).getResult();
    }

    public RPCResponse<Host> addHost(String fqdn) throws FreeIpaClientException {
        RPCResponse<Host> response = null;
        //TODO Implement as part of CDPSDX-584
        return response;
    }

    public User deleteUser(String userUid) throws FreeIpaClientException {
        List<String> flags = List.of(userUid);
        Map<String, Object> params = Map.of();
        return (User) invoke("user_del", flags, params, User.class).getResult();
    }

    public Role deleteRole(String roleName) throws FreeIpaClientException {
        List<String> flags = List.of(roleName);
        Map<String, Object> params = Map.of();
        return (Role) invoke("role_del", flags, params, Role.class).getResult();
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

    public Set<Permission> findPermission(String permission) throws FreeIpaClientException {
        Map<String, Object> params = Map.of("cn", permission);
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Permission.class);
        return (Set<Permission>) invoke("permission_find", List.of(), params, type).getResult();
    }

    public int getUsernameLength() throws FreeIpaClientException {
        Config config = (Config) invoke("config_show", List.of(), Map.of(), Config.class).getResult();
        return config.getIpamaxusernamelength();
    }

    public void setUsernameLength(int length) throws FreeIpaClientException {
        Map<String, Object> params = Map.of("ipamaxusernamelength", length);
        invoke("config_mod", List.of(), params, Object.class);
    }

    public void addPermissionToPrivilege(String privilege, String permission) throws FreeIpaClientException {
        List<String> flags = List.of(privilege);
        Map<String, Object> params = Map.of(
                "permission", List.of(permission)
        );
        invoke("privilege_add_permission", flags, params, Object.class);
    }

    public Set<DnsZoneList> findAllDnsZone() throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "pkey_only", true,
                "raw", true
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, DnsZoneList.class);
        return (Set<DnsZoneList>) invoke("dnszone_find", flags, params, type).getResult();
    }

    public Set<Service> findAllService() throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Service.class);
        return (Set<Service>) invoke("service_find", flags, params, type).getResult();
    }

    public Service deleteService(String canonicalPrincipal) throws FreeIpaClientException {
        List<String> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of();
        return (Service) invoke("service_del", flags, params, Service.class).getResult();
    }

    public RPCResponse<Service> addService(String canonicalPrincipal) throws FreeIpaClientException {
        RPCResponse<Service> response = null;
        //TODO Implement as part of CDPSDX-584
        return response;
    }

    /**
     * Adds new reverse DNS zone for CIDR
     *
     * @param cidr Subnet formatted as 192.168.1.0/24
     * @return DnsZone created
     */
    public DnsZone addReverseDnsZone(String cidr) throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "name_from_ip", cidr,
                "raw", true);
        return (DnsZone) invoke("dnszone_add", flags, params, DnsZone.class).getResult();
    }

    public RPCResponse<Object> deleteDnsZone(String... dnsZoneNames) throws FreeIpaClientException {
        List<String> flags = List.of(dnsZoneNames);
        Map<String, Object> params = Map.of();
        return invoke("dnszone_del", flags, params, Object.class);
    }

    public Set<DnsRecord> findAllDnsRecordInZone(String dnsZoneName) throws FreeIpaClientException {
        List<String> flags = List.of(dnsZoneName);
        Map<String, Object> params = Map.of();
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, DnsRecord.class);
        return (Set<DnsRecord>) invoke("dnsrecord_find", flags, params, type).getResult();
    }

    public RPCResponse<Object> deleteDnsRecord(String recordName, String dnsZoneName) throws FreeIpaClientException {
        List<String> flags = List.of(dnsZoneName, recordName);
        Map<String, Object> params = Map.of();
        return invoke("dnsrecord_del", flags, params, Object.class);
    }

    public RPCResponse<Service> serviceAllowRetrieveKeytab(String canonicalPrincipal, String user) throws FreeIpaClientException {
        RPCResponse<Service> response = null;
        //TODO Implement as part of CDPSDX-584
        return response;
    }

    public RPCResponse<Keytab> getExistingKeytab(String canonicalPrincipal) throws FreeIpaClientException {
        RPCResponse<Keytab> response = null;
        //TODO Implement as part of CDPSDX-584
        return response;
    }

    public RPCResponse<Keytab> getKeytab(String canonicalPrincipal) throws FreeIpaClientException {
        RPCResponse<Keytab> response = null;
        //TODO Implement as part of CDPSDX-584
        return response;
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
