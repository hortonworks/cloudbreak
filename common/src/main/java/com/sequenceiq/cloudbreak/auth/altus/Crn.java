package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * A Cloudera Resource Name uniquely identifies an Altus resource. The format is:
 *
 * crn:partition:service:region:account-id:resourcetype:resource
 *
 * An example might be example:
 *
 * crn:altus:drds:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:director:MyDirector
 *
 * See the below for an explanation of the component parts.
 *
 * Note the CRN format and component values are part of our compatibility
 * contract.
 */
//CHECKSTYLE:OFF
public class Crn {

    private static final Pattern CRN_PATTERN =
            Pattern.compile("^crn:(\\w+):(\\w+):(\\S+):(\\S+):(\\w+):(\\S+)$");

    private static final boolean ADMIN_SERVICE = true;
    private static final boolean NON_ADMIN_SERVICE = false;

    /**
     * The Altus partition in which the resource resides. An Altus partition is a
     * namespace for sets of Altus services. At present we plan only the single
     * Altus partition.
     */
    public enum Partition {
        ALTUS("altus", "ccs");

        final String name;
        final String legacyName;

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

        Partition(String name, String legacyName) {
            this.name = checkNotNull(name);
            this.legacyName = checkNotNull(legacyName);
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Get a partition from a string. 'null' if 'input' is not a valid
         * partition name.
         * @param input the input string
         * @return the partition
         */
        public static Partition fromString(String input) {
            checkNotNull(input);
            return FROM_STRING.get(input);
        }

        /**
         * Get a partition from a string. This will never return null.
         * @param input the input string
         * @return the partition
         */
        public static Partition safeFromString(String input) {
            checkNotNull(input);
            Partition partition = fromString(input);
            if (partition == null) {
                throw new IllegalArgumentException(String.format(
                        "%s is not a valid partition value", input));
            }
            return partition;
        }
    }

    /**
     * The Altus service in which the resource resides.
     */
    public enum Service {
        // DRDS was replaced by DATAENGADMIN and is kept here for backward
        // compatibility reasons (e.g., dynamodb serialized CRNs).
        @Deprecated
        DRDS("drds", ADMIN_SERVICE),
        IAM("iam", NON_ADMIN_SERVICE),
        DATAENG("dataeng", "mastodon", NON_ADMIN_SERVICE),
        ENVIRONMENTS("environments", "vault", NON_ADMIN_SERVICE),
        WA("wa", "sigma", NON_ADMIN_SERVICE),
        NAVOPT("navopt", NON_ADMIN_SERVICE),
        DBUS("dbus", NON_ADMIN_SERVICE),
        SDX("sdx", "gridlink", NON_ADMIN_SERVICE),
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
        FREEIPA("freeipa", NON_ADMIN_SERVICE);

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

        final String name;
        final String legacyName;
        final boolean adminService;

        Service(String name, boolean adminService) {
            this(name, null, adminService);
        }

        Service(String name, String legacyName, boolean adminService) {
            this.name = checkNotNull(name);
            this.legacyName = legacyName;
            this.adminService = adminService;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Get a service from a string. 'null' if 'input' is not a valid
         * service name.
         * @param input the input string
         * @return the service
         */
        public static Service fromString(String input) {
            checkNotNull(input);
            return FROM_STRING.get(input);
        }

        /**
         * Get a service from a string. This will never return null.
         * @param input the input string
         * @return the service
         */
        public static Service safeFromString(String input) {
            checkNotNull(input);
            Service service = fromString(input);
            if (service == null) {
                throw new IllegalArgumentException(String.format(
                        "%s is not a valid service value", input));
            }
            return service;
        }

        /**
         * Returns whether the service is an admin service or not.
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
        US_WEST_1("us-west-1");

        final String name;

        Region(String name) {
            this.name = checkNotNull(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * The type of this Altus resource.
     */
    public enum ResourceType {
        ACCESS_KEY("accesskey"),
        CLUSTER("cluster"),
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
        CREDENTIAL("credential"),
        LDAP("ldap"),
        KERBEROS("kerberos");

        private static final ImmutableMap<String, ResourceType> FROM_STRING;
        static {
            ImmutableMap.Builder<String, ResourceType> builder = ImmutableMap.builder();
            Arrays.stream(ResourceType.values()).forEach(
                    resourceType -> builder.put(resourceType.name, resourceType));
            FROM_STRING = builder.build();
        }

        final String name;

        ResourceType(String name) {
            this.name = checkNotNull(name);
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Get a resource type from a string. This will never return null.
         * @param input the input string
         * @return the resource type
         */
        public static ResourceType fromString(String input) {
            checkNotNull(input);
            ResourceType resourceType = FROM_STRING.get(input);
            if (resourceType == null) {
                throw new IllegalArgumentException(String.format(
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
     * @param partition the Altus partition in which the resource resides
     * @param service the Altus service in which the resource resides
     * @param region the Altus region in which the resource resides
     * @param accountId the Altus account with which the resource is associated
     * @param resourceType the type of this Altus resource
     * @param resource the name of this Altus resource
     */
    public Crn(Partition partition,
            Service service,
            Region region,
            String accountId,
            ResourceType resourceType,
            String resource) {
        this.partition = checkNotNull(partition);
        this.service = checkNotNull(service);
        this.region = checkNotNull(region);
        this.accountId = checkNotNull(accountId);
        this.resourceType = checkNotNull(resourceType);
        this.resource = checkNotNull(resource);
    }

    /**
     * Returns the partition.
     * @return the partition
     */
    public Partition getPartition() {
        return partition;
    }

    /**
     * Returns the service.
     * @return the service
     */
    public Service getService() {
        return service;
    }

    /**
     * Returns the region.
     * @return the region
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Returns the account ID.
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Returns the resource type.
     * @return the resource type
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Returns the resource.
     * @return the resource
     */
    public String getResource() {
        return resource;
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
     * @param input the input string
     * @return the CRN
     */
    public static @Nullable Crn fromString(String input) {
        checkNotNull(input);
        Matcher matcher = CRN_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        String partition = matcher.group(1);
        Partition.safeFromString(partition);
        String region = matcher.group(3);
        Preconditions.checkState(
                Region.US_WEST_1.name.equals(region),
                String.format("%s is not a supported region", region));
        return new Crn.Builder()
                .setService(Service.safeFromString(matcher.group(2)))
                .setAccountId(matcher.group(4))
                .setResourceType(ResourceType.fromString(matcher.group(5)))
                .setResource(matcher.group(6))
                .build();
    }

    /**
     * Creates a CRN from the input string. This will explode instead of
     * returning null.
     * @param input the input string
     * @return the CRN
     */
    public static Crn safeFromString(String input) {
        return checkNotNull(fromString(input));
    }

    /**
     * Returns whether an input string is a CRN.
     * @param input the input string
     * @return whether the input string is a CRN
     */
    public static boolean isCrn(@Nullable String input) {
        if (input == null) {
            return false;
        }
        try {
            return fromString(input) != null;
        } catch (Exception e) {
            return false;
        }
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

    public static class Builder {
        // We hardcode the following for now as we have neither the partitions nor
        // regions yet.
        private final Partition partition = Partition.ALTUS;
        private final Region region = Region.US_WEST_1;

        private Service service;
        private String accountId;
        private ResourceType resourceType;
        private String resource;

        public Builder setService(Service service) {
            this.service = checkNotNull(service);
            return this;
        }

        public Builder setAccountId(String accountId) {
            this.accountId = checkNotNull(accountId);
            return this;
        }

        public Builder setResourceType(ResourceType resourceType) {
            this.resourceType = checkNotNull(resourceType);
            return this;
        }

        public Builder setResource(String resource) {
            this.resource = checkNotNull(resource);
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
    }

    public static Builder builder() {
        return new Builder();
    }
}
//CHECKSTYLE:ON