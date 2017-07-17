package com.sequenceiq.cloudbreak.doc;

public class OperationDescriptions {
    public static class BlueprintOpDescription {
        public static final String POST_PRIVATE = "create blueprint as private resource";
        public static final String POST_PUBLIC = "create blueprint as public resource";
        public static final String GET_PRIVATE = "retrieve private blueprints";
        public static final String GET_PUBLIC = "retrieve public and private (owned) blueprints";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private blueprint by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) blueprint by name";
        public static final String GET_BY_ID = "retrieve blueprint by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private blueprint by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private blueprint by name";
        public static final String DELETE_BY_ID = "delete blueprint by id";
    }

    public static class TemplateOpDescription {
        public static final String POST_PRIVATE = "create template as private resource";
        public static final String POST_PUBLIC = "create template as public resource";
        public static final String GET_PRIVATE = "retrieve private templates";
        public static final String GET_PUBLIC = "retrieve public and private (owned) templates";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private template by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) template by name";
        public static final String GET_BY_ID = "retrieve template by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private template by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private template by name";
        public static final String DELETE_BY_ID = "delete template by id";
    }

    public static class ConstraintOpDescription {
        public static final String POST_PRIVATE = "create constraint template as private resource";
        public static final String POST_PUBLIC = "create constraint template as public resource";
        public static final String GET_PRIVATE = "retrieve private constraint templates";
        public static final String GET_PUBLIC = "retrieve public and private (owned) constraint templates";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private constraint template by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) constraint template by name";
        public static final String GET_BY_ID = "retrieve constraint template by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private constraint template by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private constraint template by name";
        public static final String DELETE_BY_ID = "delete constraint template by id";
    }

    public static class TopologyOpDesctiption {
        public static final String GET_BY_ID = "retrieve topology by id";
        public static final String GET_PUBLIC = "retrieve topoligies";
        public static final String POST_PUBLIC = "create topology as public resource";
        public static final String DELETE_BY_ID = "delete topology by id";
    }

    public static class CredentialOpDescription {
        public static final String POST_PRIVATE = "create credential as private resource";
        public static final String POST_PUBLIC = "create credential as public resource";
        public static final String GET_PRIVATE = "retrieve private credentials";
        public static final String GET_PUBLIC = "retrieve public and private (owned) credentials";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private credential by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) credential by name";
        public static final String GET_BY_ID = "retrieve credential by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private credential by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private credential by name";
        public static final String INTERACTIVE_LOGIN = "interactive login";
        public static final String DELETE_BY_ID = "delete credential by id";
        public static final String GET_JKS_FILE = "retrieve azure JKS file by credential id";
        public static final String PUT_CERTIFICATE_BY_ID = "update azure credential by credential id";
        public static final String GET_SSH_FILE = "retrieve azure ssh key file for credential by credential id";
    }

    public static class StackOpDescription {
        public static final String POST_PRIVATE = "create stack as private resource";
        public static final String POST_PUBLIC = "create stack as public resource";
        public static final String GET_PRIVATE = "retrieve private stack";
        public static final String GET_PUBLIC = "retrieve public and private (owned) stacks";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private stack by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) stack by name";
        public static final String GET_BY_ID = "retrieve stack by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private stack by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private stack by name";
        public static final String DELETE_BY_ID = "delete stack by id";
        public static final String GET_STATUS_BY_ID = "retrieve stack status by stack id";
        public static final String PUT_BY_ID = "update stack by id";
        public static final String GET_METADATA = "retrieve stack metadata";
        public static final String GET_BY_AMBARI_ADDRESS = "retrieve stack by ambari address";
        public static final String GET_STACK_CERT = "retrieves the TLS certificate used by the gateway";
        public static final String VALIDATE = "validate stack";
        public static final String DELETE_INSTANCE_BY_ID = "delete instance resource from stack";
        public static final String GET_PLATFORM_VARIANTS = "retrieve available platform variants";
        public static final String GET_ALL = "retrieve all stacks";
    }

    public static class ClusterOpDescription {
        public static final String POST_FOR_STACK = "create cluster for stack";
        public static final String GET_BY_STACK_ID = "retrieve cluster by stack id";
        public static final String GET_PRIVATE_BY_NAME = "retrieve cluster by stack name (private)";
        public static final String GET_PUBLIC_BY_NAME = "retrieve cluster by stack name (public)";
        public static final String DELETE_BY_STACK_ID = "delete cluster on a specific stack";
        public static final String PUT_BY_STACK_ID = "update cluster by stack id";
        public static final String UPGRADE_AMBARI = "upgrade the Ambari version";
        public static final String GET_CLUSTER_PROPERTIES = "get cluster properties with blueprint outputs";
        public static final String FAILURE_REPORT = "failure report";
        public static final String REPAIR_CLUSTER = "repair the cluster";
    }

    public static class ClusterTemplateOpDescription {
        public static final String POST_PRIVATE = "create cluster template as private resource";
        public static final String POST_PUBLIC = "create cluster template as public resource";
        public static final String GET_PRIVATE = "retrieve private cluster templates";
        public static final String GET_PUBLIC = "retrieve public and private (owned) cluster template";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private cluster template by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) cluster template by name";
        public static final String GET_BY_ID = "retrieve cluster template by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private cluster template by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private cluster template by name";
        public static final String DELETE_BY_ID = "delete cluster template by id";
    }

    public static class RecipeOpDescription {
        public static final String POST_PRIVATE = "create recipe as private resource";
        public static final String POST_PUBLIC = "create recipe as public resource";
        public static final String GET_PRIVATE = "retrieve private recipes";
        public static final String GET_PUBLIC = "retrieve public and private (owned) recipes";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private recipe by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) recipe by name";
        public static final String GET_BY_ID = "retrieve recipe by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private recipe by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private recipe by name";
        public static final String DELETE_BY_ID = "delete recipe by id";
    }

    public static class SssdConfigOpDescription {
        public static final String POST_PRIVATE = "create SSSD config as private resource";
        public static final String POST_PUBLIC = "create SSSD config as public resource";
        public static final String GET_PRIVATE = "retrieve private SSSD configs";
        public static final String GET_PUBLIC = "retrieve public and private (owned) SSSD configs";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private SSSD config by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) SSSD config by name";
        public static final String GET_BY_ID = "retrieve SSSD config by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private SSSD config by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private SSSD config by name";
        public static final String DELETE_BY_ID = "delete SSSD config by id";
    }

    public static class UsagesOpDescription {
        public static final String GET_ALL = "retrieve usages by filter parameters";
        public static final String GET_PUBLIC = "retrieve public and private (owned) usages by filter parameters";
        public static final String GET_PRIVATE = "retrieve private usages by filter parameters";
        public static final String GET_FLEX_DAILY = "retrieve Flex related daily usages";
        public static final String GET_FLEX_LATEST = "retrieve Flex related latest usages, usages for the given day";
    }

    public static class EventOpDescription {
        public static final String GET_BY_TIMESTAMP = "retrieve events by timestamp (long)";
        public static final String GET_BY_ID = "retrieve events by stackid (long)";
    }

    public static class NetworkOpDescription {
        public static final String POST_PRIVATE = "create network as private resource";
        public static final String POST_PUBLIC = "create network as public resource";
        public static final String GET_PRIVATE = "retrieve private networks";
        public static final String GET_PUBLIC = "retrieve public and private (owned) networks";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private network by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) network by name";
        public static final String GET_BY_ID = "retrieve network by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private network by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private network by name";
        public static final String DELETE_BY_ID = "delete network by id";
    }

    public static class UserOpDescription {
        public static final String USER_DETAILS_EVICT = "remove user from cache (by username)";
        public static final String CURRENT_USER_DETAILS_EVICT = "remove current user from cache";
        public static final String USER_GET_RESOURCE = "check that account user has any resources";
    }

    public static class SecurityGroupOpDescription {
        public static final String POST_PRIVATE = "create security group as private resource";
        public static final String POST_PUBLIC = "create security group as public resource";
        public static final String GET_PRIVATE = "retrieve private security groups";
        public static final String GET_PUBLIC = "retrieve public and private (owned) security groups";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private security group by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) security group by name";
        public static final String GET_BY_ID = "retrieve security group by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private security group by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private security group by name";
        public static final String DELETE_BY_ID = "delete security group by id";
    }

    public static class AccountPreferencesDescription {
        public static final String GET_PRIVATE = "retrieve account preferences for admin user";
        public static final String IS_PLATFORM_SELECTION_DISABLED = "is platform selection disabled";
        public static final String PUT_PRIVATE = "update account preferences of admin user";
        public static final String POST_PRIVATE = "post account preferences of admin user";
        public static final String VALIDATE = "validate account preferences of all stacks";
    }

    public static class LdapConfigOpDescription {
        public static final String POST_PRIVATE = "create LDAP config as private resource";
        public static final String POST_PUBLIC = "create LDAP config as public resource";
        public static final String GET_PRIVATE = "retrieve private LDAP configs";
        public static final String GET_PUBLIC = "retrieve public and private (owned) LDAP configs";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private LDAP config by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) LDAP config by name";
        public static final String GET_BY_ID = "retrieve LDAP config by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private LDAP config by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private LDAP config by name";
        public static final String DELETE_BY_ID = "delete LDAP config by id";
    }

    public static class UtilityOpDescription {
        public static final String TEST_RDS_CONNECTION = "tests an RDS connection";
        public static final String TEST_RDS_CONNECTION_BY_ID = "tests an already exists RDS connection";
        public static final String TEST_LDAP_CONNECTION = "tests an LDAP connection";
        public static final String TEST_LDAP_CONNECTION_BY_ID = "tests an already exists LDAP connection";
        public static final String TEST_DATABASE = "tests a database connection parameters";
        public static final String CREATE_DATABASE = "create a database connection parameters";
    }

    public static class RdsConfigOpDescription {
        public static final String POST_PRIVATE = "create RDS configuration as private resource";
        public static final String POST_PUBLIC = "create RDS configuration as public resource";
        public static final String GET_PRIVATE = "retrieve private RDS configurations";
        public static final String GET_PUBLIC = "retrieve public and private (owned) RDS configurations";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private RDS configuration by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) RDS configuration by name";
        public static final String GET_BY_ID = "retrieve RDS configuration by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private RDS configuration by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private RDS configuration by name";
        public static final String DELETE_BY_ID = "delete RDS configuration by id";
    }

    public static class ConnectorOpDescription {
        public static final String GET_PLATFORMS = "retrive available platforms";
        public static final String GET_PLATFORM_VARIANTS = "retrive available platform variants";
        public static final String GET_PLATFORM_VARIANT_BY_TYPE = "retrive a platform variant by type";
        public static final String GET_DISK_TYPES = "retrive available disk types";
        public static final String GET_DISK_TYPE_BY_TYPE = "retrive disks by type";
        public static final String GET_ORCHESTRATOR_TYPES = "retrive available orchestrator types";
        public static final String GET_ORCHESTRATORS_BY_TYPES = "retrive orchestrators by type";
        public static final String GET_VM_TYPES = "retrive available vm types";
        public static final String GET_VM_TYPE_BY_TYPE = "retrive vm types by type";
        public static final String GET_REGIONS = "retrive available regions";
        public static final String GET_REGION_R_BY_TYPE = "retrive regions by type";
        public static final String GET_REGION_AV_BY_TYPE = "retrive availability zones by type";
        public static final String GET_IMAGES = "retrive available images";
        public static final String GET_TAG_SPECIFICATIONS = "retrive tag specifications";
        public static final String GET_SPECIALS = "retrive special properties";
        public static final String GET_IMAGE_R_BY_TYPE = "retrive images by type";
    }

    public static class SettingsOpDescription {
        public static final String GET_ALL_SETTINGS = "retrive all available settings";
        public static final String GET_RECIPE_SETTINGS = "retrive available recipe settings";
        public static final String GET_SSSD_SETTINGS = "retrive available SSSD configuration settings";
        public static final String GET_DATABASE_SETTINGS = "retrive available Ambari database settings";
    }

    public static class SubscriptionOpDescription {
        public static final String SUBSCRIBE = "retrive subscribe identifier";
    }

    public static class SmartSenseSubOpDescription {
        public static final String POST_PRIVATE = "create SmartSense subscription as private resource";
        public static final String POST_PUBLIC = "create SmartSense subscription as public resource";
        public static final String GET_PRIVATE = "retrieve private SmartSense subscriptions";
        public static final String GET_PUBLIC = "retrieve public and private (owned) SmartSense subscriptions";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private SmartSense subscription by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) SmartSense subscription by name";
        public static final String GET = "retrieve default SmartSense subscription";
        public static final String GET_BY_ID = "retrieve SmartSense subscription by id";
        public static final String DELETE_PRIVATE_BY_ID = "delete private SmartSense subscription by subscription ID";
        public static final String DELETE_PUBLIC_BY_ID = "delete public (owned) or private SmartSense subscription by subscription ID";
        public static final String DELETE_BY_ID = "delete SmartSense subscription by id";
    }

    public static class FlexSubOpDescription {
        public static final String POST_PRIVATE = "create Flex subscription as private resource";
        public static final String POST_PUBLIC = "create Flex subscription as public resource";
        public static final String GET_PRIVATE = "retrieve private Flex subscriptions";
        public static final String GET_PUBLIC = "retrieve public and private (owned) Flex subscriptions";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private Flex subscription by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) Flex subscription by name";
        public static final String GET_BY_ID = "retrieve Flex subscription by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private Flex subscription by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private Flex subscription by name";
        public static final String DELETE_BY_ID = "delete Flex subscription by id";
        public static final String SET_DEFAULT_IN_ACCOUNT = "sets the account default flag on the Flex subscription";
        public static final String SET_USED_FOR_CONTROLLER_IN_ACCOUNT = "sets the account 'used for controller' flag on the Flex subscription";
    }
}
