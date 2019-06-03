package flags

import (
	"fmt"
	"reflect"

	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var REQUIRED = RequiredFlag{true}
var OPTIONAL = RequiredFlag{false}

var (
	FlDebugOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:   "debug",
			Usage:  "debug mode",
			EnvVar: "DEBUG",
		},
	}
	FlWaitOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "wait",
			Usage: "wait for the operation to finish, no argument required",
		},
	}
	FlInputJson = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cli-input-json",
			Usage: "user provided file with json content",
		},
	}
	FlOutputOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "output",
			Usage:  "supported formats: json, yaml, table (default: \"json\")",
			EnvVar: "CB_OUT_FORMAT",
		},
	}
	FlProfileOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "profile",
			Usage:  "selects a config profile to use",
			EnvVar: "CB_PROFILE",
		},
	}
	FlForceOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "force",
			Usage: "force the operation",
		},
	}
	FlServerOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "server",
			Usage:  "server address",
			EnvVar: "CB_SERVER_ADDRESS",
		},
	}
	FlServerRequired = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:   "server",
			Usage:  "server address",
			EnvVar: "CB_SERVER_ADDRESS",
		},
	}
	FlName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "name",
			Usage: "name of resource",
		},
	}
	FlNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "name",
			Usage: "name of resource",
		},
	}
	FlNames = StringSliceFlag{
		RequiredFlag: REQUIRED,
		StringSliceFlag: cli.StringSliceFlag{
			Name:  "name",
			Usage: "name of resource (provide option once for each name)",
		},
	}
	FlClusterToUpgrade = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cluster",
			Usage: "cluster to upgrade",
		},
	}
	FlDescriptionOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "description",
			Usage: "description of resource",
		},
	}
	FlDlOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "datalake",
			Usage: "marks the blueprint with Data Lake Ready tag",
		},
	}
	FlRoleARN = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "role-arn",
		},
	}
	FlAccessKey = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "access-key",
		},
	}
	FlSecretKey = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "secret-key",
		},
	}
	FlProjectId = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "project-id",
		},
	}
	FlServiceAccountId = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "service-account-id",
		},
	}
	FlServiceAccountPrivateKeyFile = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "service-account-private-key-file",
		},
	}
	FlServiceAccountJsonFile = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "service-account-json-file",
		},
	}
	FlTenantUser = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "tenant-user",
		},
	}
	FlTenantPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "tenant-password",
		},
	}
	FlTenantName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "tenant-name",
		},
	}
	FlEndpoint = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "endpoint",
		},
	}
	FlKeystoneScopeOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name: "keystone-scope",
		},
	}
	FlUserDomain = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "user-domain",
		},
	}
	FlProjectDomainNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name: "project-domain-name",
		},
	}
	FlDomainNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name: "domain-name",
		},
	}
	FlProjectNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name: "project-name",
		},
	}
	FlFacingOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name: "facing",
		},
	}
	FlSubscriptionId = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "subscription-id",
		},
	}
	FlTenantId = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "tenant-id",
		},
	}
	FlAppId = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "app-id",
		},
	}
	FlAppPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "app-password",
		},
	}
	FlFile = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "file",
			Usage: "location of the input JSON file",
		},
	}
	FlURL = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "url",
			Usage: "URL location of the input JSON file",
		},
	}
	FlBlueprintName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "blueprint-name",
			Usage: "name of the blueprint",
		},
	}
	FlBlueprintNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "blueprint-name",
			Usage: "name of the blueprint",
		},
	}
	FlBlueprintFileOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "blueprint-file",
			Usage: "location of the blueprint file",
		},
	}
	FlCloudStorageTypeOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "cloud-storage",
			Usage: "type of the cloud storage [wasb/WASB, adls-gen1/ADLS-GEN1, s3/S3, gcs/GCS, adls-gen2/ADLS-GEN2]",
		},
	}
	FlDefaultEncryptionOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-default-encryption",
			Usage: "default encryption for AWS instances which can use a default key",
		},
	}
	FlCustomEncryptionOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-custom-encryption",
			Usage: "custom key encryption for AWS instances which can use your custom key",
		},
	}
	FlRawEncryptionOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-raw-encryption",
			Usage: "custom encryption for GCP instances which can use your raw key",
		},
	}
	FlRsaEncryptionOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-rsa-encryption",
			Usage: "custom key encryption for GCP instances which can use your rsa key",
		},
	}
	FlKmsEncryptionOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-kms-encryption",
			Usage: "custom key encryption for GCP instances which can use your kms key",
		},
	}
	FlWithBlueprintValidation = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-blueprint-validation",
			Usage: "enable blueprint validation",
		},
	}
	FlExecutionType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "execution-type",
			Usage: "type of execution [pre-ambari-start, pre-termination, post-ambari-start, post-cluster-install]",
		},
	}
	FlCMUserOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "input-json-param-user",
			Usage: "user of the cluster and Cloudera Manager",
		},
	}
	FlCMPasswordOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "input-json-param-password",
			Usage: "password of the cluster and Cloudera Manager",
		},
	}
	FlGroupName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "group-name",
			Usage: "name of the group to scale",
		},
	}
	FlDesiredNodeCount = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "desired-node-count",
			Usage: "desired number of nodes",
		},
	}
	FlOldPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "old-password",
			Usage: "old password of ambari",
		},
	}
	FlNewPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "new-password",
			Usage: "new password of ambari",
		},
	}
	FlAmbariUser = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ambari-user",
			Usage: "user of ambari",
		},
	}
	FlLdapServer = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-server",
			Usage: "address of the ldap server (e.g: ldap://10.0.0.1:384)",
		},
	}
	FlLdapSecureOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "ldaps",
			Usage: "set ldaps if the ldap is secured with SSL",
		},
	}
	FlLdapCertificate = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "ldaps-cert-file",
			Usage: "location of the certificate file to be imported in case of LDAPS",
		},
	}
	FlLdapDomain = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-domain",
			Usage: "ldap domain (e.g: ad.dp.com)",
		},
	}
	FlLdapBindDN = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-bind-dn",
			Usage: "ldap bind dn (e.g: CN=Administrator,CN=Users,DC=ad,DC=cb,DC=com)",
		},
	}
	FlLdapUserNameAttribute = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-name-attribute",
			Usage: "ldap user name attribute",
		},
	}
	FlLdapUserObjectClass = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-object-class",
			Usage: "ldap user object class",
		},
	}
	FlLdapGroupMemberAttribute = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-member-attribute",
			Usage: "ldap group member attribute",
		},
	}
	FlLdapGroupNameAttribute = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-name-attribute",
			Usage: "ldap group name attribute",
		},
	}
	FlLdapGroupObjectClass = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-object-class",
			Usage: "ldap group object class",
		},
	}
	FlLdapBindPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-bind-password",
			Usage: "ldap bind password",
		},
	}
	FlLdapDirectoryType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-directory-type",
			Usage: "ldap directory type (LDAP or ACTIVE_DIRECTORY)",
		},
	}
	FlLdapUserSearchBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-search-base",
			Usage: "ldap user search base (e.g: CN=Users,DC=ad,DC=cb,DC=com)",
		},
	}
	FlLdapUserDnPattern = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-dn-pattern",
			Usage: "ldap userDnPattern (e.g: CN={0},DC=ad,DC=cb,DC=com)",
		},
	}
	FlLdapGroupSearchBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-search-base",
			Usage: "ldap group search base (e.g: OU=scopes,DC=ad,DC=cb,DC=com)",
		},
	}
	FlLdapAdminGroup = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "ldap-admin-group",
			Usage: "ldap group of administrators",
		},
	}
	FlLdapUserToCreate = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-to-create",
			Usage: "name of the ldap user (e.g user will create CN=user)",
		},
	}
	FlLdapUserToCreatePassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-to-create-password",
			Usage: "password of the user",
		},
	}
	FlLdapUserToCreateEmail = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-to-create-email",
			Usage: "email of the ldap user (it will set the mail attribute)",
		},
	}
	FlLdapUserToCreateBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-to-create-base",
			Usage: "base DN where the user will be created (e.g: CN=Users,DC=ad,DC=cb,DC=com)",
		},
	}
	FlLdapUserToCreateGroups = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-to-create-groups",
			Usage: "semicolon separated list of group DNs that the user will be added to (e.g: OU=dataplane,CN=Users,DC=ad,DC=cb,DC=com;)",
		},
	}
	FlLdapGroupToCreate = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-to-create",
			Usage: "name of the ldap group (e.g dataplane will create CN=dataplane)",
		},
	}
	FlLdapGroupToCreateBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-to-create-base",
			Usage: "base DN where the group will be created (e.g: CN=Users,DC=ad,DC=cb,DC=com)",
		},
	}
	FlLdapGroupToDelete = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-to-delete",
			Usage: "name of the ldap group",
		},
	}
	FlLdapGroupToDeleteBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-to-delete-base",
			Usage: "base DN from where the group will be deleted (e.g: CN=Users,DC=ad,DC=cb,DC=com)",
		},
	}
	FlLdapUserToDelete = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-to-delete",
			Usage: "name of the ldap user to delete",
		},
	}
	FlLdapUserToDeleteBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-to-delete-base",
			Usage: "base DN from where the user will be deleted (e.g: CN=Users,DC=ad,DC=cb,DC=com)",
		},
	}
	FlCredential = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "credential",
			Usage: "name of the credential",
		},
	}
	FlRegion = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "region",
			Usage: "name of the region",
		},
	}
	FlClusterShape = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cluster-shape",
			Usage: "defines the used cluster shape",
		},
	}
	FlAvailabilityZoneOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "availability-zone",
			Usage: "name of the availability zone",
		},
	}
	FlImageCatalog = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "imagecatalog",
			Usage: "name of the imagecatalog",
		},
	}
	FlImageCatalogOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "imagecatalog",
			Usage: "name of the imagecatalog",
		},
	}
	FlImageId = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "imageid",
			Usage: "id of the image",
		},
	}
	FlProxyHost = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "proxy-host",
			Usage: "hostname or ip of the proxy",
		},
	}
	FlProxyPort = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "proxy-port",
			Usage: "port of the proxy",
		},
	}
	FlProxyProtocol = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "proxy-protocol",
			Usage: "protocol of the proxy (http or https)",
			Value: "http",
		},
	}
	FlProxyUser = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "proxy-user",
			Usage: "user for the proxy if basic auth is required",
		},
	}
	FlProxyPassword = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "proxy-password",
			Usage: "password for the proxy if basic auth is required",
		},
	}
	FlRdsUserName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "db-username",
			Usage: "username to use for the jdbc connection",
		},
	}
	FlRdsPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "db-password",
			Usage: "password to use for the jdbc connection",
		},
	}
	FlRdsDriverOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "driver",
			Usage: "[DEPRECATED] has no effect",
		},
	}
	FlRdsURL = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "url",
			Usage: "JDBC connection URL in the form of jdbc:<db-type>://<address>:<port>/<db>",
		},
	}
	FlRdsDatabaseEngineOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "database-engine",
			Usage: "[DEPRECATED] has no effect",
		},
	}
	FlRdsType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "type",
			Usage: "type of database, aka the service name that will use the db like HIVE, DRUID, SUPERSET, RANGER, etc.",
		},
	}
	FlRdsValidatedOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "not-validated",
			Usage: "[DEPRECATED] has no effect, use 'dp database test ...' command instead",
		},
	}
	FlKubernetesConfigFile = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "kubernetes-config",
			Usage: "Kubernetes config file location",
		},
	}
	FLMpackURL = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "url",
			Usage: "URL of the mpack",
		},
	}
	FLMpackPurge = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "purge",
			Usage: "purge existing resources specified in purge-list",
		},
	}
	FLMpackPurgeList = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "purge-list",
			Usage: "comma separated list of resources to purge (stack-definitions,service-definitions,mpacks). By default (stack-definitions,mpacks) will be purged",
		},
	}
	FLMpackForce = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "force",
			Usage: "force install management pack",
		},
	}
	FlRdsConnectorJarURLOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "connector-jar-url",
			Usage: "URL of the jdbc jar file",
		},
	}
	FlCidrOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "cidr",
			Usage: "defines Classless Inter-Domain Routing for cluster",
		},
	}
	FlWithCustomDomainOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-custom-domain",
			Usage: "adds custom domain configuration to the template",
		},
	}
	FlWithTagsOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-tags",
			Usage: "adds user defined tags configuration to the template",
		},
	}
	FlWithImageOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "with-image",
			Usage: "adds image-catalog configuration to the template",
		},
	}
	FlWithSourceCluster = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "source-cluster",
			Usage: "source cluster to use as datalake",
		},
	}
	FlHostGroups = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "host-groups",
			Usage: "comma separated list of hostgroups where the failed nodes will be repaired",
		},
	}
	FlNodes = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "nodes",
			Usage: "comma separated list of nodes that will be repaired",
		},
	}
	FlDeleteVolumes = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "delete-volumes",
			Usage: "volumes of failed nodes will be deleted, otherwise reattaches them to newly created node instances.",
		},
	}
	FlRemoveOnly = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "remove-only",
			Usage: "the failed nodes will only be removed, otherwise the failed nodes will be removed and new nodes will be started.",
		},
	}
	FlResourceID = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "resource-id",
			Usage: "id of resource",
		},
	}
	FlAuditID = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "audit-id",
			Usage: "id of audit",
		},
	}
	FlWorkspaceOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "workspace",
			Usage:  "name of the workspace",
			EnvVar: "CB_WORKSPACE",
		},
	}
	FlUserID = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "user-id",
			Usage: "id of the user",
		},
	}
	FlUserName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "name",
			Usage: "name of user",
		},
	}
	FlUserNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "name",
			Usage: "name of user",
		},
	}
	FlCaasStrateyProvider = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "identity-provider-meta-path",
			Usage: "identity provider meta path url",
		},
	}
	FlCaasStrategyName = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "strategy-name",
			Usage: "name of strategy",
		},
	}
	FlCaasTenantName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "name",
			Usage: "name of tenant",
		},
	}
	FlCaasTenantLabel = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "label",
			Usage: "Display name for tenant",
		},
	}
	FlCaasTenantEmail = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "email",
			Usage: "email of the tenant",
		},
	}
	FlRolesIDs = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "role-ids",
			Usage: "comma seperated values of roles ids",
		},
	}
	FlRoleNames = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "role-names",
			Usage: "comma seperated values of role names",
		},
	}
	FlVersion = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "version",
			Usage: "component version",
		},
	}
	FlVdfUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "vdf-url",
			Usage: "vdf url",
		},
	}
	FlMPackUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "mpack-url",
			Usage: "mpack url",
		},
	}
	FlRepoUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "repo-url",
			Usage: "repository url",
		},
	}
	FlRepoGpgUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "gpg-url",
			Usage: "repository GPG url",
		},
	}
	FlEnvironmentCredential = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "credential",
			Usage: "name of the credential",
		},
	}
	FlEnvironmentCredentialOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "credential",
			Usage: "name of the credential",
		},
	}
	FlEnvironmentRegions = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "regions",
			Usage: "region names for the environment",
		},
	}
	FlLdapNamesOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "ldap-names",
			Usage: "ldap config names delimited by comma",
		},
	}
	FlProxyNamesOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "proxy-names",
			Usage: "proxy config names delimited by comma",
		},
	}
	FlKerberosNamesOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "kerberos-names",
			Usage: "kerberos config names delimited by comma",
		},
	}
	FlRdsNamesOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "rds-names",
			Usage: "rds config names delimited by comma",
		},
	}
	FlLdapNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "ldap-name",
			Usage: "ldap config name",
		},
	}
	FlEnvironmentName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "env-name",
			Usage: "name of an environment",
		},
	}
	FlEnvironmentNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "env-name",
			Usage: "name of an environment",
		},
	}
	FlEnvironmentTemplateFile = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "file",
			Usage: "location of the environment JSON template file",
		},
	}
	FlEnvironments = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "environments",
			Usage: "environment names to be used for attaching, detaching resources",
		},
	}
	FlEnvironmentsOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "environments",
			Usage: "names of environments in which the created resource should be attached during the creation",
		},
	}
	FlEnvironmentLocationName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "location-name",
			Usage: "location name of the environment. must be one of the regions",
		},
	}
	FlEnvironmentLocationNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "location-name",
			Usage: "location name of the environment. must be one of the regions",
		},
	}
	FlEnvironmentLongitudeOptional = Float64Flag{
		RequiredFlag: OPTIONAL,
		Float64Flag: cli.Float64Flag{
			Name:  "longitude",
			Usage: "longitude of the environment's location. must be specified if the location is made-up and not supported on the cloud provider",
		},
	}
	FlEnvironmentLatitudeOptional = Float64Flag{
		RequiredFlag: OPTIONAL,
		Float64Flag: cli.Float64Flag{
			Name:  "latitude",
			Usage: "latitude of the environment's location. must be specified if the location is made-up and not supported on the cloud provider",
		},
	}
	FlRangerAdminPasswordOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "ranger-admin-password",
			Usage: "ranger admin password",
		},
	}
	FlKerberosNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "kerberos-name",
			Usage: "kerberos config name",
		},
	}
	FlKerberosPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "password",
			Usage: "kerberos password",
		},
	}
	FlKerberosPrincipal = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "principal",
			Usage: "kerberos principal",
		},
	}
	FlKerberosDisableVerifyKdcTrust = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "disable-verify-kdc-trust",
			Usage: "disable kdc-trust verification",
		},
	}
	FlKerberosDomain = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "domain",
			Usage: "kerberos domain",
		},
	}
	FlKerberosNameServers = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "nameservers",
			Usage: "kerberos nameservers",
		},
	}
	FlKerberosTcpAllowed = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "tcp-allowed",
			Usage: "kerberos tcp-allowed flag",
		},
	}
	FlKerberosDescriptor = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "descriptor",
			Usage: "kerberos descriptor",
		},
	}
	FlKerberosKrb5Conf = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "krb5Conf",
			Usage: "kerberos krb5Conf",
		},
	}
	FlKerberosUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "url",
			Usage: "kerberos url",
		},
	}
	FlKerberosAdminUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "admin-url",
			Usage: "kerberos admin url",
		},
	}
	FlKerberosRealm = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "realm",
			Usage: "kerberos realm",
		},
	}
	FlKerberosLdapUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-url",
			Usage: "kerberos LDAP url",
		},
	}
	FlKerberosContainerDn = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "container-dn",
			Usage: "kerberos container dn",
		},
	}
	FlKerberosAdmin = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "admin",
			Usage: "kerberos admin",
		},
	}
	FlTimeoutMinutes = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "minutes",
			Usage: "timeout (minutes)",
		},
	}
	FlTimeoutHours = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "hours",
			Usage: "timeout (hours)",
		},
	}
	FlTimeoutDays = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "days",
			Usage: "timeout (hours)",
		},
	}
	FlShowUsage = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "show-usage",
			Usage: "shows the command usage",
		},
	}
	FlShowInternal = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "show-internal",
			Usage: "shows the internal commands as well",
		},
	}

	FlApiKeyIDOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "apikeyid",
			Usage: "API key ID",
		},
	}
	FlPrivateKeyOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "privatekey",
			Usage: "API private key",
		},
	}
	FlWriteToProfileOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "write-to-profile",
			Usage: "writes the values into the profile",
		},
	}
)

type RequiredFlag struct {
	Required bool
}

type StringFlag struct {
	cli.StringFlag
	RequiredFlag
}

type StringSliceFlag struct {
	cli.StringSliceFlag
	RequiredFlag
}

type BoolFlag struct {
	cli.BoolFlag
	RequiredFlag
}

type IntFlag struct {
	cli.IntFlag
	RequiredFlag
}

type Int64Flag struct {
	cli.Int64Flag
	RequiredFlag
}

type Float64Flag struct {
	cli.Float64Flag
	RequiredFlag
}

func RequiredFlags(flags []cli.Flag) []cli.Flag {
	var required []cli.Flag
	for _, flag := range flags {
		if isRequiredVisible(flag) {
			required = append(required, flag)
		}
	}
	return required
}

func OptionalFlags(flags []cli.Flag) []cli.Flag {
	var required []cli.Flag
	for _, flag := range flags {
		if flag.GetName() != "generate-bash-completion" {
			if !isRequiredVisible(flag) {
				required = append(required, flag)
			}
		}
	}
	return required
}

func CheckRequiredFlagsAndArguments(c *cli.Context) error {
	if err := argumentsNotAllowed(c); err != nil {
		return err
	}
	return checkRequiredFlags(c)
}

func checkRequiredFlags(c *cli.Context) error {
	missingFlags := make([]string, 0)
	for _, f := range c.Command.Flags {
		if isRequired(f) {
			strf, ok := f.(StringFlag)
			if ok && len(c.String(strf.GetName())) == 0 {
				missingFlags = append(missingFlags, f.GetName())
			}
			strslf, ok := f.(StringSliceFlag)
			if ok && len(c.StringSlice(strslf.GetName())) == 0 {
				missingFlags = append(missingFlags, f.GetName())
			}
		}
	}
	if len(missingFlags) > 0 {
		return utils.LogMissingParameter(c, missingFlags)
	}

	return nil
}

func argumentsNotAllowed(c *cli.Context) error {
	args := c.Args()
	if len(args) > 0 {
		msg := fmt.Sprintf("Argument %q is not allowed for this command. If you've specified a parameter for a logical "+
			"(true/false) flag please remove it or use flag=true/false format.", args.Get(0))
		utils.LogErrorMessage(msg)
		return fmt.Errorf("no allowed argument")
	}
	return nil
}

func isRequiredVisible(flag cli.Flag) bool {
	if flag.GetName() == "help, h" || flag.GetName() == "generate-bash-completion" {
		return false
	}
	hidden := flagValue(flag).FieldByName("Hidden").Bool()
	required := flagValue(flag).FieldByName("Required").Bool()
	return !hidden && required
}

func isRequired(flag cli.Flag) (required bool) {
	defer func() {
		if r := recover(); r != nil {
			required = false
		}
	}()
	if flag.GetName() == "help, h" {
		return false
	}
	return flagValue(flag).FieldByName("Required").Bool()
}

func flagValue(flag cli.Flag) reflect.Value {
	fv := reflect.ValueOf(flag)
	for fv.Kind() == reflect.Ptr {
		fv = reflect.Indirect(fv)
	}
	return fv
}

type FlagBuilder struct {
	flags []cli.Flag
}

func NewFlagBuilder() *FlagBuilder {
	return &FlagBuilder{flags: make([]cli.Flag, 0)}
}

func (fb *FlagBuilder) AddFlags(flags ...cli.Flag) *FlagBuilder {
	for _, f := range flags {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddAuthenticationFlags() *FlagBuilder {
	for _, f := range []cli.Flag{FlServerOptional, FlWorkspaceOptional, FlProfileOptional, FlApiKeyIDOptional, FlPrivateKeyOptional} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddAuthenticationFlagsWithoutWorkspace() *FlagBuilder {
	for _, f := range []cli.Flag{FlServerOptional, FlProfileOptional, FlApiKeyIDOptional, FlPrivateKeyOptional} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddResourceDefaultFlags() *FlagBuilder {
	for _, f := range []cli.Flag{FlName, FlDescriptionOptional} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddResourceFlagsWithOptionalName() *FlagBuilder {
	for _, f := range []cli.Flag{FlNameOptional, FlDescriptionOptional} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddOutputFlag() *FlagBuilder {
	fb.flags = append(fb.flags, FlOutputOptional)
	return fb
}

func (fb *FlagBuilder) AddTemplateFlags() *FlagBuilder {
	for _, f := range []cli.Flag{FlWithCustomDomainOptional, FlWithTagsOptional, FlWithImageOptional, FlWithBlueprintValidation} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddCommonKerberosCreateFlags() *FlagBuilder {
	for _, f := range []cli.Flag{FlEnvironmentsOptional, FlKerberosDisableVerifyKdcTrust, FlKerberosDomain, FlKerberosNameServers,
		FlKerberosPassword, FlKerberosTcpAllowed, FlKerberosPrincipal, FlKerberosAdmin} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) Build() []cli.Flag {
	return fb.flags
}

func PrintFlagCompletion(f cli.Flag) {
	fmt.Printf("--%s\n", f.GetName())
}
