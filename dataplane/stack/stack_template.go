package stack

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/hortonworks/cb-cli/dataplane/utils"
	"strconv"
	"strings"

	"github.com/hortonworks/cb-cli/dataplane/clusterdefinition"
	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/types"
	commonUtils "github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var defaultNodes = []cloud.Node{
	{Name: "master", GroupType: model.InstanceGroupV4ResponseTypeGATEWAY, Count: 1},
	{Name: "worker", GroupType: model.InstanceGroupV4ResponseTypeCORE, Count: 3},
	{Name: "compute", GroupType: model.InstanceGroupV4ResponseTypeCORE, Count: 0},
}

var maxCardinality = map[string]int{
	"1":   1,
	"0-1": 1,
	"1-2": 2,
	"0+":  3,
	"1+":  3,
	"ALL": 3,
}

var getClusterDefinitionClient = func(server, refreshToken string) clusterdefinition.GetClusterDefinitionInWorkspace {
	cbClient := oauth.NewCloudbreakHTTPClient(server, refreshToken)
	return cbClient.Cloudbreak.V4WorkspaceIDClusterdefinitions
}

var stackClient = func(server, refreshToken string) getStackInWorkspace {
	cbClient := oauth.NewCloudbreakHTTPClient(server, refreshToken)
	return cbClient.Cloudbreak.V4WorkspaceIDStacks
}

func GenerateAwsStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AWS)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getClusterDefinitionClient, getCloudStorageType(c.String)))
}

func GenerateAzureStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AZURE)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getClusterDefinitionClient, getCloudStorageType(c.String)))
}

func GenerateGcpStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.GCP)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getClusterDefinitionClient, getCloudStorageType(c.String)))
}

func GenerateOpenstackStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.OPENSTACK)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getClusterDefinitionClient, getCloudStorageType(c.String)))
}

func GenerateYarnStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.YARN)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getClusterDefinitionClient, getCloudStorageType(c.String)))
}

func GenerateAtachedStackTemplate(c *cli.Context) error {
	return generateAttachedTemplateImpl(c.String, c.Bool, c.Int64, getCloudStorageType(c.String))
}

func getNetworkMode(c *cli.Context) cloud.NetworkMode {
	switch c.Command.Names()[0] {
	case "new-network":
		return cloud.NEW_NETWORK_NEW_SUBNET
	case "existing-network":
		return cloud.EXISTING_NETWORK_NEW_SUBNET
	case "existing-subnet":
		return cloud.EXISTING_NETWORK_EXISTING_SUBNET
	case "legacy-network":
		return cloud.LEGACY_NETWORK
	case "shared-network":
		return cloud.SHARED_NETWORK
	default:
		return cloud.NO_NETWORK
	}
}

func getCloudStorageType(stringFinder func(string) string) cloud.CloudStorageType {
	storageType := stringFinder(fl.FlCloudStorageTypeOptional.Name)
	switch strings.ToLower(storageType) {
	case "adls-gen1":
		return cloud.ADLS_GEN1
	case "wasb":
		return cloud.WASB
	case "gcs":
		return cloud.GCS
	case "s3":
		return cloud.S3
	case "adls-gen2":
		return cloud.ADLS_GEN2
	default:
		return cloud.NO_CLOUD_STORAGE
	}
}

func getString(skippedFields map[string]bool, fieldName string, defaultValue string) string {
	if _, skipped := skippedFields[fieldName]; skipped {
		return ""
	}
	return defaultValue
}

func getStringPointer(skippedFields map[string]bool, fieldName string, defaultValue string) *string {
	if _, skipped := skippedFields[fieldName]; skipped {
		return &(&types.S{S: ""}).S
	}
	return &defaultValue
}

func generateStackTemplateImpl(mode cloud.NetworkMode, stringFinder func(string) string, boolFinder func(string) bool, int64Finder func(string) int64, getClusterDefinitionClient func(string, string) clusterdefinition.GetClusterDefinitionInWorkspace, storageType cloud.CloudStorageType) *model.StackV4Request {
	provider := cloud.GetProvider()
	skippedFields := provider.SkippedFields()

	template := model.StackV4Request{
		Cluster: &model.ClusterV4Request{
			Ambari: &model.AmbariV4Request{
				ClusterDefinitionName:     "____",
				UserName:                  &(&types.S{S: "____"}).S,
				Password:                  &(&types.S{S: ""}).S,
				ValidateClusterDefinition: &(&types.B{B: false}).B,
			},
		},
		Name: &(&types.S{S: ""}).S,
		Environment: &model.EnvironmentSettingsV4Request{
			CredentialName: "____",
		},
		Placement: &model.PlacementSettingsV4Request{
			Region:           getStringPointer(skippedFields, cloud.REGION_FIELD, "____"),
			AvailabilityZone: getString(skippedFields, cloud.AVAILABILITY_ZONE_FIELD, "____"),
		},
		InstanceGroups: []*model.InstanceGroupV4Request{},
		Network:        provider.GenerateDefaultNetwork(mode),
		Authentication: &model.StackAuthenticationV4Request{
			PublicKey: "____",
		},
	}
	preExtendTemplateWithOptionalBlocks(&template, boolFinder, storageType)
	nodes := defaultNodes
	if bpName := stringFinder(fl.FlClusterDefinitionNameOptional.Name); len(bpName) != 0 {
		workspace := int64Finder(fl.FlWorkspaceOptional.Name)
		bpResp := clusterdefinition.FetchClusterDefinition(workspace, bpName, getClusterDefinitionClient(stringFinder(fl.FlServerOptional.Name), stringFinder(fl.FlRefreshTokenOptional.Name)))
		bp, err := base64.StdEncoding.DecodeString(bpResp.ClusterDefinition)
		if err != nil {
			commonUtils.LogErrorAndExit(err)
		}
		nodes = getNodesByClusterDefinition(bp)
		template.Cluster.Ambari.ClusterDefinitionName = bpName
	} else if bpFile := stringFinder(fl.FlClusterDefinitionFileOptional.Name); len(bpFile) != 0 {
		bp := commonUtils.ReadFile(bpFile)
		nodes = getNodesByClusterDefinition(bp)
	}
	for _, node := range nodes {
		template.InstanceGroups = append(template.InstanceGroups, convertNodeToInstanceGroup(provider, node))
	}
	provider.SetParametersTemplate(&template)
	postExtendTemplateWithOptionalBlocks(&template, boolFinder)
	return &template
}

func generateAttachedTemplateImpl(stringFinder func(string) string, boolFinder func(string) bool, int64Finder func(string) int64, storageType cloud.CloudStorageType) error {
	datalake := fetchStack(int64Finder(fl.FlWorkspaceOptional.Name), stringFinder(fl.FlWithSourceCluster.Name), stackClient(stringFinder(fl.FlServerOptional.Name), stringFinder(fl.FlRefreshTokenOptional.Name)))
	isSharedServiceReady, _ := datalake.Cluster.Ambari.Blueprint.Tags["shared_services_ready"].(bool)
	if !isSharedServiceReady {
		commonUtils.LogErrorMessageAndExit("The source cluster must be a datalake")
	} else if datalake.Status != "AVAILABLE" || datalake.Cluster.Status != "AVAILABLE" {
		commonUtils.LogErrorMessageAndExit("Datalake must be in available state")
	} else {
		env := datalake.Environment
		if env == nil {
			commonUtils.LogErrorAndExit(errors.New("the datalake does not belong to any environment"))
		}
		if len(env.CloudPlatform) == 0 {
			commonUtils.LogErrorAndExit(errors.New("the cloud platform is not specified for the source cluster"))
		}
		cloud.SetProviderType(cloud.CloudType(env.CloudPlatform))
		attachedClusterTemplate := generateStackTemplateImpl(cloud.EXISTING_NETWORK_EXISTING_SUBNET, stringFinder, boolFinder, int64Finder, getClusterDefinitionClient, storageType)
		attachedClusterTemplate.Placement.Region = datalake.Placement.Region
		attachedClusterTemplate.Placement.AvailabilityZone = datalake.Placement.AvailabilityZone
		attachedClusterTemplate.Environment.CredentialName = *env.Credential.Name
		attachedClusterTemplate.Network = cloud.GetProvider().GenerateNetworkRequestFromNetworkResponse(datalake.Network)
		attachedClusterTemplate.Cluster.LdapName = *datalake.Cluster.Ldap.Name
		attachedClusterTemplate.Cluster.CloudStorage = generateCloudStorage(datalake.Cluster.CloudStorage)
		for _, rds := range datalake.Cluster.Databases {
			if *rds.Type == "RANGER" || *rds.Type == "HIVE" {
				attachedClusterTemplate.Cluster.Databases = append(attachedClusterTemplate.Cluster.Databases, *rds.Name)
			}
		}
		sourceCluster := stringFinder(fl.FlWithSourceCluster.Name)
		attachedClusterTemplate.SharedService = &model.SharedServiceV4Request{
			DatalakeName: &sourceCluster,
		}

		return printTemplate(*attachedClusterTemplate)
	}
	return nil
}

func generateCloudStorage(fileSystem *model.CloudStorageV4Response) *model.CloudStorageV4Request {
	if fileSystem != nil {
		return &model.CloudStorageV4Request{
			S3:        fileSystem.S3,
			Adls:      fileSystem.Adls,
			Gcs:       fileSystem.Gcs,
			Wasb:      fileSystem.Wasb,
			AdlsGen2:  fileSystem.AdlsGen2,
			Locations: generateLocations(fileSystem.Locations),
		}
	}
	return nil
}

func generateLocations(datalake []*model.StorageLocationV4Response) []*model.StorageLocationV4Request {
	result := make([]*model.StorageLocationV4Request, len(datalake))
	for i, loc := range datalake {
		result[i] = &model.StorageLocationV4Request{
			PropertyFile: loc.PropertyFile,
			PropertyName: loc.PropertyName,
			Value:        loc.Value,
		}
	}
	return result
}

func printTemplate(template model.StackV4Request) error {
	resp, err := json.MarshalIndent(template, "", "\t")
	if err != nil {
		commonUtils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
	return nil
}

func getNodesByClusterDefinition(bp []byte) []cloud.Node {
	var bpJson map[string]interface{}
	if err := json.Unmarshal(bp, &bpJson); err != nil {
		commonUtils.LogErrorAndExit(err)
	}
	var nodes []*cloud.Node
	if bpJson["host_groups"] == nil {
		commonUtils.LogErrorMessageAndExit("host_groups not found in cluster definition")
	}
	var gateway *cloud.Node
	for _, e := range bpJson["host_groups"].([]interface{}) {
		var hg = e.(map[string]interface{})
		var count = 1
		if hg["cardinality"] != nil {
			cardinality := hg["cardinality"].(string)
			count = maxCardinality[cardinality]
			if count == 0 {
				var err error
				count, err = strconv.Atoi(cardinality)
				if err != nil {
					commonUtils.LogErrorMessageAndExit("Unable to parse as number: " + cardinality)
				}
			}
		}
		if hg["name"] == nil {
			commonUtils.LogErrorMessageAndExit("host group name not found in cluster definition")
		}
		node := cloud.Node{Name: hg["name"].(string), GroupType: model.InstanceGroupV4ResponseTypeCORE, Count: int32(count)}
		nodes = append(nodes, &node)
		if gateway == nil || gateway.Count > node.Count {
			gateway = &node
		}
	}
	gateway.GroupType = model.InstanceGroupV4ResponseTypeGATEWAY
	var resp []cloud.Node
	for _, n := range nodes {
		resp = append(resp, *n)
	}
	return resp
}

func preExtendTemplateWithOptionalBlocks(template *model.StackV4Request, boolFinder func(string) bool, storageType cloud.CloudStorageType) {
	if withCustomDomain := boolFinder(fl.FlWithCustomDomainOptional.Name); withCustomDomain {
		template.CustomDomain = &model.CustomDomainSettingsV4Request{
			DomainName:              "____",
			Hostname:                "____",
			ClusterNameAsSubdomain:  &(&types.B{B: false}).B,
			HostgroupNameAsHostname: &(&types.B{B: false}).B,
		}
	}
	if withTags := boolFinder(fl.FlWithTagsOptional.Name); withTags {
		template.Tags = &model.TagsV4Request{
			UserDefined: map[string]string{
				"____": "____",
			},
		}
	}
	if withImage := boolFinder(fl.FlWithImageOptional.Name); withImage {
		template.Image = &model.ImageSettingsV4Request{
			Catalog: "____",
			ID:      "____",
		}
	}
	if withClusterDefinitionValidation := boolFinder(fl.FlWithClusterDefinitionValidation.Name); withClusterDefinitionValidation {
		template.Cluster.Ambari.ValidateClusterDefinition = &(&types.B{B: true}).B
	}
	extendTemplateWithStorageType(template, storageType)
}

func postExtendTemplateWithOptionalBlocks(template *model.StackV4Request, boolFinder func(string) bool) {
	extendTemplateWithEncryptionType(template, boolFinder)
}

func extendTemplateWithEncryptionType(template *model.StackV4Request, boolFinder func(string) bool) {
	if withCustomEncryption := boolFinder(fl.FlCustomEncryptionOptional.Name); withCustomEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.Aws = &model.AwsInstanceTemplateV4Parameters{
				Encryption: &model.AwsEncryptionV4Parameters{
					Type: "CUSTOM",
					Key:  "____",
				},
			}
		}
	} else if withDefaultEncryption := boolFinder(fl.FlDefaultEncryptionOptional.Name); withDefaultEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.Aws = &model.AwsInstanceTemplateV4Parameters{
				Encryption: &model.AwsEncryptionV4Parameters{
					Type: "DEFAULT",
				},
			}
		}
	} else if withRawEncryption := boolFinder(fl.FlRawEncryptionOptional.Name); withRawEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.Gcp = &model.GcpInstanceTemplateV4Parameters{
				Encryption: &model.GcpEncryptionV4Parameters{
					Type:                "CUSTOM",
					KeyEncryptionMethod: "RAW",
					Key:                 "____",
				},
			}
		}
	} else if withRsaEncryption := boolFinder(fl.FlRsaEncryptionOptional.Name); withRsaEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.Gcp = &model.GcpInstanceTemplateV4Parameters{
				Encryption: &model.GcpEncryptionV4Parameters{
					Type:                "CUSTOM",
					KeyEncryptionMethod: "RSA",
					Key:                 "____",
				},
			}
		}
	} else if withKmsEncryption := boolFinder(fl.FlKmsEncryptionOptional.Name); withKmsEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.Gcp = &model.GcpInstanceTemplateV4Parameters{
				Encryption: &model.GcpEncryptionV4Parameters{
					Type:                "CUSTOM",
					KeyEncryptionMethod: "KMS",
					Key:                 "____",
				},
			}
		}
	}
}

func extendTemplateWithStorageType(template *model.StackV4Request, storageType cloud.CloudStorageType) {
	if storageType == cloud.WASB {
		template.Cluster.CloudStorage = &model.CloudStorageV4Request{
			Wasb: &model.WasbCloudStorageV4Parameters{
				AccountKey:  &(&types.S{S: "____"}).S,
				AccountName: &(&types.S{S: "____"}).S,
				Secure:      &(&types.B{B: false}).B,
			},
			Locations: []*model.StorageLocationV4Request{},
		}
	} else if storageType == cloud.ADLS_GEN1 {
		template.Cluster.CloudStorage = &model.CloudStorageV4Request{
			Adls: &model.AdlsCloudStorageV4Parameters{
				AccountName: &(&types.S{S: "____"}).S,
				ClientID:    &(&types.S{S: "____"}).S,
				Credential:  &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationV4Request{},
		}
	} else if storageType == cloud.S3 {
		template.Cluster.CloudStorage = &model.CloudStorageV4Request{
			S3: &model.S3CloudStorageV4Parameters{
				InstanceProfile: &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationV4Request{},
		}
	} else if storageType == cloud.GCS {
		template.Cluster.CloudStorage = &model.CloudStorageV4Request{
			Gcs: &model.GcsCloudStorageV4Parameters{
				ServiceAccountEmail: &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationV4Request{},
		}
	} else if storageType == cloud.ADLS_GEN2 {
		template.Cluster.CloudStorage = &model.CloudStorageV4Request{
			AdlsGen2: &model.AdlsGen2CloudStorageV4Parameters{
				AccountKey:  &(&types.S{S: "____"}).S,
				AccountName: &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationV4Request{},
		}
	}
}

func convertNodeToInstanceGroup(provider cloud.CloudProvider, node cloud.Node) *model.InstanceGroupV4Request {
	ig := &model.InstanceGroupV4Request{
		Name:          &node.Name,
		Template:      provider.GenerateDefaultTemplate(),
		NodeCount:     &node.Count,
		Type:          node.GroupType,
		SecurityGroup: provider.GenerateDefaultSecurityGroup(node),
		RecipeNames:   []string{},
	}
	provider.SetInstanceGroupParametersTemplate(ig, node)
	return ig
}

func GenerateReinstallTemplate(c *cli.Context) {
	template := &model.ReinstallV4Request{
		ClusterDefinition: &(&types.S{S: c.String(fl.FlClusterDefinitionName.Name)}).S,
		InstanceGroups:    []*model.InstanceGroupV4Request{},
	}

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	stackName := c.String(fl.FlName.Name)
	stackResp := fetchStack(workspaceID, stackName, stackClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlRefreshTokenOptional.Name)))
	cloudPlatform := utils.SafeCloudPlatformConvert(stackResp.Environment)
	provider, ok := cloud.CloudProviders[cloud.CloudType(cloudPlatform)]
	if !ok {
		commonUtils.LogErrorMessageAndExit("not supported cloud provider")
	}

	bpName := c.String(fl.FlClusterDefinitionNameOptional.Name)
	workspace := c.Int64(fl.FlWorkspaceOptional.Name)
	bpResp := clusterdefinition.FetchClusterDefinition(workspace, bpName, getClusterDefinitionClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlRefreshTokenOptional.Name)))
	bp, err := base64.StdEncoding.DecodeString(bpResp.ClusterDefinition)
	if err != nil {
		commonUtils.LogErrorAndExit(err)
	}

	for _, node := range getNodesByClusterDefinition(bp) {
		ig := &model.InstanceGroupV4Request{
			Name:          &(&types.S{S: node.Name}).S,
			NodeCount:     &(&types.I32{I: node.Count}).I,
			RecoveryMode:  "AUTO",
			SecurityGroup: provider.GenerateDefaultSecurityGroup(node),
			Template:      provider.GenerateDefaultTemplate(),
			Type:          node.GroupType,
		}
		template.InstanceGroups = append(template.InstanceGroups, ig)
	}

	resp, err := json.MarshalIndent(template, "", "\t")
	if err != nil {
		commonUtils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
}
