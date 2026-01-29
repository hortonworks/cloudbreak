package com.sequenceiq.freeipa.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;
import com.sequenceiq.cloudbreak.util.CheckedTimeoutRunnable;
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
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.ServerRole;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.client.model.SudoCommand;
import com.sequenceiq.freeipa.client.model.SudoRule;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.client.model.Trust;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.client.operation.BatchOperation;
import com.sequenceiq.freeipa.client.operation.SudoCommandAddOperation;
import com.sequenceiq.freeipa.client.operation.SudoRuleAddAllowCommandOperation;
import com.sequenceiq.freeipa.client.operation.SudoRuleAddDenyCommandOperation;
import com.sequenceiq.freeipa.client.operation.SudoRuleAddGroupOperation;
import com.sequenceiq.freeipa.client.operation.SudoRuleAddOperation;
import com.sequenceiq.freeipa.client.operation.SudoRuleShowOperation;
import com.sequenceiq.freeipa.client.operation.UserAddOperation;
import com.sequenceiq.freeipa.client.operation.UserDisableOperation;
import com.sequenceiq.freeipa.client.operation.UserEnableOperation;
import com.sequenceiq.freeipa.client.operation.UserModOperation;
import com.sequenceiq.freeipa.client.operation.UserRemoveOperation;

public class FreeIpaClient {

    public static final String MAX_PASSWORD_EXPIRATION_DATETIME = "20380101000000Z";

    public static final Map<String, Object> UNLIMITED_PARAMS = Map.of("sizelimit", 0, "timelimit", 0);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV");

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClient.class);

    private static final String DEFAULT_API_VERSION = "2.230";

    private static final int CESSATION_OF_OPERATION = 5;

    private static final boolean USER_ENABLED = false;

    private static final Map<String, Object> PRIMARY_KEY_ONLY = Map.of("pkey_only", true);

    private JsonRpcHttpClient jsonRpcHttpClient;

    private final String apiVersion;

    private final String apiAddress;

    private final String hostname;

    public FreeIpaClient(JsonRpcHttpClient jsonRpcHttpClient, String apiAddress, String hostname) {
        this(jsonRpcHttpClient, DEFAULT_API_VERSION, apiAddress, hostname);
    }

    public FreeIpaClient(JsonRpcHttpClient jsonRpcHttpClient, String apiVersion, String apiAddress, String hostname) {
        this.jsonRpcHttpClient = jsonRpcHttpClient;
        this.apiVersion = apiVersion;
        this.apiAddress = apiAddress;
        this.hostname = hostname;
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

    public Set<String> userListAllUids() throws FreeIpaClientException {
        return userFindAll(Optional.empty(), PRIMARY_KEY_ONLY).stream()
                .map(User::getUid)
                .collect(Collectors.toSet());
    }

    /**
     * Finds all users in FreeIPA. This command sends a user_find request with the sizelimit
     * and timelimit set to 0.
     *
     * @param searchString     the search string
     * @param additionalParams additional parameters, e.g., ("all", true) or ("pkey_only", true)
     * @return Set of found Users
     */
    public Set<User> userFindAll(Optional<String> searchString, Map<String, Object> additionalParams)
            throws FreeIpaClientException {
        List<Object> flags = searchString.isEmpty() ? List.of() : List.of(searchString.get());
        Map<String, Object> params = new HashMap<>(UNLIMITED_PARAMS);
        params.putAll(additionalParams);

        ParameterizedType type = TypeUtils
                .parameterize(Set.class, User.class);
        return (Set<User>) invoke("user_find", flags, params, type).getResult();
    }

    public void userDisable(String user) throws FreeIpaClientException {
        UserDisableOperation.create(user).invoke(this).orElseThrow(() ->
                new FreeIpaClientException(String.format("User disable failed for user %s", user)));
    }

    public void userEnable(String user) throws FreeIpaClientException {
        UserEnableOperation.create(user).invoke(this).orElseThrow(() ->
                new FreeIpaClientException(String.format("User enable failed for user %s", user)));
    }

    public Set<Role> findAllRole() throws FreeIpaClientException {
        List<Object> flags = List.of();

        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Role.class);
        return (Set<Role>) invoke("role_find", flags, UNLIMITED_PARAMS, type).getResult();
    }

    public Privilege showPrivilege(String privilegeName) throws FreeIpaClientException {
        List<Object> flags = List.of(privilegeName);
        Map<String, Object> params = Map.of("all", true);
        return (Privilege) invoke("privilege_show", flags, params, Privilege.class).getResult();
    }

    public Set<Host> findAllHost() throws FreeIpaClientException {
        return findAllHost(Map.of());
    }

    public Set<Host> findAllHostFqdnOnly() throws FreeIpaClientException {
        return findAllHost(PRIMARY_KEY_ONLY);
    }

    private Set<Host> findAllHost(Map<String, Object> extraParams) throws FreeIpaClientException {
        List<Object> flags = List.of();
        Map<String, Object> params = new HashMap<>(UNLIMITED_PARAMS);
        params.putAll(extraParams);

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

        ParameterizedType type = TypeUtils
                .parameterize(Set.class, IpaServer.class);
        return (Set<IpaServer>) invoke("server_find", flags, UNLIMITED_PARAMS, type).getResult();
    }

    public Host deleteServer(String fqdn) throws FreeIpaClientException {
        Map<String, Object> params = Map.of(
                "force", true,
                "ignore_last_of_role", true,
                "ignore_topology_disconnect", true
        );
        return (Host) invoke("server_del", List.of(fqdn), params, Host.class).getResult();
    }

    public Host addHost(String fqdn) throws FreeIpaClientException {
        Map<String, Object> params = Map.of("force", true);
        return (Host) invoke("host_add", List.of(fqdn), params, Host.class).getResult();
    }

    public User deleteUser(String userUid) throws FreeIpaClientException {
        return UserRemoveOperation.create(userUid).invoke(this).orElseThrow(() ->
                new FreeIpaClientException(String.format("User deletion failed for user %s", userUid)));
    }

    public Role deleteRole(String roleName) throws FreeIpaClientException {
        List<Object> flags = List.of(roleName);
        Map<String, Object> params = Map.of();
        return (Role) invoke("role_del", flags, params, Role.class).getResult();
    }

    public User userAdd(String user, String firstName, String lastName) throws FreeIpaClientException {
        return userAdd(user, firstName, lastName, USER_ENABLED, Optional.empty());
    }

    /**
     * Adds a user to FreeIPA. This overload allows the user to be created in a disabled state.
     *
     * @param user      the user
     * @param firstName the user's first name
     * @param lastName  the user's last name
     * @param disabled  whether the user is disabled
     * @param title     Optional title
     * @return the user model
     */
    public User userAdd(String user, String firstName, String lastName, boolean disabled, Optional<String> title)
            throws FreeIpaClientException {
        return UserAddOperation.create(user, firstName, lastName, disabled, title).invoke(this).orElseThrow(() ->
                new FreeIpaClientException(String.format("User addition failed for user %s", user)));
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
        UserModOperation.create("userpassword", password, user).invoke(this);
        updateUserPasswordExpiration(user, expiration);
    }

    public void updateUserPasswordExpiration(String user, Optional<Instant> expiration) throws FreeIpaClientException {
        String passwordExpirationDate = formatDate(expiration);
        UserModOperation.create("setattr", "krbPasswordExpiration=" + passwordExpirationDate, user).invoke(this);
    }

    public void updateUserPasswordMaxExpiration(String user) throws FreeIpaClientException {
        updateUserPasswordExpiration(user, Optional.empty());
    }

    public String formatDate(Optional<Instant> instant) {
        if (instant.isPresent()) {
            return DATE_TIME_FORMATTER.format(ZonedDateTime.ofInstant(instant.get(), ZoneOffset.UTC));
        } else {
            return MAX_PASSWORD_EXPIRATION_DATETIME;
        }
    }

    public Set<Group> groupFindAll() throws FreeIpaClientException {
        List<Object> flags = List.of();

        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Group.class);
        return (Set<Group>) invoke("group_find", flags, UNLIMITED_PARAMS, type).getResult();
    }

    public Group groupShow(String groupName) throws FreeIpaClientException {
        List<Object> flags = List.of(groupName);
        Map<String, Object> params = Map.of();
        return (Group) invoke("group_show", flags, params, Group.class).getResult();
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
        return (Set<Cert>) invoke("cert_find", List.of(), UNLIMITED_PARAMS, type).getResult();
    }

    public void revokeCert(long serialNumber) throws FreeIpaClientException {
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
        Map<String, Object> params = new HashMap<>(UNLIMITED_PARAMS);
        params.put("cn", permission);
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, Permission.class);
        return (Set<Permission>) invoke("permission_find", List.of(), params, type).getResult();
    }

    public Config getConfig() throws FreeIpaClientException {
        Map<String, Object> params = Map.of("all", true);
        return (Config) invoke("config_show", List.of(), params, Config.class).getResult();
    }

    public void setUsernameLength(int length) throws FreeIpaClientException {
        Map<String, Object> params = Map.of("ipamaxusernamelength", length);
        invoke("config_mod", List.of(), params, Object.class);
    }

    public void setMaxHostNameLength(int length) throws FreeIpaClientException {
        Map<String, Object> params = Map.of("ipamaxhostnamelength", length);
        invoke("config_mod", List.of(), params, Object.class);
    }

    public void enableAndTriggerSidGeneration() throws FreeIpaClientException {
        Map<String, Object> params = Map.of(
                "enable_sid", true,
                "add_sids", true);
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
        Map<String, Object> params = new HashMap<>(UNLIMITED_PARAMS);
        params.put("raw", true);
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, DnsZone.class);
        return (Set<DnsZone>) invoke("dnszone_find", flags, params, type).getResult();
    }

    public Set<DnsZone> findDnsZone(String cidr) throws FreeIpaClientException {
        List<Object> flags = List.of();
        Map<String, Object> params = new HashMap<>(UNLIMITED_PARAMS);
        params.putAll(Map.of(
                "raw", true,
                "name_from_ip", cidr
        ));
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
        return findAllService(List.of(), Map.of());
    }

    public Set<Service> findAllServiceCanonicalNamesOnly() throws FreeIpaClientException {
        return findAllService(List.of(), PRIMARY_KEY_ONLY);
    }

    public Set<Service> findAllServiceCanonicalNamesOnly(String searchCriteria) throws FreeIpaClientException {
        return findAllService(List.of(searchCriteria), PRIMARY_KEY_ONLY);
    }

    private Set<Service> findAllService(List<Object> flags, Map<String, Object> extraParams) throws FreeIpaClientException {
        Map<String, Object> params = new HashMap<>(UNLIMITED_PARAMS);
        params.putAll(extraParams);

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

    public List<ServerRole> findServerRoles(String roleName, String status, String serverFqdn) throws FreeIpaClientException {
        Map<String, Object> params = new HashMap<>(UNLIMITED_PARAMS);
        Optional.ofNullable(status).ifPresent(s -> params.put("status", s));
        Optional.ofNullable(serverFqdn).ifPresent(s -> params.put("server_server", s));
        Optional.ofNullable(roleName).ifPresent(s -> params.put("role_servrole", s));
        ParameterizedType type = TypeUtils
                .parameterize(List.class, ServerRole.class);
        return (List<ServerRole>) invoke("server_role_find", List.of(), params, type).getResult();
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
                "skip_overlap_check", true,
                "raw", true);
        return (DnsZone) invoke("dnszone_add", flags, params, DnsZone.class).getResult();
    }

    public DnsZone addDnsZone(String zone) throws FreeIpaClientException {
        List<Object> flags = List.of(zone);
        Map<String, Object> params = Map.of(
                "skip_overlap_check", true,
                "raw", true);
        return (DnsZone) invoke("dnszone_add", flags, params, DnsZone.class).getResult();
    }

    public DnsZone addForwardDnsZone(String forwardZone, String forwarderIp, String forwardPolicy) throws FreeIpaClientException {
        List<Object> flags = List.of(forwardZone);
        Map<String, Object> params = Map.of(
                "idnsforwarders", forwarderIp,
                "idnsforwardpolicy", forwardPolicy,
                "skip_overlap_check", true);
        return (DnsZone) invoke("dnsforwardzone_add", flags, params, DnsZone.class).getResult();
    }

    public DnsZone modForwardDnsZone(String forwardZone, String forwarderIp, String forwardPolicy) throws FreeIpaClientException {
        List<Object> flags = List.of(forwardZone);
        Map<String, Object> params = Map.of(
                "idnsforwarders", forwarderIp,
                "idnsforwardpolicy", forwardPolicy);
        return (DnsZone) invoke("dnsforwardzone_mod", flags, params, DnsZone.class).getResult();
    }

    public RPCResponse<Object> deleteForwardDnsZone(String forwardZone) throws FreeIpaClientException {
        List<Object> flags = List.of(forwardZone);
        Map<String, Object> params = Map.of();
        return invoke("dnsforwardzone_del", flags, params, Object.class);
    }

    public DnsZone showForwardDnsZone(String forwardZone) throws FreeIpaClientException {
        List<Object> flags = List.of(forwardZone);
        Map<String, Object> params = Map.of();
        return (DnsZone) invoke("dnsforwardzone_show", flags, params, DnsZone.class).getResult();
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

    public DnsRecord addDnsPtrRecord(String dnsZoneName, String ptrRecordName, String hostName) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName, createPtrRecordName(dnsZoneName, ptrRecordName));
        Map<String, Object> params = Map.of("ptr_part_hostname", hostName);
        return (DnsRecord) invoke("dnsrecord_add", flags, params, DnsRecord.class).getResult();
    }

    public RPCResponse<Object> deleteDnsZone(String dnsZoneName) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName);
        Map<String, Object> params = Map.of();
        return invoke("dnszone_del", flags, params, Object.class);
    }

    public Set<DnsRecord> findAllDnsRecordInZone(String dnsZoneName) throws FreeIpaClientException {
        List<Object> flags = List.of(dnsZoneName);
        ParameterizedType type = TypeUtils
                .parameterize(Set.class, DnsRecord.class);
        return (Set<DnsRecord>) invoke("dnsrecord_find", flags, UNLIMITED_PARAMS, type).getResult();
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

    public Trust addTrust(String trustSecret, String trustType, boolean bidirectional, String realm) throws FreeIpaClientException {
        List<Object> flags = List.of(realm);
        Map<String, Object> params = Map.of("trust_type", trustType,
                "trust_secret", trustSecret,
                "bidirectional", bidirectional);
        return (Trust) invoke("trust_add", flags, params, Trust.class).getResult();
    }

    public RPCResponse<Object> deleteTrust(String realm) throws FreeIpaClientException {
        List<Object> flags = List.of(realm);
        Map<String, Object> params = Map.of();
        return invoke("trust_del", flags, params, Object.class);
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

        try {
            RPCResponse<T> response = (RPCResponse<T>) jsonRpcHttpClient.invoke(method, List.of(flags, parameterMap), type);
            LOGGER.debug("Response object: {}", response);
            if (response == null) {
                // TODO CDPCP-1028 investigate why invoke returns null instead of throwing an exception
                // when the cluster-proxy request times out.
                throw new NullPointerException("JSON-RPC response is null");
            }
            return response;
        } catch (ClusterProxyException e) {
            String message = String.format("Invoke FreeIPA failed: %s", e.getLocalizedMessage());
            LOGGER.warn(message);
            if (FreeIpaClientExceptionUtil.isClusterProxyErrorRetryable(e)) {
                throw new RetryableFreeIpaClientException(message, e);
            } else {
                OptionalInt responseCode = FreeIpaClientExceptionUtil.extractResponseCode(e);
                throw FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(new FreeIpaClientException(message, e, responseCode));
            }
        } catch (Exception e) {
            String message = String.format("Invoke FreeIPA failed: %s", e.getLocalizedMessage());
            LOGGER.warn(message);
            OptionalInt responseCode = FreeIpaClientExceptionUtil.extractResponseCode(e);
            throw FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(new FreeIpaClientException(message, e, responseCode));
        } catch (Throwable throwable) {
            String message = String.format("Invoke FreeIPA failed: %s", throwable.getLocalizedMessage());
            LOGGER.warn(message);
            throw new FreeIpaClientException(message, throwable);
        }
    }

    public PasswordPolicy getPasswordPolicy() throws FreeIpaClientException {
        return (PasswordPolicy) invoke("pwpolicy_show", List.of(), Map.of(), PasswordPolicy.class).getResult();
    }

    public void updatePasswordPolicy(Map<String, Object> params) throws FreeIpaClientException {
        invoke("pwpolicy_mod", Collections.emptyList(), params, Object.class);
    }

    public List<TopologySuffix> findAllTopologySuffixes() throws FreeIpaClientException {
        List<Object> flags = List.of();
        ParameterizedType type = TypeUtils
                .parameterize(List.class, TopologySuffix.class);
        return (List<TopologySuffix>) invoke("topologysuffix_find", flags, UNLIMITED_PARAMS, type).getResult();
    }

    public List<TopologySegment> findTopologySegments(String topologySuffixCn) throws FreeIpaClientException {
        List<Object> flags = List.of(topologySuffixCn);
        ParameterizedType type = TypeUtils
                .parameterize(List.class, TopologySegment.class);
        return (List<TopologySegment>) invoke("topologysegment_find", flags, UNLIMITED_PARAMS, type).getResult();
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

    public void callBatch(BiConsumer<String, String> warnings, List<Object> operations, Integer partitionSize,
            Set<FreeIpaErrorCodes> acceptableErrorCodes, CheckedTimeoutRunnable check) throws FreeIpaClientException, TimeoutException {
        List<List<Object>> partitions = Lists.partition(operations, partitionSize);
        for (List<Object> operationsPartition : partitions) {
            check.run();
            BatchOperation.create(operationsPartition, warnings, acceptableErrorCodes).invoke(this);
        }
    }

    public <T> List<RPCResponse<T>> callBatchWithResult(BiConsumer<String, String> warnings, List<Object> operations, Integer partitionSize,
            Set<FreeIpaErrorCodes> acceptableErrorCodes, CheckedTimeoutRunnable check, Type resultType) throws FreeIpaClientException, TimeoutException {
        List<List<Object>> partitions = Lists.partition(operations, partitionSize);
        List<RPCResponse<T>> responses = new ArrayList<>();
        for (List<Object> operationsPartition : partitions) {
            check.run();
            responses.add(BatchOperation.<T>create(operationsPartition, warnings, acceptableErrorCodes).rpcInvoke(this, resultType));
        }
        return responses;
    }

    public void checkIfClientStillUsable(FreeIpaClientException e) throws FreeIpaClientException {
        if (e.isClientUnusable()) {
            LOGGER.warn("Client is not usable anymore");
            throw e;
        }
    }

    private static Map<String, String> createDnsName(String name) {
        return Map.of("__dns_name__", name);
    }

    private static Map<String, String> createPtrRecordName(String reverseDnsZone, String ptrRecordName) {
        return Map.of("__dns_name__", ptrRecordName);
    }

    public Set<SudoCommand> sudoCommandFindAll() throws FreeIpaClientException {
        List<Object> flags = List.of();

        ParameterizedType type = TypeUtils.parameterize(Set.class, SudoCommand.class);
        return (Set<SudoCommand>) invoke("sudocmd_find", flags, UNLIMITED_PARAMS, type).getResult();
    }

    public Optional<SudoCommand> sudoCommandAdd(String sudoCommand) throws FreeIpaClientException {
        return SudoCommandAddOperation.create(sudoCommand).invoke(this);
    }

    public Optional<SudoRule> sudoRuleShow(String ruleName) throws FreeIpaClientException {
        return SudoRuleShowOperation.create(ruleName).invoke(this);
    }

    public SudoRule sudoRuleAdd(String sudoRuleName, boolean hostCategoryAll) throws FreeIpaClientException {
        return SudoRuleAddOperation.create(sudoRuleName, hostCategoryAll, SudoRuleShowOperation.create(sudoRuleName)).invoke(this).orElseThrow(() ->
                new FreeIpaClientException("Failed to create sudo rule"));
    }

    public void sudoRuleAddAllowCommand(String ruleName, String sudoCommand) throws FreeIpaClientException {
        SudoRuleAddAllowCommandOperation.create(ruleName, sudoCommand).invoke(this);
    }

    public void sudoRuleAddDenyCommand(String ruleName, String sudoCommand) throws FreeIpaClientException {
        SudoRuleAddDenyCommandOperation.create(ruleName, sudoCommand).invoke(this);
    }

    public void sudoRuleAddGroup(String ruleName, String group) throws FreeIpaClientException {
        SudoRuleAddGroupOperation.create(ruleName, group).invoke(this);
    }
}
