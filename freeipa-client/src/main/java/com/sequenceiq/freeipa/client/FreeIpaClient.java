package com.sequenceiq.freeipa.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
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
import com.sequenceiq.freeipa.client.model.Cert;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.client.model.DnsZoneList;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.PasswordPolicy;
import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.client.model.Privilege;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.client.model.User;

public class FreeIpaClient {

    public static final String MAX_PASSWORD_EXPIRATION_DATETIME = "20380101000000Z";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV");

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClient.class);

    private static final String DEFAULT_API_VERSION = "2.230";

    private static final int CESSATION_OF_OPERATION = 5;

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

    public Privilege showPrivilege(String privilegeName) throws FreeIpaClientException {
        List<String> flags = List.of(privilegeName);
        Map<String, Object> params = Map.of();
        return (Privilege) invoke("privilege_show", flags, params, Privilege.class).getResult();
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

    public Host addHost(String fqdn) throws FreeIpaClientException {
        RPCResponse<Host> response = null;
        Map<String, Object> params = Map.of("force", true);
        return (Host) invoke("host_add", List.of(fqdn), params, Host.class).getResult();
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

    public User userAdd(String user, String firstName, String lastName) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        Map<String, Object> params = Map.of(
                "givenname", firstName,
                "sn", lastName,
                "loginshell", "/bin/bash",
                "random", true,
                "setattr", "krbPasswordExpiration=" + MAX_PASSWORD_EXPIRATION_DATETIME
        );
        return (User) invoke("user_add", flags, params, User.class).getResult();
    }

    public void userSetPassword(String user, String password) throws FreeIpaClientException {
        // FreeIPA expires any password that is set by another user. Work around this by
        // performing a separate API call to set the password expiration into the future
        userMod(user, "userpassword", password);
        updateUserPasswordMaxExpiration(user);
    }

    public void updateUserPasswordExpiration(String user, Optional<Instant> expiration) throws FreeIpaClientException {
        String passwordExpirationDate = formatDate(expiration);
        userMod(user, "setattr", "krbPasswordExpiration=" + passwordExpirationDate);
    }

    public void updateUserPasswordMaxExpiration(String user) throws FreeIpaClientException {
        updateUserPasswordExpiration(user, Optional.empty());
    }

    public User userSetPasswordHash(String user, String hashedPassword,
            String unencryptedKrbPrincipalKey, Optional<Instant> expiration) throws FreeIpaClientException {
        String passwordExpirationDate = formatDate(expiration);
        Map<String, Object> params =
                Map.of("setattr", List.of(
                        "cdpHashedPassword=" + hashedPassword,
                        "cdpUnencryptedKrbPrincipalKey=" + unencryptedKrbPrincipalKey,
                        "krbPasswordExpiration=" + passwordExpirationDate));
        return userMod(user, params);
    }

    String formatDate(Optional<Instant> instant) {
        if (instant.isPresent()) {
            return DATE_TIME_FORMATTER.format(ZonedDateTime.ofInstant(instant.get(), ZoneOffset.UTC));
        } else {
            return MAX_PASSWORD_EXPIRATION_DATETIME;
        }
    }

    public User userMod(String user, String key, Object value) throws FreeIpaClientException {
        Map<String, Object> params = Map.of(
                key, value
        );
        return userMod(user, params);
    }

    public User userMod(String user, Map<String, Object> params) throws FreeIpaClientException {
        List<String> flags = List.of(user);
        return (User) invoke("user_mod", flags, params, User.class).getResult();
    }

    public Group groupAdd(String group) throws FreeIpaClientException {
        List<String> flags = List.of(group);
        Map<String, Object> params = Map.of();
        return (Group) invoke("group_add", flags, params, Group.class).getResult();
    }

    public void deleteGroup(String group) throws FreeIpaClientException {
        List<String> flags = List.of(group);
        Map<String, Object> params = Map.of();
        invoke("group_del", flags, params, Object.class);
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
    public RPCResponse<Object> groupAddMembers(String group, Collection<String> users) throws FreeIpaClientException {
        List<String> flags = List.of(group);
        Map<String, Object> params = Map.of(
                "user", users
        );
        return invoke("group_add_member", flags, params, Object.class);
    }

    public RPCResponse<Object> groupRemoveMembers(String group, Collection<String> users) throws FreeIpaClientException {
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

    public Set<Cert> findAllCert() throws FreeIpaClientException {
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Cert.class);
        return (Set<Cert>) invoke("cert_find", List.of(), Map.of(), type).getResult();
    }

    public void revokeCert(int serialNumber) throws FreeIpaClientException {
        List<String> flags = List.of(String.valueOf(serialNumber));
        Map<String, Object> params = Map.of("revocation_reason", CESSATION_OF_OPERATION);
        invoke("cert_revoke", flags, params, Object.class);
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

    public Config getConfig() throws FreeIpaClientException {
        Map<String, Object> params = Map.of("all", true);
        return (Config) invoke("config_show", List.of(), params, Config.class).getResult();
    }

    public void setUsernameLength(int length) throws FreeIpaClientException {
        Map<String, Object> params = Map.of("ipamaxusernamelength", length);
        invoke("config_mod", List.of(), params, Object.class);
    }

    public void addPermissionToPrivilege(String privilege, String permission) throws FreeIpaClientException {
        addPermissionsToPrivilege(privilege, List.of(permission));
    }

    public void addPermissionsToPrivilege(String privilege, List<String> permissions) throws FreeIpaClientException {
        List<String> flags = List.of(privilege);
        Map<String, Object> params = Map.of(
                "permission", permissions
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

    public Set<DnsZoneList> findDnsZone(String cidr) throws FreeIpaClientException {
        List<String> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "pkey_only", true,
                "raw", true,
                "name_from_ip", cidr
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

    public Service addService(String canonicalPrincipal) throws FreeIpaClientException {
        List<String> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of("force", true);
        return (Service) invoke("service_add", flags, params, Service.class).getResult();
    }

    public Service addServiceAlias(String canonicalPrincipal, String principalname) throws FreeIpaClientException {
        List<String> flags = List.of(canonicalPrincipal, principalname);
        Map<String, Object> params = Map.of();
        return (Service) invoke("service_add_principal", flags, params, Service.class).getResult();
    }

    public Role addRole(String roleName) throws FreeIpaClientException {
        List<String> flags = List.of(roleName);
        Map<String, Object> params = Map.of();
        return (Role) invoke("role_add", flags, params, Role.class).getResult();
    }

    public Role addRolePrivileges(String roleName, Set<String> privilegeNames) throws FreeIpaClientException {
        List<String> flags = List.of(roleName);
        Map<String, Object> params = Map.of("privilege", privilegeNames);
        return (Role) invoke("role_add_privilege", flags, params, Role.class).getResult();
    }

    public Role showRole(String roleName) throws FreeIpaClientException {
        List<String> flags = List.of(roleName);
        Map<String, Object> params = Map.of();
        return (Role) invoke("role_show", flags, params, Role.class).getResult();
    }

    public Role addRoleMember(String roleName, Set<String> users, Set<String> groups, Set<String> hosts, Set<String> hostgroups, Set<String> services)
            throws FreeIpaClientException {
        List<String> flags = List.of(roleName);
        Map<String, Object> params = new HashMap<>();
        addToMapIfNotEmpty(params, "user", users);
        addToMapIfNotEmpty(params, "group", groups);
        addToMapIfNotEmpty(params, "hostgroup", hostgroups);
        addToMapIfNotEmpty(params, "service", services);
        addToMapIfNotEmpty(params, "host", hosts);
        return (Role) invoke("role_add_member", flags, params, Role.class).getResult();
    }

    private void addToMapIfNotEmpty(Map<String, Object> params, String key, Set<String> values) {
        if (values != null && !values.isEmpty()) {
            params.put(key, values);
        }
    }

    public Service showService(String canonicalPrincipal) throws FreeIpaClientException {
        List<String> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of(
                "rights", true,
                "all", true);
        return (Service) invoke("service_show", flags, params, Service.class).getResult();
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
        Map<String, Object> params = Map.of("del_all", true);
        return invoke("dnsrecord_del", flags, params, Object.class);
    }

    public void allowServiceKeytabRetrieval(String canonicalPrincipal, String user) throws FreeIpaClientException {
        List<String> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of("user", user);
        invoke("service_allow_retrieve_keytab", flags, params, Service.class);
    }

    public void allowHostKeytabRetrieval(String fqdn, String user) throws FreeIpaClientException {
        List<String> flags = List.of(fqdn);
        Map<String, Object> params = Map.of("user", user);
        invoke("host_allow_retrieve_keytab", flags, params, Host.class);
    }

    public Keytab getExistingKeytab(String canonicalPrincipal) throws FreeIpaClientException {
        List<String> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of("retrieve", true);
        return (Keytab) invoke("get_keytab", flags, params, Keytab.class).getResult();
    }

    public Keytab getKeytab(String canonicalPrincipal) throws FreeIpaClientException {
        List<String> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of();
        return (Keytab) invoke("get_keytab", flags, params, Keytab.class).getResult();
    }

    public Host showHost(String fqdn) throws FreeIpaClientException {
        List<String> flags = List.of(fqdn);
        Map<String, Object> params = Map.of();
        return (Host) invoke("host_show", flags, params, Host.class).getResult();
    }

    public <T> RPCResponse<T> invoke(String method, List<String> flags, Map<String, Object> params, Type resultType) throws FreeIpaClientException {
        Map<String, Object> parameterMap = new HashMap<>();
        if (params != null && !params.isEmpty()) {
            parameterMap.putAll(params);
        }
        parameterMap.put("version", apiVersion);

        LOGGER.debug("Issuing JSON-RPC request:\n\n method: {}\n flags: {}\n", method, flags);
        ParameterizedType type = TypeUtils
                .parameterize(RPCResponse.class, resultType);
        try {
            RPCResponse<T> response = (RPCResponse<T>) jsonRpcHttpClient.invoke(method, List.of(flags, parameterMap), type);
            LOGGER.debug("Response object: {}", response);
            if (response == null) {
                // TODO CDPCP-1028 investigate why invoke returns null instead of throwing an exception
                // when the cluster-proxy request times out.
                throw new NullPointerException("JSON-RPC response is null");
            }
            return response;
        } catch (Throwable throwable) {
            String message = String.format("Invoke FreeIpa failed: %s", throwable.getLocalizedMessage());
            LOGGER.error(message, throwable);
            throw new FreeIpaClientException(message, throwable);
        }
    }

    public PasswordPolicy getPasswordPolicy() throws FreeIpaClientException {
        return (PasswordPolicy) invoke("pwpolicy_show", List.of(), Map.of(), PasswordPolicy.class).getResult();
    }

    public void updatePasswordPolicy(Map<String, Object> params) throws FreeIpaClientException {
        invoke("pwpolicy_mod", Collections.emptyList(), params, Object.class);
    }
}
