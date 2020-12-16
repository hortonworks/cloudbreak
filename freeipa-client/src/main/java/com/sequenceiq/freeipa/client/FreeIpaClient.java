package com.sequenceiq.freeipa.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;
import com.sequenceiq.freeipa.client.model.Ca;
import com.sequenceiq.freeipa.client.model.Cert;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.PasswordPolicy;
import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.client.model.Privilege;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.client.model.User;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

public class FreeIpaClient {

    public static final String MAX_PASSWORD_EXPIRATION_DATETIME = "20380101000000Z";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV");

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClient.class);

    private static final String DEFAULT_API_VERSION = "2.230";

    private static final int CESSATION_OF_OPERATION = 5;

    private static final Pattern RESPONSE_CODE_PATTERN = Pattern.compile("^Server returned HTTP response code: (\\d+)");

    private JsonRpcHttpClient jsonRpcHttpClient;

    private final String apiVersion;

    private final String apiAddress;

    private final String hostname;

    private final Tracer tracer;

    public FreeIpaClient(JsonRpcHttpClient jsonRpcHttpClient, String apiAddress, String hostname, Tracer tracer) {
        this(jsonRpcHttpClient, DEFAULT_API_VERSION, apiAddress, hostname, tracer);
    }

    public FreeIpaClient(JsonRpcHttpClient jsonRpcHttpClient, String apiVersion, String apiAddress, String hostname, Tracer tracer) {
        this.jsonRpcHttpClient = jsonRpcHttpClient;
        this.apiVersion = apiVersion;
        this.apiAddress = apiAddress;
        this.hostname = hostname;
        this.tracer = tracer;
    }

    public String getApiAddress() {
        return apiAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public User userShow(String user) throws FreeIpaClientException {
        List<Object> flags = List.of(user);
        Map<String, Object> params = Map.of();
        return (User) invoke("user_show", flags, params, User.class).getResult();
    }

    public Optional<User> userFind(String user) throws FreeIpaClientException {
        List<Object> flags = List.of(user);
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
        List<Object> flags = List.of();
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
        List<Object> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Role.class);
        return (Set<Role>) invoke("role_find", flags, params, type).getResult();
    }

    public Privilege showPrivilege(String privilegeName) throws FreeIpaClientException {
        List<Object> flags = List.of(privilegeName);
        Map<String, Object> params = Map.of();
        return (Privilege) invoke("privilege_show", flags, params, Privilege.class).getResult();
    }

    public Set<Host> findAllHost() throws FreeIpaClientException {
        List<Object> flags = List.of();
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

    public Set<IpaServer> findAllServers() throws FreeIpaClientException {
        List<Object> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, IpaServer.class);
        return (Set<IpaServer>) invoke("server_find", flags, params, type).getResult();
    }

    public Host deleteServer(String fqdn) throws FreeIpaClientException {
        Map<String, Object> params = Map.of();
        return (Host) invoke("server_del", List.of(fqdn), params, Host.class).getResult();
    }

    public Host addHost(String fqdn) throws FreeIpaClientException {
        RPCResponse<Host> response = null;
        Map<String, Object> params = Map.of("force", true);
        return (Host) invoke("host_add", List.of(fqdn), params, Host.class).getResult();
    }

    public User deleteUser(String userUid) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(userUid, () -> String.format("User '%s' is protected and cannot be deleted from FreeIPA", userUid));
        List<Object> flags = List.of(userUid);
        Map<String, Object> params = Map.of();
        return (User) invoke("user_del", flags, params, User.class).getResult();
    }

    public Role deleteRole(String roleName) throws FreeIpaClientException {
        List<Object> flags = List.of(roleName);
        Map<String, Object> params = Map.of();
        return (Role) invoke("role_del", flags, params, Role.class).getResult();
    }

    public User userAdd(String user, String firstName, String lastName) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(user, () -> String.format("User '%s' is protected and cannot be added to FreeIPA", user));
        List<Object> flags = List.of(user);
        Map<String, Object> params = Map.of(
                "givenname", firstName,
                "sn", lastName,
                "loginshell", "/bin/bash",
                "random", true,
                "setattr", "krbPasswordExpiration=" + MAX_PASSWORD_EXPIRATION_DATETIME
        );
        return (User) invoke("user_add", flags, params, User.class).getResult();
    }

    /**
     * Updates the password and password expiration time for the specified user
     *
     * @param user       the user
     * @param password   the password
     * @param expiration Optional of the expiration instant. An empty optional implies the max expiration time
     */
    public void userSetPasswordWithExpiration(String user, String password, Optional<Instant> expiration) throws FreeIpaClientException {
        // FreeIPA expires any password that is set by another user. Work around this by
        // performing a separate API call to set the password expiration into the future
        userMod(user, "userpassword", password);
        updateUserPasswordExpiration(user, expiration);
    }

    public void updateUserPasswordExpiration(String user, Optional<Instant> expiration) throws FreeIpaClientException {
        String passwordExpirationDate = formatDate(expiration);
        userMod(user, "setattr", "krbPasswordExpiration=" + passwordExpirationDate);
    }

    public void updateUserPasswordMaxExpiration(String user) throws FreeIpaClientException {
        updateUserPasswordExpiration(user, Optional.empty());
    }

    public User userSetWorkloadCredentials(String user, String hashedPassword,
            String unencryptedKrbPrincipalKey, Optional<Instant> expiration,
            List<String> sshPublicKeys) throws FreeIpaClientException {
        Map<String, Object> params = new HashMap<>();
        List<String> attributes = new ArrayList<>();

        if (StringUtils.isNotBlank(hashedPassword)) {
            attributes.add("cdpHashedPassword=" + hashedPassword);
            attributes.add("cdpUnencryptedKrbPrincipalKey=" + unencryptedKrbPrincipalKey);
            attributes.add("krbPasswordExpiration=" + formatDate(expiration));
            params.put("setattr", attributes);
        }

        params.put("ipasshpubkey", sshPublicKeys);

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
        List<Object> flags = List.of(user);
        return (User) invoke("user_mod", flags, params, User.class).getResult();
    }

    public Group groupAdd(String group) throws FreeIpaClientException {
        FreeIpaChecks.checkGroupNotProtected(group, () -> String.format("Group '%s' is protected and cannot be added to FreeIPA", group));
        List<Object> flags = List.of(group);
        Map<String, Object> params = Map.of();
        return (Group) invoke("group_add", flags, params, Group.class).getResult();
    }

    public void deleteGroup(String group) throws FreeIpaClientException {
        FreeIpaChecks.checkGroupNotProtected(group, () -> String.format("Group '%s' is protected and cannot be deleted from FreeIPA", group));
        List<Object> flags = List.of(group);
        Map<String, Object> params = Map.of();
        invoke("group_del", flags, params, Object.class);
    }

    public RPCResponse<Group> groupAddMembers(String group, Collection<String> users) throws FreeIpaClientException {
        FreeIpaChecks.checkGroupNotUnmanaged(group, () -> String.format("Group '%s' is not managed and membership cannot be changed", group));
        List<Object> flags = List.of(group);
        Map<String, Object> params = Map.of(
                "user", users
        );
        return invoke("group_add_member", flags, params, Group.class);
    }

    public RPCResponse<Group> groupRemoveMembers(String group, Collection<String> users) throws FreeIpaClientException {
        FreeIpaChecks.checkGroupNotUnmanaged(group, () -> String.format("Group '%s' is not managed and membership cannot be changed", group));
        List<Object> flags = List.of(group);
        Map<String, Object> params = Map.of(
                "user", users
        );
        return invoke("group_remove_member", flags, params, Group.class);
    }

    public Set<Group> groupFindAll() throws FreeIpaClientException {
        List<Object> flags = List.of();
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
        List<Object> flags = List.of("ipa");
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
        List<Object> flags = List.of(String.valueOf(serialNumber));
        Map<String, Object> params = Map.of("revocation_reason", CESSATION_OF_OPERATION);
        invoke("cert_revoke", flags, params, Object.class);
    }

    public void addPasswordExpirationPermission(String permission) throws FreeIpaClientException {
        List<Object> flags = List.of(permission);
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
        List<Object> flags = List.of(privilege);
        Map<String, Object> params = Map.of(
                "permission", permissions
        );
        invoke("privilege_add_permission", flags, params, Object.class);
    }

    public Set<DnsZone> findAllDnsZone() throws FreeIpaClientException {
        List<Object> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "raw", true
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, DnsZone.class);
        return (Set<DnsZone>) invoke("dnszone_find", flags, params, type).getResult();
    }

    public Set<DnsZone> findDnsZone(String cidr) throws FreeIpaClientException {
        List<Object> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "raw", true,
                "name_from_ip", cidr
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, DnsZone.class);
        return (Set<DnsZone>) invoke("dnszone_find", flags, params, type).getResult();
    }

    public DnsZone setDnsZoneAuthoritativeNameserver(String dnsZone, String authoritativeNameserverFqdn) throws FreeIpaClientException {
        return dnsZoneMod(dnsZone, "idnssoamname", authoritativeNameserverFqdn);
    }

    private DnsZone dnsZoneMod(String zoneName, String key, Object value) throws FreeIpaClientException {
        Map<String, Object> params = Map.of(
                key, value
        );
        return dnsZoneMod(zoneName, params);
    }

    private DnsZone dnsZoneMod(String zoneName, Map<String, Object> params) throws FreeIpaClientException {
        List<Object> flags = List.of(zoneName);
        return (DnsZone) invoke("dnszone_mod", flags, params, DnsZone.class).getResult();
    }

    public Set<Service> findAllService() throws FreeIpaClientException {
        List<Object> flags = List.of();
        Map<String, Object> params = Map.of(
                "sizelimit", 0,
                "timelimit", 0
        );
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Service.class);
        return (Set<Service>) invoke("service_find", flags, params, type).getResult();
    }

    public Service deleteService(String canonicalPrincipal) throws FreeIpaClientException {
        List<Object> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of();
        return (Service) invoke("service_del", flags, params, Service.class).getResult();
    }

    public Service addService(String canonicalPrincipal) throws FreeIpaClientException {
        List<Object> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of("force", true);
        return (Service) invoke("service_add", flags, params, Service.class).getResult();
    }

    public Service addServiceAlias(String canonicalPrincipal, String principalname) throws FreeIpaClientException {
        List<Object> flags = List.of(canonicalPrincipal, principalname);
        Map<String, Object> params = Map.of();
        return (Service) invoke("service_add_principal", flags, params, Service.class).getResult();
    }

    public Role addRole(String roleName) throws FreeIpaClientException {
        List<Object> flags = List.of(roleName);
        Map<String, Object> params = Map.of();
        return (Role) invoke("role_add", flags, params, Role.class).getResult();
    }

    public Role addRolePrivileges(String roleName, Set<String> privilegeNames) throws FreeIpaClientException {
        List<Object> flags = List.of(roleName);
        Map<String, Object> params = Map.of("privilege", privilegeNames);
        return (Role) invoke("role_add_privilege", flags, params, Role.class).getResult();
    }

    public Role showRole(String roleName) throws FreeIpaClientException {
        List<Object> flags = List.of(roleName);
        Map<String, Object> params = Map.of();
        return (Role) invoke("role_show", flags, params, Role.class).getResult();
    }

    public Role addRoleMember(String roleName, Set<String> users, Set<String> groups, Set<String> hosts, Set<String> hostgroups, Set<String> services)
            throws FreeIpaClientException {
        List<Object> flags = List.of(roleName);
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
        List<Object> flags = List.of(canonicalPrincipal);
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
        List<Object> flags = List.of();
        Map<String, Object> params = Map.of(
                "name_from_ip", cidr,
                "raw", true);
        return (DnsZone) invoke("dnszone_add", flags, params, DnsZone.class).getResult();
    }

    public DnsRecord showDnsRecord(String dnsZoneName, String recordName) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName, createDnsName(recordName));
        Map<String, Object> params = Map.of();
        return (DnsRecord) invoke("dnsrecord_show", flags, params, DnsRecord.class).getResult();
    }

    public DnsRecord addDnsCnameRecord(String dnsZoneName, String recordName, String cnameRecord) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName, createDnsName(recordName));
        Map<String, Object> params = Map.of(
                "cname_part_hostname", createDnsName(cnameRecord));
        return (DnsRecord) invoke("dnsrecord_add", flags, params, DnsRecord.class).getResult();
    }

    public DnsRecord addDnsARecord(String dnsZoneName, String recordName, String ip, boolean createReverse) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName, createDnsName(recordName));
        Map<String, Object> params = Map.of(
                "a_part_ip_address", ip,
                "a_extra_create_reverse", createReverse);
        return (DnsRecord) invoke("dnsrecord_add", flags, params, DnsRecord.class).getResult();
    }

    public RPCResponse<Object> deleteDnsZone(String dnsZoneName) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName);
        Map<String, Object> params = Map.of();
        return invoke("dnszone_del", flags, params, Object.class);
    }

    public Set<DnsRecord> findAllDnsRecordInZone(String dnsZoneName) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName);
        Map<String, Object> params = Map.of();
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, DnsRecord.class);
        return (Set<DnsRecord>) invoke("dnsrecord_find", flags, params, type).getResult();
    }

    public RPCResponse<Object> deleteDnsRecord(String recordName, String dnsZoneName) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName, createDnsName(recordName));
        Map<String, Object> params = Map.of("del_all", true);
        return invoke("dnsrecord_del", flags, params, Object.class);
    }

    public RPCResponse<Object> deleteDnsSrvRecord(String recordName, String dnsZoneName, List<String> srvRecords) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName, createDnsName(recordName));
        Map<String, Object> params = Map.of("srvrecord", srvRecords);
        return invoke("dnsrecord_del", flags, params, Object.class);
    }

    public void allowServiceKeytabRetrieval(String canonicalPrincipal, String user) throws FreeIpaClientException {
        List<Object> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of("user", user);
        invoke("service_allow_retrieve_keytab", flags, params, Service.class);
    }

    public void allowHostKeytabRetrieval(String fqdn, String user) throws FreeIpaClientException {
        List<Object> flags = List.of(fqdn);
        Map<String, Object> params = Map.of("user", user);
        invoke("host_allow_retrieve_keytab", flags, params, Host.class);
    }

    public Keytab getExistingKeytab(String canonicalPrincipal) throws FreeIpaClientException {
        List<Object> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of("retrieve", true);
        return (Keytab) invoke("get_keytab", flags, params, Keytab.class).getResult();
    }

    public Keytab getKeytab(String canonicalPrincipal) throws FreeIpaClientException {
        List<Object> flags = List.of(canonicalPrincipal);
        Map<String, Object> params = Map.of();
        return (Keytab) invoke("get_keytab", flags, params, Keytab.class).getResult();
    }

    public Host showHost(String fqdn) throws FreeIpaClientException {
        List<Object> flags = List.of(fqdn);
        Map<String, Object> params = Map.of();
        return (Host) invoke("host_show", flags, params, Host.class).getResult();
    }

    public RPCResponse<Boolean> serverConnCheck(String cn, String remoteCn) throws FreeIpaClientException {
        return invoke("server_conncheck", List.of(cn), Map.of("remote_cn", remoteCn), Boolean.class);
    }

    public <T> RPCResponse<T> invoke(String method, List<Object> flags, Map<String, Object> params, Type resultType) throws FreeIpaClientException {
        Map<String, Object> parameterMap = new HashMap<>();
        if (params != null && !params.isEmpty()) {
            parameterMap.putAll(params);
        }
        parameterMap.put("version", apiVersion);

        LOGGER.debug("Issuing JSON-RPC request:\n\n method: {}\n flags: {}\n", method, flags);
        ParameterizedType type = TypeUtils
                .parameterize(RPCResponse.class, resultType);

        Span span = TracingUtil.initSpan(tracer, "FreeIpa", method);
        try (Scope ignored = tracer.activateSpan(span)) {
            RPCResponse<T> response = (RPCResponse<T>) jsonRpcHttpClient.invoke(method, List.of(flags, parameterMap), type);
            LOGGER.debug("Response object: {}", response);
            if (response == null) {
                // TODO CDPCP-1028 investigate why invoke returns null instead of throwing an exception
                // when the cluster-proxy request times out.
                throw new NullPointerException("JSON-RPC response is null");
            }
            return response;
        } catch (Exception e) {
            String message = String.format("Invoke FreeIPA failed: %s", e.getLocalizedMessage());
            LOGGER.warn(message);
            OptionalInt responseCode = extractResponseCode(e);
            span.setTag(TracingUtil.ERROR, true);
            span.setTag(TracingUtil.MESSAGE, e.getLocalizedMessage());
            throw FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(new FreeIpaClientException(message, e, responseCode));
        } catch (Throwable throwable) {
            String message = String.format("Invoke FreeIPA failed: %s", throwable.getLocalizedMessage());
            LOGGER.warn(message);
            span.setTag(TracingUtil.ERROR, true);
            span.setTag(TracingUtil.MESSAGE, throwable.getLocalizedMessage());
            throw new FreeIpaClientException(message, throwable);
        } finally {
            span.finish();
        }
    }

    private OptionalInt extractResponseCode(Exception e) {
        OptionalInt responseCode = OptionalInt.empty();
        try {
            Matcher matcher = RESPONSE_CODE_PATTERN.matcher(e.getMessage());
            if (matcher.find()) {
                responseCode = OptionalInt.of(Integer.parseInt(matcher.group(1)));
            } else if (null != e.getCause() && RESPONSE_CODE_PATTERN.matcher(e.getCause().getMessage()).find()) {
                matcher = RESPONSE_CODE_PATTERN.matcher(e.getCause().getMessage());
                matcher.find();
                responseCode = OptionalInt.of(Integer.parseInt(matcher.group(1)));
            }
        } catch (Exception ex) {
            LOGGER.warn("Couldn't extract response code from message", ex);
        }
        return responseCode;
    }

    public PasswordPolicy getPasswordPolicy() throws FreeIpaClientException {
        return (PasswordPolicy) invoke("pwpolicy_show", List.of(), Map.of(), PasswordPolicy.class).getResult();
    }

    public void updatePasswordPolicy(Map<String, Object> params) throws FreeIpaClientException {
        invoke("pwpolicy_mod", Collections.emptyList(), params, Object.class);
    }

    public List<TopologySuffix> findAllTopologySuffixes() throws FreeIpaClientException {
        List<Object> flags = List.of();
        Map<String, Object> params = Map.of();
        ParameterizedType type = TypeUtils
                .parameterize(List.class, TopologySuffix.class);
        return (List<TopologySuffix>) invoke("topologysuffix_find", flags, params, type).getResult();
    }

    public List<TopologySegment> findTopologySegments(String topologySuffixCn) throws FreeIpaClientException {
        List<Object> flags = List.of(topologySuffixCn);
        Map<String, Object> params = Map.of();
        ParameterizedType type = TypeUtils
                .parameterize(List.class, TopologySegment.class);
        return (List<TopologySegment>) invoke("topologysegment_find", flags, params, type).getResult();
    }

    public TopologySegment addTopologySegment(String topologySuffixCn, TopologySegment topologySegment) throws FreeIpaClientException {
        List<Object> flags = List.of(topologySuffixCn, topologySegment.getCn());
        Map<String, Object> params = Map.of(
                "iparepltoposegmentleftnode", topologySegment.getLeftNode(),
                "iparepltoposegmentrightnode", topologySegment.getRightNode(),
                "iparepltoposegmentdirection", topologySegment.getDirection()
        );
        return (TopologySegment) invoke("topologysegment_add", flags, params, TopologySegment.class).getResult();
    }

    public TopologySegment deleteTopologySegment(String topologySuffixCn, TopologySegment topologySegment) throws FreeIpaClientException {
        List<Object> flags = List.of(topologySuffixCn, topologySegment.getCn());
        Map<String, Object> params = Map.of();
        return (TopologySegment) invoke("topologysegment_del", flags, params, TopologySegment.class).getResult();
    }

    private static Map<String, String> createDnsName(String name) {
        return Map.of("__dns_name__", name);
    }

}
