package com.sequenceiq.cloudbreak.auth.crn;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.sequenceiq.cloudbreak.auth.crn.Crn.ResourceType.MACHINE_USER;
import static com.sequenceiq.cloudbreak.auth.crn.Crn.ResourceType.USER;
import static com.sequenceiq.cloudbreak.auth.crn.Crn.Service.IAM;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

/**
 * A Cloudera Resource Name uniquely identifies a Cloudera Data Platform (CDP) resource.
 * It also supports the legacy "altus" partition.
 * <p>
 * The format is:
 * <p>
 * crn:partition:service:region:account-id:resourcetype:resource
 * <p>
 * An example might be example:
 * crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:ff186d14-1cfc-461f-a111-466857dd276e
 * <p>
 * See the below for an explanation of the component parts.
 * <p>
 * Note the CRN format and component values are part of our compatibility
 * contract.
 */
public class Crn {

    private static final Pattern CRN_PATTERN =
            Pattern.compile("^crn:([\\w\\-]+):(\\w+):([\\w\\-]+):([\\w\\-]+):(\\w+):(\\S+)$");

    private static final boolean ADMIN_SERVICE = true;

    private static final boolean NON_ADMIN_SERVICE = false;

    /**
     * The CDP partition in which the resource resides. A CDP partition is a
     * namespace for sets of CDP services.
     */
    public enum Partition {
        /**
         * @deprecated {@link #ALTUS} was replaced by {@link #CDP} and is kept here for backward compatibility reasons (e.g., dynamodb serialized CRNs).
         */
        @Deprecated
        ALTUS("altus", "ccs"),

        CDP("cdp", null),
        CDP_GOV("cdp-us-gov", null);

        private static final ImmutableMap<String, Partition> FROM_STRING;

        static {
            ImmutableMap.Builder<String, Partition> builder = ImmutableMap.builder();
            Arrays.stream(Partition.values()).forEach(partition -> {
                builder.put(partition.name, partition);
                if (partition.legacyName != null) {
                    builder.put(partition.legacyName, partition);
                }
            });
            FROM_STRING = builder.build();
        }

        private final String name;

        private final String legacyName;

        Partition(String name, String legacyName) {
            this.name = checkNotNull(name, "name should not be null.");
            this.legacyName = legacyName;
        }

        public String getName() {
            return name;
        }

        public String getLegacyName() {
            return legacyName;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Get a partition from a string. 'null' if 'input' is not a valid
         * partition name.
         *
         * @param input the input string
         * @return the partition
         * @throws NullPointerException if 'input' is null
         */
        public static Partition fromString(String input) {
            checkNotNull(input, "input should not be null.");
            return FROM_STRING.get(input);
        }

        /**
         * Get a partition from a string. This will never return null.
         *
         * @param input the input string
         * @return the partition
         * @throws NullPointerException if 'input' is null
         * @throws CrnParseException    if 'input' is not a valid partition name
         */
        public static Partition safeFromString(String input) {
            checkNotNull(input, "input should not be null.");
            Partition partition = fromString(input);
            if (partition == null) {
                throw new CrnParseException(String.format(
                        "%s is not a valid partition value", input));
            }
            return partition;
        }
    }

    /**
     * The Altus service in which the resource resides.
     */
    public enum Service {
        /**
         * @deprecated {@link #DRDS} was replaced by {@link #DATAENGADMIN} and is kept here for backward compatibility reasons (e.g., dynamodb serialized CRNs).
         */
        @Deprecated
        DRDS("drds", ADMIN_SERVICE),
        IAM("iam", NON_ADMIN_SERVICE),
        DATAENG("dataeng", "mastodon", NON_ADMIN_SERVICE),
        ENVIRONMENTS("environments", "vault", NON_ADMIN_SERVICE),
        WA("wa", "sigma", NON_ADMIN_SERVICE),
        NAVOPT("navopt", NON_ADMIN_SERVICE),
        DBUS("dbus", NON_ADMIN_SERVICE),
        AUTOSCALE("autoscale", NON_ADMIN_SERVICE),
        CLOUDBREAK("cloudbreak", NON_ADMIN_SERVICE),
        /**
         * @deprecated {@link #SDX} was replaced by {@link #DATALAKE} and is kept here for backward compatibility reasons (e.g., dynamodb serialized CRNs).
         */
        @Deprecated
        SDX("sdx", NON_ADMIN_SERVICE),
        REDBEAMS("redbeams", NON_ADMIN_SERVICE),
        SDXADMIN("sdxadmin", ADMIN_SERVICE),
        COREADMIN("coreadmin", ADMIN_SERVICE),
        DATAENGADMIN("dataengadmin", ADMIN_SERVICE),
        ARCUSADMIN("arcusadmin", ADMIN_SERVICE),
        SIGMAADMIN("sigmaadmin", ADMIN_SERVICE),
        DATAWARE("dataware", "analyticdb", NON_ADMIN_SERVICE),
        VALIDATIONSTREAM("validationstream", NON_ADMIN_SERVICE),
        VALIDATIONSTORAGE("validationstorage", NON_ADMIN_SERVICE),
        VALIDATIONPORTAL("validationportal", NON_ADMIN_SERVICE),
        DSADMIN("dsadmin", ADMIN_SERVICE),
        SAMPLE("sample", NON_ADMIN_SERVICE),
        WORKSPACES("workspaces", NON_ADMIN_SERVICE),
        FREEIPA("freeipa", NON_ADMIN_SERVICE),
        DATAHUB("datahub", NON_ADMIN_SERVICE),
        DATALAKE("datalake", NON_ADMIN_SERVICE),
        EXTERNALIZED_COMPUTE("externalizedcompute", NON_ADMIN_SERVICE),
        /**
         * @deprecated {@link #DEX} was replaced by {@link #DE} and is kept here for backward compatibility.
         */
        @Deprecated
        DEX("dex", NON_ADMIN_SERVICE),
        DE("de", NON_ADMIN_SERVICE),
        ACCOUNTTAG("accounttag", NON_ADMIN_SERVICE),
        ACCOUNTTELEMETRY("accounttelemetry", NON_ADMIN_SERVICE),
        ML("ml", NON_ADMIN_SERVICE),
        DF("df", NON_ADMIN_SERVICE),
        CCMV2("ccmv2", NON_ADMIN_SERVICE),
        SDXSVC("sdxsvc", NON_ADMIN_SERVICE),
        SDXSVCADMIN("sdxsvcadmin", ADMIN_SERVICE),
        RECIPE("recipe", NON_ADMIN_SERVICE),
        COMPUTE("compute", NON_ADMIN_SERVICE),
        MONITORING("monitoring", NON_ADMIN_SERVICE),
        SERVICEDISCOVERY("servicediscovery", NON_ADMIN_SERVICE),
        METERING("metering", NON_ADMIN_SERVICE),
        METERINGADMIN("meteringadmin", ADMIN_SERVICE),
        PUBLICENDPOINTMANAGEMENTADMIN("publicendpointmanagementadmin", ADMIN_SERVICE),
        MINASSHDADMIN("minasshdadmin", ADMIN_SERVICE),
        AUDIT("audit", NON_ADMIN_SERVICE),
        AUDITADMIN("auditadmin", ADMIN_SERVICE),
        CDPDEMOAPP("cdpdemoapp", NON_ADMIN_SERVICE),
        MLADMIN("mladmin", ADMIN_SERVICE),
        MINASSHDMANAGEMENT("minasshdmanagment", NON_ADMIN_SERVICE),
        CLASSICCLUSTERS("classicclusters", NON_ADMIN_SERVICE),
        OPDB("opdb", NON_ADMIN_SERVICE),
        DW("dw", NON_ADMIN_SERVICE),
        DATACATALOG("datacatalog", NON_ADMIN_SERVICE),
        SDX2("sdx2", NON_ADMIN_SERVICE),
        CSMCHARGEBACK("csmchargeback", NON_ADMIN_SERVICE),
        KERBEROSMGMT("kerberosmgmt", NON_ADMIN_SERVICE),
        DIAGNOSTICS("diagnostics", NON_ADMIN_SERVICE),
        CONSOLEAUTHENTICATION("consoleauthentication", NON_ADMIN_SERVICE),
        AUTH("auth", NON_ADMIN_SERVICE),
        UNIFIEDDIAGNOSTICS("unifieddiagnostics", NON_ADMIN_SERVICE),
        CONTAINERIMAGECATALOG("containerimagecatalog", NON_ADMIN_SERVICE),
        IMAGECATALOG("imagecatalog", NON_ADMIN_SERVICE),
        OPDBADMIN("opdbadmin", ADMIN_SERVICE),
        METERINGEVENTSMONITORADMIN("meteringeventsmonitoradmin", ADMIN_SERVICE),
        CLOUDBREAKADMIN("cloudbreakadmin", ADMIN_SERVICE),
        MESSAGEBROKERADMIN("messagebrokeradmin", ADMIN_SERVICE),
        REPLICATIONMANAGER("replicationmanager", NON_ADMIN_SERVICE),
        CLUSTERCONNECTIVITYADMIN("clusterconnectivityadmin", ADMIN_SERVICE),
        CLUSTERPROXY("clusterproxy", NON_ADMIN_SERVICE),
        CLUSTERCONNECTIVITYMANAGEMENTV2("clusterconnectivitymanagementv2", NON_ADMIN_SERVICE),
        SCIM("scim", NON_ADMIN_SERVICE),
        CERTIFICATEMANAGEMENTADMIN("certificatemanagementadmin", ADMIN_SERVICE),
        COMPUTEADMIN("computeadmin", ADMIN_SERVICE),
        PUBLICENDPOINTMANAGEMENT("publicendpointmanagement", NON_ADMIN_SERVICE),
        CONSUMPTION("consumption", NON_ADMIN_SERVICE),
        CONSUMPTIONADMIN("consumptionadmin", ADMIN_SERVICE),
        ENVOYAUTHENTICATION("envoyauthentication", NON_ADMIN_SERVICE),
        JUMPGATESERVER("jumpgateserver", NON_ADMIN_SERVICE),
        ONE("one", NON_ADMIN_SERVICE),
        ONEADMIN("oneadmin", ADMIN_SERVICE),
        STUDIO("studio", NON_ADMIN_SERVICE),
        STUDIOADMIN("studioadmin", ADMIN_SERVICE),
        TRIALMANAGER("trialmanager", NON_ADMIN_SERVICE),
        TRIALMANAGERADMIN("trialmanageradmin", ADMIN_SERVICE),
        LENSSUPPORTADMIN("lenssupportadmin", ADMIN_SERVICE),
        DRS("drs", NON_ADMIN_SERVICE),
        WORKLOADCONNECTIVITY("workloadconnectivity", NON_ADMIN_SERVICE),
        CLOUDPRIVATELINKS("cloudprivatelinks", NON_ADMIN_SERVICE),
        DRSCP("drscp", NON_ADMIN_SERVICE),
        WORKLOADCONNECTIVITYADMIN("workloadconnectivityadmin", ADMIN_SERVICE),
        NOTIFICATIONADMIN("notificationadmin", ADMIN_SERVICE),
        NOTIFICATION("notification", NON_ADMIN_SERVICE),
        COMMONCONSOLE("commonconsole", NON_ADMIN_SERVICE),
        HYBRID("hybrid", NON_ADMIN_SERVICE),
        REMOTECLUSTER("remotecluster", NON_ADMIN_SERVICE),
        APP_FACTORY("appfactory", NON_ADMIN_SERVICE),
        APPLICATIONS("applications", NON_ADMIN_SERVICE);

        private static final ImmutableMap<String, Service> FROM_STRING;

        static {
            ImmutableMap.Builder<String, Service> builder = ImmutableMap.builder();
            Arrays.stream(Service.values()).forEach(service -> {
                builder.put(service.name, service);
                if (service.legacyName != null) {
                    builder.put(service.legacyName, service);
                }
            });
            FROM_STRING = builder.build();
        }

        private final String name;

        private final String legacyName;

        private final boolean adminService;

        Service(String name, boolean adminService) {
            this(name, null, adminService);
        }

        Service(String name, String legacyName, boolean adminService) {
            this.name = checkNotNull(name, "name should not be null.");
            this.legacyName = legacyName;
            this.adminService = adminService;
        }

        public String getName() {
            return name;
        }

        public String getLegacyName() {
            return legacyName;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Get a service from a string. 'null' if 'input' is not a valid
         * service name.
         *
         * @param input the input string
         * @return the service
         * @throws NullPointerException if 'input' is null
         */
        public static Service fromString(String input) {
            checkNotNull(input, "input should not be null.");
            return FROM_STRING.get(input);
        }

        /**
         * Get a service from a string. This will never return null.
         *
         * @param input the input string
         * @return the service
         * @throws NullPointerException if 'input' is null
         * @throws CrnParseException    if 'input' is not a valid service name
         */
        public static Service safeFromString(String input) {
            checkNotNull(input, "input should not be null.");
            Service service = fromString(input);
            if (service == null) {
                throw new CrnParseException(String.format(
                        "%s is not a valid service value", input));
            }
            return service;
        }

        /**
         * Returns whether the service is an admin service or not.
         *
         * @return whether the service is an admin service or not.
         */
        public boolean isAdminService() {
            return adminService;
        }
    }

    /**
     * The Altus region in which the resource resides.
     */
    public enum Region {
        US_WEST_1("us-west-1"),
        EU_1("eu-1"),
        AP_1("ap-1"),
        USG_1("usg-1");

        private static final ImmutableMap<String, Region> FROM_STRING;

        static {
            ImmutableMap.Builder<String, Region> builder = ImmutableMap.builder();
            Arrays.stream(Region.values()).forEach(region -> {
                builder.put(region.name, region);
            });
            FROM_STRING = builder.build();
        }

        private final String name;

        Region(String name) {
            this.name = checkNotNull(name, "name should not be null.");
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Get a region from a string. 'null' if 'input' is not a valid
         * region name.
         *
         * @param input the input string
         * @return the region
         */
        public static Region fromString(String input) {
            checkNotNull(input, "input should not be null.");
            return FROM_STRING.get(input);
        }

        /**
         * Get a region from a string. This will never return null.
         *
         * @param input the input string
         * @return the region
         */
        public static Region safeFromString(String input) {
            checkNotNull(input, "input should not be null.");
            Region region = fromString(input);
            if (region == null) {
                throw new CrnParseException(String.format(
                        "%s is not a valid region value", input));
            }
            return region;
        }
    }

    /**
     * The type of this Altus resource.
     */
    public enum ResourceType {
        ACCESS_KEY("accesskey"),
        ACCESS_TOKEN("accessToken"),
        CLUSTER("cluster"),
        /**
         * @deprecated {@link #SDX_CLUSTER} was replaced by {@link #DATALAKE} and is kept here for backward compatibility reasons (e.g., dynamodb serialized
         * CRNs).
         */
        @Deprecated
        SDX_CLUSTER("sdxcluster"),
        DIRECTOR("director"),
        ENVIRONMENT("environment"),
        JOB("job"),
        UPLOAD("upload"),
        USER("user"),
        GROUP("group"),
        POLICY("policy"),
        ROLE("role"),
        RESOURCE_ROLE("resourceRole"),
        MACHINE_USER("machineUser"),
        NAMESPACE("namespace"),
        KCLUSTER("kcluster"),
        SAML_PROVIDER("samlProvider"),
        WORKSPACE("workspace"),
        DATABASE("database"),
        DATABASE_SERVER("databaseServer"),
        /**
         * @deprecated {@link #BLUEPRINT} was replaced by {@link #CLUSTER_DEFINITION} and is kept here for backward compatibility reasons (e.g., dynamodb
         * serialized CRNs).
         */
        @Deprecated
        BLUEPRINT("blueprint"),
        CLUSTER_DEFINITION("clusterdefinition"),
        CLUSTER_TEMPLATE("clustertemplate"),
        CUSTOM_CONFIGURATIONS("customconfigurations"),
        /**
         * @deprecated {@link #STACK} was replaced by {@link #CLUSTER} and is kept here for backward compatibility reasons (e.g., dynamodb serialized CRNs).
         */
        @Deprecated
        STACK("stack"),
        CREDENTIAL("credential"),
        NETWORK("network"),
        LDAP("ldap"),
        IMAGE_CATALOG("imageCatalog"),
        KERBEROS("kerberos"),
        RECIPE("recipe"),
        PUBLIC_KEY("publicKey"),
        FREEIPA("freeipa"),
        DATALAKE("datalake"),

        EXTERNALIZED_COMPUTE("externalizedcompute"),
        ACCOUNT_TAG("accountTag"),
        ACCOUNT_TELEMETRY("accountTelemetry"),
        DATAHUB_AUTOSCALE_CONFIG("datahubAutoscaleConfig"),
        SERVICE("service"),
        AGENT("agent"),
        WXM_ENVIRONMENT("wxm_environment"),
        INSTANCE("instance"),
        CONNECTION("connection"),
        DOCKER_CONFIG("dockerConfig"),
        LDAP_PROVIDER("ldapProvider"),
        LOCAL_PROVIDER("localProvider"),
        WORKLOAD("workload"),
        MINA_SSHD_SERVICE("minaSshdService"),
        ACCOUNT("account"),
        PROXY_CONFIG("proxyConfig"),
        FLOW("flow"),
        DEPLOYMENT("deployment"),
        DEPLOYMENT_REQUEST("deploymentRequest"),
        ASSET_UPDATE_REQUEST("assetUpdateRequest"),
        PROJECT("project"),
        EVENT("event"),
        AUTO_ACTION("autoAction"),
        COST_CENTER("costCenter"),
        SYNC_EVENT("syncEvent"),
        FUNCTION_DEPLOYMENT("functionDeployment"),
        COMPUTE_CLUSTER("computeCluster"),
        BACKUP("backup"),
        RESTORE("restore"),
        DELETE_BACKUP_REQUEST("deleteBackupRequest"),
        USERSYNC("usersync"),
        PVC_CONTROL_PLANE("pvcControlPlane"),
        VIRTUAL_CLUSTER("virtualCluster"),
        MLSERVING("mlserving"),
        ENCRYPTION_PROFILE("encryptionProfile"),
        APP("app");

        private static final ImmutableMap<String, ResourceType> FROM_STRING;

        static {
            ImmutableMap.Builder<String, ResourceType> builder = ImmutableMap.builder();
            Arrays.stream(ResourceType.values()).forEach(
                    resourceType -> builder.put(resourceType.name, resourceType));
            FROM_STRING = builder.build();
        }

        private final String name;

        ResourceType(String name) {
            this.name = checkNotNull(name, "name should not be null.");
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Get a resource type from a string. This will never return null.
         *
         * @param input the input string
         * @return the resource type
         * @throws CrnParseException if 'input' is not a valid resource type
         */
        public static ResourceType fromString(String input) {
            checkNotNull(input, "input should not be null.");
            ResourceType resourceType = FROM_STRING.get(input);
            if (resourceType == null) {
                throw new CrnParseException(String.format(
                        "%s is not a valid resource type value", input));
            }
            return resourceType;
        }
    }

    private final Partition partition;

    private final Service service;

    private final Region region;

    private final String accountId;

    private final ResourceType resourceType;

    private final String resource;

    /**
     * Constructor. Also see the Builder below.
     *
     * @param partition    the Altus partition in which the resource resides
     * @param service      the Altus service in which the resource resides
     * @param region       the Altus region in which the resource resides
     * @param accountId    the Altus account with which the resource is associated
     * @param resourceType the type of this Altus resource
     * @param resource     the name of this Altus resource
     */
    Crn(Partition partition,
        Service service,
        Region region,
        String accountId,
        ResourceType resourceType,
        String resource) {
        this.partition = checkNotNull(partition, "partition should not be null.");
        this.service = checkNotNull(service, "service should not be null.");
        this.region = checkNotNull(region, "region should not be null.");
        this.accountId = checkNotNull(accountId, "accountId should not be null.");
        this.resourceType = checkNotNull(resourceType, "resourceType should not be null.");
        this.resource = checkNotNull(resource, "resource should not be null.");
    }

    /**
     * Returns the partition.
     *
     * @return the partition
     */
    public Partition getPartition() {
        return partition;
    }

    /**
     * Returns the service.
     *
     * @return the service
     */
    public Service getService() {
        return service;
    }

    /**
     * Returns the region.
     *
     * @return the region
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Returns the account ID.
     *
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Returns the resource type.
     *
     * @return the resource type
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Returns the resource.
     *
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * Returns the user ID for this CRN. A type 0 user CRN is composed of an
     * external ID and the user ID separated by a slash; a type 1 user CRN has
     * only the user ID.
     *
     * @return the user ID
     * @throws IllegalStateException if this is not a user CRN
     */
    public String getUserId() {
        checkState(resourceType == ResourceType.USER || resourceType == ResourceType.MACHINE_USER,
                String.format("CRN %s has no user ID because it is of type %s", toString(), resourceType));
        int idx = resource.indexOf('/');
        return idx == -1 ? resource : resource.substring(idx + 1);
    }

    @Override
    public String toString() {
        return String.format("crn:%s:%s:%s:%s:%s:%s",
                partition, service, region, accountId, resourceType, resource);
    }

    /**
     * Creates a CRN from the input string. This will return null if the input
     * string does not match the CRN pattern and throw if the input string does
     * match, but cannot be parsed into a CRN for some reason.
     *
     * @param input the input string
     * @return the CRN
     * @throws NullPointerException if the input string is null
     * @throws CrnParseException    if the input string matches the CRN pattern but cannot be parsed
     */
    //CHECKSTYLE:OFF: checkstyle:magicnumber
    @Nullable
    public static Crn fromString(String input) {
        checkNotNull(input, "input should not be null.");
        Matcher matcher = CRN_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        return new Crn.Builder()
                .setPartition(Partition.safeFromString(matcher.group(1)))
                .setService(Service.safeFromString(matcher.group(2)))
                .setRegion(Region.safeFromString(matcher.group(3)))
                .setAccountId(matcher.group(4))
                .setResourceType(ResourceType.fromString(matcher.group(5)))
                .setResource(matcher.group(6))
                .build();
    }
    //CHECKSTYLE:ON: checkstyle:magicnumber

    /**
     * Creates a CRN from the input string. This will explode instead of
     * returning null.
     *
     * @param input the input string
     * @return the CRN
     * @throws NullPointerException if the input string is null
     * @throws CrnParseException    if the input string does not match the CRN pattern or cannot be parsed
     */
    public static Crn safeFromString(String input) {
        Crn crn = fromString(input);
        if (crn == null) {
            throw new CrnParseException(String.format("%s does not match the CRN pattern", input));
        }
        return crn;
    }

    /**
     * Returns whether an input string is a CRN.
     *
     * @param input the input string
     * @return whether the input string is a CRN
     */
    public static boolean isCrn(@Nullable String input) {
        try {
            safeFromString(input);
            return true;
        } catch (NullPointerException | CrnParseException e) {
            return false;
        }
    }

    public static Crn ofUser(String value) {
        Crn crn = Crn.fromString(value);
        checkArgument(!java.util.Objects.isNull(crn)
                && IAM.equals(crn.getService())
                && (USER.equals(crn.getResourceType())
                || MACHINE_USER.equals(crn.getResourceType())), value + " is not a valid user crn");
        return crn;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(partition,
                service,
                region,
                accountId,
                resourceType,
                resource);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        Crn other = (Crn) object;
        return Objects.equal(this.partition, other.partition)
                && Objects.equal(this.service, other.service)
                && Objects.equal(this.region, other.region)
                && Objects.equal(this.accountId, other.accountId)
                && Objects.equal(this.resourceType, other.resourceType)
                && Objects.equal(this.resource, other.resource);
    }

    public static Crn copyWithDifferentAccountId(Crn base, String accountId) {
        return new Crn(base.getPartition(),
                base.getService(),
                base.getRegion(),
                accountId,
                base.getResourceType(),
                base.getResource());
    }

    static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Partition partition;

        private Service service;

        private String accountId;

        private ResourceType resourceType;

        private String resource;

        private Region region;

        Builder() {
        }

        public Builder setAccountId(String accountId) {
            this.accountId = checkNotNull(accountId, "accountId should not be null.");
            return this;
        }

        public Builder setResource(String resource) {
            this.resource = checkNotNull(resource, "resource should not be null.");
            return this;
        }

        public Crn build() {
            return new Crn(partition,
                    service,
                    region,
                    accountId,
                    resourceType,
                    resource);
        }

        Builder setPartition(Partition partition) {
            this.partition = partition;
            return this;
        }

        Builder setRegion(Region region) {
            this.region = region;
            return this;
        }

        Builder setService(Service service) {
            this.service = checkNotNull(service, "service should not be null.");
            return this;
        }

        Builder setResourceType(ResourceType resourceType) {
            this.resourceType = checkNotNull(resourceType, "resourceType should not be null.");
            return this;
        }

    }
}
