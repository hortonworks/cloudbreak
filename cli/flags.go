package cli

import (
	"fmt"
	"reflect"

	"github.com/hortonworks/cb-cli/cli/utils"
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
	FlAuthTypeOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "auth-type",
			Usage:  "authentication method to use, values: [" + OAUTH2 + ", " + BASIC + "]",
			EnvVar: "CB_AUTH_TYPE",
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
	FlUsername = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "username",
			Usage:  "user name (e-mail address)",
			EnvVar: "CB_USER_NAME",
		},
	}
	FlUsernameRequired = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:   "username",
			Usage:  "user name (e-mail address)",
			EnvVar: "CB_USER_NAME",
		},
	}
	FlPassword = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "password",
			Usage:  "password",
			EnvVar: "CB_PASSWORD",
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
	FlDescriptionOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "description",
			Usage: "description of resource",
		},
	}
	FlPublicOptional = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "public",
			Usage: "public in account",
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
			Usage: "location of the Ambari blueprint JSON file",
		},
	}
	FlURL = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "url",
			Usage: "URL location of the JSON file",
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
	FlExecutionType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "execution-type",
			Usage: "type of execution [pre-ambari-start, pre-termination, post-ambari-start, post-cluster-install]",
		},
	}
	FlAmbariPasswordOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "input-json-param-password",
			Usage: "password of the cluster and ambari",
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
	FlLdapDomain = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-domain",
			Usage: "ldap domain (e.g: ad.cb.com)",
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
			Usage: "semicolon separated list of group DNs that the user will be added to (e.g: OU=cloudbreak,CN=Users,DC=ad,DC=cb,DC=com;)",
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
	FlKerberosPasswordOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "kerberos-password",
			Usage: "kerberos password",
		},
	}
	FlKerberosPrincipalOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "kerberos-principal",
			Usage: "kerberos principal",
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
	FlKey = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "key",
			Usage: "key of the tag",
		},
	}
	FlValue = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "value",
			Usage: "value of the tag",
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

type BoolFlag struct {
	cli.BoolFlag
	RequiredFlag
}

func requiredFlags(flags []cli.Flag) []cli.Flag {
	required := []cli.Flag{}
	for _, flag := range flags {
		if isRequiredVisible(flag) {
			required = append(required, flag)
		}
	}
	return required
}

func optionalFlags(flags []cli.Flag) []cli.Flag {
	required := []cli.Flag{}
	for _, flag := range flags {
		if flag.GetName() != "generate-bash-completion" {
			if !isRequiredVisible(flag) {
				required = append(required, flag)
			}
		}
	}
	return required
}

func checkRequiredFlagsAndArguments(c *cli.Context) {
	checkRequiredFlags(c)
	argumentsNotAllowed(c)
}

func checkRequiredFlags(c *cli.Context) {
	missingFlags := make([]string, 0)
	for _, f := range c.Command.Flags {
		if isRequired(f) && len(c.String(f.GetName())) == 0 {
			missingFlags = append(missingFlags, f.GetName())
		}
	}
	if len(missingFlags) > 0 {
		utils.LogMissingParameterAndExit(c, missingFlags)
	}
}

func argumentsNotAllowed(c *cli.Context) {
	args := c.Args()
	if len(args) > 0 {
		utils.LogErrorAndExit(fmt.Errorf("Argument %q is not allowed for this command", args.Get(0)))
	}
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
	for _, f := range []cli.Flag{FlServerOptional, FlUsername, FlPassword, FlProfileOptional, FlAuthTypeOptional} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddResourceDefaultFlags() *FlagBuilder {
	for _, f := range []cli.Flag{FlName, FlDescriptionOptional, FlPublicOptional} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddResourceFlagsWithOptionalName() *FlagBuilder {
	for _, f := range []cli.Flag{FlNameOptional, FlDescriptionOptional, FlPublicOptional} {
		fb.flags = append(fb.flags, f)
	}
	return fb
}

func (fb *FlagBuilder) AddOutputFlag() *FlagBuilder {
	fb.flags = append(fb.flags, FlOutputOptional)
	return fb
}

func (fb *FlagBuilder) Build() []cli.Flag {
	return fb.flags
}
