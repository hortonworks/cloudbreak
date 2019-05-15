package com.sequenceiq.freeipa.api.v1.freeipa.stack.doc;

public class FreeIpaModelDescriptions {
    public static final String ENVIRONMENT_ID = "environment of the freeipa stack";
    public static final String FREEIPA_NAME = "name of the freeipa stack";
    public static final String REGION = "region of the freeipa stack";
    public static final String AVAILABILITY_ZONE = "availability zone of the freeipa stack";
    public static final String PLACEMENT_SETTINGS = "placement configuration parameters for a cluster (e.g. 'region', 'availabilityZone')";
    public static final String INSTANCE_GROUPS = "collection of instance groupst";
    public static final String AUTHENTICATION = "freeipa stack related authentication";
    public static final String NETWORK = "freeipa stack related network";
    public static final String IMAGE_SETTINGS = "settings for custom images";
    public static final String FREEIPA_SERVER_SETTINGS = "settings for freeipa server";

    private FreeIpaModelDescriptions() {
    }

    public static class InstanceGroupModelDescription {
        public static final String INSTANCE_GROUP_NAME = "name of the instance group";
        public static final String INSTANCE_GROUP_TYPE = "type of the instance group, default value is CORE";
        public static final String NODE_COUNT = "number of nodes";
        public static final String TEMPLATE = "instancegroup related template";
        public static final String SECURITYGROUP = "instancegroup related securitygroup";
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
    }
}
