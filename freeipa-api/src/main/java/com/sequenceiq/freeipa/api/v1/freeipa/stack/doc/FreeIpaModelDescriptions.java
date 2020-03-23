package com.sequenceiq.freeipa.api.v1.freeipa.stack.doc;

public class FreeIpaModelDescriptions {
    public static final String FREEIPA_NAME = "name of the freeipa stack";
    public static final String REGION = "region of the freeipa stack";
    public static final String AVAILABILITY_ZONE = "availability zone of the freeipa stack";
    public static final String PLACEMENT_SETTINGS = "placement configuration parameters for a cluster (e.g. 'region', 'availabilityZone')";
    public static final String INSTANCE_GROUPS = "collection of instance groupst";
    public static final String AUTHENTICATION = "freeipa stack related authentication";
    public static final String NETWORK = "freeipa stack related network";
    public static final String IMAGE_SETTINGS = "settings for custom images";
    public static final String FREEIPA_SERVER_SETTINGS = "settings for freeipa server";
    public static final String GATEWAY_PORT = "port of the gateway secured proxy";
    public static final String TELEMETRY = "telemetry setting for freeipa server";
    public static final String CLOUD_STORAGE = "cloud storage details for freeipa server";
    public static final String FREEIPA_APPLICATION_VERSION = "version of the application provisioned FreeIPA";
    public static final String CLOUD_PLATFORM = "Cloud Platform for FreeIPA";
    public static final String USE_CCM = "whether to use CCM for communicating with the freeipa instance";
    public static final String TUNNEL = "Configuration that the connection going directly or with cluster proxy or with ccm and cluster proxy.";
    public static final String TAGS = "Tags for freeipa server.";

    private FreeIpaModelDescriptions() {
    }

    public static class InstanceGroupModelDescription {
        public static final String INSTANCE_GROUP_NAME = "name of the instance group";
        public static final String INSTANCE_GROUP_TYPE = "type of the instance group, default value is CORE";
        public static final String NODE_COUNT = "number of nodes";
        public static final String TEMPLATE = "instancegroup related template";
        public static final String SECURITYGROUP = "instancegroup related securitygroup";
        public static final String STATUS = "status of the instance";
        public static final String INSTANCE_TYPE = "type of the instance";
    }

    public static class InstanceMetaDataModelDescription {
        public static final String PRIVATE_IP = "private ip of the insctance";
        public static final String PUBLIC_IP = "public ip of the instance";
        public static final String INSTANCE_ID = "id of the instance";
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
        public static final String SECURITY_GROUP_ID = "Exisiting security group id";
        public static final String SECURITY_GROUP_IDS = "Exisiting security group ids";
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
    }

    public static class NetworkModelDescription {
        public static final String AWS_PARAMETERS = "provider specific parameters of the specified network";
        public static final String AZURE_PARAMETERS = "provider specific parameters of the specified network";
        public static final String GCP_PARAMETERS = "provider specific parameters of the specified network";
        public static final String OPEN_STACK_PARAMETERS = "provider specific parameters of the specified network";
        public static final String SUBNET_CIDR = "the subnet definition of the network in CIDR format";
    }

    public static class FreeIpaServerSettingsModelDescriptions {
        public static final String SERVER_IP = "FreeIPA servers IP address";
        public static final String DOMAIN = "Domain name associated to the FreeIPA";
        public static final String HOSTNAME = "Base hostname for FreeIPA servers";
        public static final String ADMIN_GROUP_NAME = "Name of the admin group to be used for all the services.";
        public static final String TAGS = "Tags on freeipa.";
    }
}
