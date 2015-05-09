package com.sequenceiq.cloudbreak.controller.doc;

public class ModelDescriptions {

    public static final String ID = "id of the resource";
    public static final String NAME = "name of the resource";
    public static final String DESCRIPTION = "description of the resource";
    public static final String PUBLIC_IN_ACCOUNT = "resource is visible in account";
    public static final String CLOUD_PLATFORM = "type of cloud provider";

    private ModelDescriptions() {
    }

    public static class BlueprintModelDescription {
        public static final String URL = "url source of an ambari blueprint, set this or the ambariBlueprint field";
        public static final String BLUEPRINT_NAME = "gathered from blueprintName field from the blueprint JSON";
        public static final String AMBARI_BLUEPRINT = "ambari blueprint JSON, set this or the url field";
        public static final String HOST_GROUP_COUNT = "number of host groups";
    }

    public static class CredentialModelDescription {
        public static final String PUBLIC_KEY = "public key for accessing instances";
        public static final String PARAMETERS = "cloud specific parameters for credential";
    }

    public static class TemplateModelDescription {
        public static final String VOLUME_COUNT = "number of volumes";
        public static final String VOLUME_SIZE = "size of volumes";
        public static final String PARAMETERS = "cloud specific parameters for template";
    }

    public static class StackModelDescription {
        public static final String STACK_ID = "id of the stack";
        public static final String STACK_NAME = "name of the stack";
        public static final String REGION = "region of the stack";
        public static final String CREDENTIAL_ID = "credential resource id for the stack";
        public static final String USERNAME = "ambari username";
        public static final String PASSWORD = "ambari password";
        public static final String IMAGE = "name of the image for instance creation";
        public static final String CONSUL_SERVER_COUNT = "consul server count";
        public static final String CONSUL_SERVER_COUNT_BY_USER = "user defined consul server count";
        public static final String PARAMETERS = "additional cloud specific parameters for stack";
        public static final String ALLOWED_SUBNETS = "allowed subnets";
        public static final String FAILURE_ACTION = "action on failure";
        public static final String FAILURE_POLICY = "failure policy in case of failures";
        public static final String STATUS = "status of the stack";
        public static final String STATUS_REASON = "status message of the stack";
        public static final String OWNER = "userId of the stack owner";
        public static final String ACCOUNT = "user account of the stack";
        public static final String AMBARI_IP = "public ambari ip of the stack";
        public static final String HASH = "unique hash identifier for stack";
        public static final String BLUEPRINT_ID = "id of the referenced blueprint";
    }

    public static class ClusterModelDescription {
        public static final String STATUS = "status of the cluster";
        public static final String STATUS_REASON = "status message of the cluster";
        public static final String CLUSTER_NAME = "name of the cluster";
        public static final String BLUEPRINT_ID = "blueprint id for the cluster";
        public static final String HOURS = "duration - how long the cluster is running in hours";
        public static final String MINUTES = "duration - how long the cluster is running in minutes (minus hours)";
        public static final String EMAIL_NEEDED = "send email about the result of the cluster installation";
    }

    public static class RecipeModelDescription {
        public static final String TIMEOUT = "recipe timeout in minutes";
        public static final String PLUGINS = "list of consul plugins with execution types";
        public static final String PROPERTIES = "additional plugin properties";
    }

    public static class InstanceGroupModelDescription {
        public static final String INSTANCE_GROUP_NAME = "name of the instance group";
        public static final String INSTANCE_GROUP_TYPE = "type of the instance group";
        public static final String NODE_COUNT = "number of nodes";
        public static final String TEMPLATE_ID = "referenced template id";
    }

    public static class InstanceGroupAdjustmentModelDescription {
        public static final String SCALING_ADJUSTMENT = "scaling adjustment of the instance groups";
        public static final String WITH_CLUSTER_EVENT = "on stack update, update cluster too";
    }

    public static class HostGroupModelDescription {
        public static final String RECIPE_IDS = "referenced recipe ids";
        public static final String HOST_GROUP_NAME = "name of the host group";
    }

    public static class HostGroupAdjustmentModelDescription {
        public static final String SCALING_ADJUSTMENT = "scaling adjustment of the host groups";
        public static final String WITH_STACK_UPDATE = "on cluster update, update stack too";
    }

    public static class InstanceMetaDataModelDescription {
        public static final String PRIVATE_IP = "private ip of the insctance";
        public static final String PUBLIC_IP = "public ip of the instance";
        public static final String INSTANCE_ID = "id of the instance";
        public static final String VOLUME_COUNT = "number of volumes";
        public static final String AMBARI_SERVER = "ambari server address";
        public static final String DOCKER_SUBNET = "docker subnet";
        public static final String DISCOVERY_FQDN = "the fully qualified domain name of the node in the service discovery cluster";
        public static final String CONTAINER_COUNT = "number of the containers";
    }

    public static class FailurePolicyModelDescription {
        public static final String THRESHOLD = "threshold of failure policy";
    }

    public static class UsageModelDescription {
        public static final String PROVIDER = "cloud provider of the stack";
        public static final String COSTS = "computed costs of instance usage";
        public static final String DAY = "days since the instance is running";
        public static final String INSTANCE_HOURS = "hours since the instance is running";
        public static final String INSTANCE_TYPE = "type of instance";
        public static final String INSTANCE_GROUP = "group name of instance";
    }

    public static class EventModelDescription {
        public static final String NODE_COUNT = "computed node count of the stack";
        public static final String TYPE = "type of the event";
        public static final String TIMESTAMP = "timestamp of the event";
        public static final String MESSAGE = "message of the event";
    }

}
