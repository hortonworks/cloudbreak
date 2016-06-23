package com.sequenceiq.cloudbreak.doc

class OperationDescriptions {
    object BlueprintOpDescription {
        val POST_PRIVATE = "create blueprint as private resource"
        val POST_PUBLIC = "create blueprint as public resource"
        val GET_PRIVATE = "retrieve private blueprints"
        val GET_PUBLIC = "retrieve public and private (owned) blueprints"
        val GET_PRIVATE_BY_NAME = "retrieve a private blueprint by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) blueprint by name"
        val GET_BY_ID = "retrieve blueprint by id"
        val DELETE_PRIVATE_BY_NAME = "delete private blueprint by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private blueprint by name"
        val DELETE_BY_ID = "delete blueprint by id"
    }

    object TemplateOpDescription {
        val POST_PRIVATE = "create template as private resource"
        val POST_PUBLIC = "create template as public resource"
        val GET_PRIVATE = "retrieve private templates"
        val GET_PUBLIC = "retrieve public and private (owned) templates"
        val GET_PRIVATE_BY_NAME = "retrieve a private template by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) template by name"
        val GET_BY_ID = "retrieve template by id"
        val DELETE_PRIVATE_BY_NAME = "delete private template by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private template by name"
        val DELETE_BY_ID = "delete template by id"
    }

    object ConstraintOpDescription {
        val POST_PRIVATE = "create constraint template as private resource"
        val POST_PUBLIC = "create constraint template as public resource"
        val GET_PRIVATE = "retrieve private constraint templates"
        val GET_PUBLIC = "retrieve public and private (owned) constraint templates"
        val GET_PRIVATE_BY_NAME = "retrieve a private constraint template by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) constraint template by name"
        val GET_BY_ID = "retrieve constraint template by id"
        val DELETE_PRIVATE_BY_NAME = "delete private constraint template by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private constraint template by name"
        val DELETE_BY_ID = "delete constraint template by id"
    }

    object TopologyOpDesctiption {
        val GET_BY_ID = "retrieve topology by id"
        val GET_PUBLIC = "retrieve topoligies"
        val POST_PUBLIC = "create topology as public resource"
        val DELETE_BY_ID = "delete topology by id"
    }

    object CredentialOpDescription {
        val POST_PRIVATE = "create credential as private resource"
        val POST_PUBLIC = "create credential as public resource"
        val GET_PRIVATE = "retrieve private credentials"
        val GET_PUBLIC = "retrieve public and private (owned) credentials"
        val GET_PRIVATE_BY_NAME = "retrieve a private credential by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) credential by name"
        val GET_BY_ID = "retrieve credential by id"
        val DELETE_PRIVATE_BY_NAME = "delete private credential by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private credential by name"
        val DELETE_BY_ID = "delete credential by id"
        val GET_JKS_FILE = "retrieve azure JKS file by credential id"
        val PUT_CERTIFICATE_BY_ID = "update azure credential by credential id"
        val GET_SSH_FILE = "retrieve azure ssh key file for credential by credential id"
    }

    object StackOpDescription {
        val POST_PRIVATE = "create stack as private resource"
        val POST_PUBLIC = "create stack as public resource"
        val GET_PRIVATE = "retrieve private stack"
        val GET_PUBLIC = "retrieve public and private (owned) stacks"
        val GET_PRIVATE_BY_NAME = "retrieve a private stack by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) stack by name"
        val GET_BY_ID = "retrieve stack by id"
        val DELETE_PRIVATE_BY_NAME = "delete private stack by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private stack by name"
        val DELETE_BY_ID = "delete stack by id"
        val GET_STATUS_BY_ID = "retrieve stack status by stack id"
        val PUT_BY_ID = "update stack by id"
        val GET_METADATA = "retrieve stack metadata"
        val GET_BY_AMBARI_ADDRESS = "retrieve stack by ambari address"
        val GET_STACK_CERT = "retrieves the TLS certificate used by the gateway"
        val VALIDATE = "validate stack"
        val DELETE_INSTANCE_BY_ID = "delete instance resource from stack"
        val GET_PLATFORM_VARIANTS = "retrieve available platform variants"
    }

    object ClusterOpDescription {
        val POST_FOR_STACK = "create cluster for stack"
        val GET_BY_STACK_ID = "retrieve cluster by stack id"
        val GET_PRIVATE_BY_NAME = "retrieve cluster by stack name (private)"
        val GET_PUBLIC_BY_NAME = "retrieve cluster by stack name (public)"
        val DELETE_BY_STACK_ID = "delete cluster on a specific stack"
        val PUT_BY_STACK_ID = "update cluster by stack id"
    }

    object RecipeOpDescription {
        val POST_PRIVATE = "create recipe as private resource"
        val POST_PUBLIC = "create recipe as public resource"
        val GET_PRIVATE = "retrieve private recipes"
        val GET_PUBLIC = "retrieve public and private (owned) recipes"
        val GET_PRIVATE_BY_NAME = "retrieve a private recipe by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) recipe by name"
        val GET_BY_ID = "retrieve recipe by id"
        val DELETE_PRIVATE_BY_NAME = "delete private recipe by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private recipe by name"
        val DELETE_BY_ID = "delete recipe by id"
    }

    object SssdConfigOpDescription {
        val POST_PRIVATE = "create SSSD config as private resource"
        val POST_PUBLIC = "create SSSD config as public resource"
        val GET_PRIVATE = "retrieve private SSSD configs"
        val GET_PUBLIC = "retrieve public and private (owned) SSSD configs"
        val GET_PRIVATE_BY_NAME = "retrieve a private SSSD config by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) SSSD config by name"
        val GET_BY_ID = "retrieve SSSD config by id"
        val DELETE_PRIVATE_BY_NAME = "delete private SSSD config by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private SSSD config by name"
        val DELETE_BY_ID = "delete SSSD config by id"
    }

    object UsagesOpDescription {
        val GET_ALL = "retrieve usages by filter parameters"
        val GET_PUBLIC = "retrieve public and private (owned) usages by filter parameters"
        val GET_PRIVATE = "retrieve private usages by filter parameters"
        val GENERATE = "generate usages"
    }

    object EventOpDescription {
        val GET_BY_TIMESTAMP = "retrieve events by timestamp (long)"
    }

    object NetworkOpDescription {
        val POST_PRIVATE = "create network as private resource"
        val POST_PUBLIC = "create network as public resource"
        val GET_PRIVATE = "retrieve private networks"
        val GET_PUBLIC = "retrieve public and private (owned) networks"
        val GET_PRIVATE_BY_NAME = "retrieve a private network by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) network by name"
        val GET_BY_ID = "retrieve network by id"
        val DELETE_PRIVATE_BY_NAME = "delete private network by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private network by name"
        val DELETE_BY_ID = "delete network by id"
    }

    object UserOpDescription {
        val USER_DETAILS_EVICT = "remove user from cache (by username)"
        val USER_GET_RESOURCE = "check that account user has any resources"
    }

    object SecurityGroupOpDescription {
        val POST_PRIVATE = "create security group as private resource"
        val POST_PUBLIC = "create security group as public resource"
        val GET_PRIVATE = "retrieve private security groups"
        val GET_PUBLIC = "retrieve public and private (owned) security groups"
        val GET_PRIVATE_BY_NAME = "retrieve a private security group by name"
        val GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) security group by name"
        val GET_BY_ID = "retrieve security group by id"
        val DELETE_PRIVATE_BY_NAME = "delete private security group by name"
        val DELETE_PUBLIC_BY_NAME = "delete public (owned) or private security group by name"
        val DELETE_BY_ID = "delete security group by id"
    }

    object AccountPreferencesDescription {
        val GET_PRIVATE = "retrieve account preferences for admin user"
        val PUT_PRIVATE = "update account preferences of admin user"
        val POST_PRIVATE = "post account preferences of admin user"
        val VALIDATE = "validate account preferences of all stacks"
    }

    object UtilityOpDescription {
        val TEST_CONNECTION = "tests a RDS connection"
    }
}
