package com.sequenceiq.freeipa.api.v1.freeipa.stack.doc;

public class FreeIpaModelDescriptions {
    public static final String FREEIPA_NAME = "name of the freeipa stack";
    public static final String REGION = "region of the freeipa stack";
    public static final String AVAILABILITY_ZONE = "availability zone of the freeipa stack";
    public static final String PLACEMENT_SETTINGS = "placement configuration parameters for a cluster (e.g. 'region', 'availabilityZone')";
    public static final String INSTANCE_GROUPS = "collection of instance groups";
    public static final String AUTHENTICATION = "freeipa stack related authentication";
    public static final String NETWORK = "freeipa stack related network";
    public static final String IMAGE_SETTINGS = "settings for custom images";
    public static final String FREEIPA_ARCHITECTURE = "freeipa CPU architecture";
    public static final String FREEIPA_SERVER_SETTINGS = "settings for freeipa server";
    public static final String GATEWAY_PORT = "port of the gateway secured proxy";
    public static final String TELEMETRY = "telemetry setting for freeipa server";
    public static final String BACKUP = "backup setting for freeipa server";
    public static final String RECIPES = "recipes for freeipa server";
    public static final String CLOUD_STORAGE = "cloud storage details for freeipa server";
    public static final String FREEIPA_APPLICATION_VERSION = "version of the application provisioned FreeIPA";
    public static final String CLOUD_PLATFORM = "Cloud Platform for FreeIPA";
    public static final String USE_CCM = "whether to use CCM for communicating with the freeipa instance";
    public static final String TUNNEL = "Configuration that the connection going directly or with cluster proxy or with CCM and cluster proxy.";
    public static final String VARIANT = "Configuration of cloud platform variant.";
    public static final String TAGS = "Tags for freeipa server.";
    public static final String USERSYNC_STATUS_DETAILS = "user sync status details for the environment";
    public static final String AWS_PARAMETERS = "Aws specific FreeIpa parameters";
    public static final String AWS_SPOT_PARAMETERS = "Aws spot instance related parameters.";
    public static final String AWS_SPOT_PERCENTAGE = "Percentage of spot instances launched in FreeIpa instance group";
    public static final String AWS_SPOT_MAX_PRICE = "Max price per hour of spot instances launched in FreeIpa instance group";
    public static final String FORCE = "Force the operation by skipping some of the validations";

    public static final String MULTIAZ = "whether FreeIPA is MultiAZ or not";

    public static final String SUPPORTED_IMDS_VERSION = "IMDS version supported by the given stack.";
    public static final String LOADBALANCER_DETAILS = "freeipa loadbalancer details";
    public static final String LOADBALANCER_TYPE = "type of FreeIpa load balancer to be created, " +
            "possible values: NONE, INTERNAL_NLB, default is INTERNAL_NLB";

    private FreeIpaModelDescriptions() {
    }

    public static class InstanceGroupModelDescription {
        public static final String INSTANCE_GROUP_NAME = "name of the instance group";
        public static final String INSTANCE_GROUP_TYPE = "type of the instance group, default value is CORE";
        public static final String NODE_COUNT = "number of nodes";
        public static final String TEMPLATE = "instancegroup related template";
        public static final String SECURITYGROUP = "instancegroup related securitygroup";
        public static final String NETWORK = "referenced network";
        public static final String AVAILABILITY_ZONE = "availability zone of instance";
        public static final String AVAILABILITY_ZONES = "availability zones of instance group";
        public static final String SUBNET_ID = "subnet ID of instance";
        public static final String AWS_PARAMETERS = "provider specific parameters of the specified network";
        public static final String STATUS = "status of the instance";
        public static final String INSTANCE_TYPE = "type of the instance";
    }

    public static class InstanceMetaDataModelDescription {
        public static final String PRIVATE_IP = "private IP of the instance";
        public static final String PUBLIC_IP = "public IP of the instance";
        public static final String INSTANCE_ID = "ID of the instance";
        public static final String DISCOVERY_FQDN = "the fully qualified domain name of the node in the service discovery cluster";
    }

    public static class HostMetadataModelDescription {
        public static final String STATE = "state of the host";
    }

    public static class TemplateModelDescription {
        public static final String VOLUME_COUNT = "number of volumes";
        public static final String VOLUME_SIZE = "size of volume";
        public static final String VOLUME_TYPE = "type of the volumes";
        public static final String INSTANCE_TYPE = "type of the instance";
    }

    public static class SecurityGroupModelDescription {
        public static final String SECURITY_RULES = "list of security rules that relates to the security group";
        public static final String SECURITY_GROUP_ID = "Existing security group ID";
        public static final String SECURITY_GROUP_IDS = "Existing security group IDs";
    }

    public static class SecurityRuleModelDescription {
        public static final String SUBNET = "definition of allowed subnet in CIDR format";
        public static final String PORTS = "list of accessible ports";
        public static final String PROTOCOL = "protocol of the rule";
        public static final String MODIFIABLE = "flag for making the rule modifiable";
    }

    public static class StackAuthenticationModelDescription {
        public static final String PUBLIC_KEY = "public key for accessing instances";
        public static final String LOGIN_USERNAME = "authentication name for machines";
        public static final String PUBLIC_KEY_ID = "public key id for accessing instances";
    }

    public static class ImageSettingsModelDescription {
        public static final String IMAGE_CATALOG = "custom image catalog URL";
        public static final String IMAGE_ID = "virtual machine image id from ImageCatalog, machines of the cluster will be started from this image";
        public static final String OS_TYPE = "os type of the image, this property is only considered when no specific image id is provided";
        public static final String LDAP_AGENT_VERSION = "LDAP agent version present on the image, if the image contains LDAP agent.";
        public static final String IMDS_VERSION = "IMDS version supported on the image. If empty, then provider specific default should be expected.";
        public static final String SOURCE_IMAGE = "Parent image based on which the current image has been created.";
    }

    public static class NetworkModelDescription {
        public static final String AWS_PARAMETERS = "provider specific parameters of the specified network";
        public static final String AZURE_PARAMETERS = "provider specific parameters of the specified network";
        public static final String GCP_PARAMETERS = "provider specific parameters of the specified network";
        public static final String OPENSTACK_PARAMETERS_DEPRECATED = "provider specific parameters of the specified network";
        public static final String OUTBOUND_INTERNET_TRAFFIC = "A flag to enable or disable the outbound internet traffic from the instances.";
        public static final String NETWORK_CIDRS = "the network CIDRs which have to be reachable from the instances";
    }

    public static class FreeIpaServerSettingsModelDescriptions {
        public static final String SERVER_IP = "FreeIPA servers IP address";
        public static final String DOMAIN = "Domain name associated to the FreeIPA";
        public static final String HOSTNAME = "Base hostname for FreeIPA servers";
        public static final String ADMIN_GROUP_NAME = "Name of the admin group to be used for all the services.";
        public static final String TAGS = "Tags on freeipa.";
        public static final String FREEIPA_HOST = "A DNS load balanced FQDN to the FreeIPA servers";
        public static final String FREEIPA_PORT = "The port for the load balanced FQDN to the FreeIPA servers";
    }

    public static class FreeIpaImageSecurityModelDescriptions {
        public static final String IMAGE_SECURITY = "FreeIpa image security settings.";
        public static final String SELINUX = "SELinux enabled on the image.";
    }

    public static class FreeIpaLoadBalancerModelDescriptions {
        public static final String PRIVATE_IPS = "private IPs of the LoadBalancer";
        public static final String FQDN = "fully qualified domain name of the LoadBalancer";
        public static final String RESOURCE_ID = "resource id of the LoadBalancer";
    }

    public static class CrossRealmTrustModelDescriptions {
        public static final String FQDN = "Fully qualified domain name of the Active Directory server.";
        public static final String IP = "IP address of the Active Directory server.";
        public static final String REALM = "Realm of the Active Directory server.";
        public static final String TRUST_SECRET = "The trust shared secret is a password-like key entered during trust configuration that is shared between " +
                "the IDM and AD domains to secure and validate the trust relationship.";
        public static final String AD_TRUST_SETUP_COMMANDS = "Active directory commands to be executed for cross-realm trust setup.";
        public static final String BASE_CLUSTER_KRB5_CONF = "krb5.conf content to be used on base cluster for cross-realm trust setup.";
        public static final String TRUST_DETAILS = "Cross realm trust details.";
        public static final String TRUST_STATUS = "The cross realm trust status.";
        public static final String OPERATION_ID = "The id of the last cross realm trust setup related operation.";
    }
}
