package stack

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/hortonworks/cb-cli/cloudbreak/blueprint"
	"github.com/hortonworks/cb-cli/cloudbreak/oauth"
	"strconv"
	"strings"

	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/cloud"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/types"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/urfave/cli"
)

var defaultNodes = []cloud.Node{
	{Name: "master", GroupType: model.InstanceGroupResponseTypeGATEWAY, Count: 1},
	{Name: "worker", GroupType: model.InstanceGroupResponseTypeCORE, Count: 3},
	{Name: "compute", GroupType: model.InstanceGroupResponseTypeCORE, Count: 0},
}

var maxCardinality = map[string]int{
	"1":   1,
	"0-1": 1,
	"1-2": 2,
	"0+":  3,
	"1+":  3,
	"ALL": 3,
}

var getBlueprintClient = func(server, userName, password, authType string) blueprint.GetBlueprintInWorkspace {
	cbClient := oauth.NewCloudbreakHTTPClient(server, userName, password, authType)
	return cbClient.Cloudbreak.V3WorkspaceIDBlueprints
}

var stackClient = func(server, userName, password, authType string) getStackInWorkspace {
	cbClient := oauth.NewCloudbreakHTTPClient(server, userName, password, authType)
	return cbClient.Cloudbreak.V3WorkspaceIDStacks
}

func GenerateAwsStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AWS)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getBlueprintClient, getCloudStorageType(c.String)))
}

func GenerateAzureStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AZURE)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getBlueprintClient, getCloudStorageType(c.String)))
}

func GenerateGcpStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.GCP)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getBlueprintClient, getCloudStorageType(c.String)))
}

func GenerateOpenstackStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.OPENSTACK)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getBlueprintClient, getCloudStorageType(c.String)))
}

func GenerateYarnStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.YARN)
	return printTemplate(*generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, c.Int64, getBlueprintClient, getCloudStorageType(c.String)))
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
	default:
		return cloud.NO_NETWORK
	}
}

func getCloudStorageType(stringFinder func(string) string) cloud.CloudStorageType {
	storageType := stringFinder(fl.FlCloudStorageTypeOptional.Name)
	switch strings.ToLower(storageType) {
	case "adls":
		return cloud.ADLS
	case "wasb":
		return cloud.WASB
	case "gcs":
		return cloud.GCS
	case "s3":
		return cloud.S3
	case "abfs":
		return cloud.ABFS
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

func generateStackTemplateImpl(mode cloud.NetworkMode, stringFinder func(string) string, boolFinder func(string) bool, int64Finder func(string) int64, getBlueprintClient func(string, string, string, string) blueprint.GetBlueprintInWorkspace, storageType cloud.CloudStorageType) *model.StackV2Request {
	provider := cloud.GetProvider()
	skippedFields := provider.SkippedFields()

	template := model.StackV2Request{
		Cluster: &model.ClusterV2Request{
			Ambari: &model.AmbariV2Request{
				BlueprintName: "____",
				UserName:      &(&types.S{S: "____"}).S,
				Password:      &(&types.S{S: ""}).S,
			},
		},
		General: &model.GeneralSettings{
			Name:           &(&types.S{S: ""}).S,
			CredentialName: &(&types.S{S: "____"}).S,
		},
		Placement: &model.PlacementSettings{
			Region:           getStringPointer(skippedFields, cloud.REGION_FIELD, "____"),
			AvailabilityZone: getString(skippedFields, cloud.AVAILABILITY_ZONE_FIELD, "____"),
		},
		InstanceGroups:      []*model.InstanceGroupsV2{},
		Network:             provider.GenerateDefaultNetwork(provider.GetNetworkParamatersTemplate(mode), mode),
		StackAuthentication: &model.StackAuthentication{PublicKey: "____"},
	}
	preExtendTemplateWithOptionalBlocks(&template, boolFinder, storageType)
	nodes := defaultNodes
	if bpName := stringFinder(fl.FlBlueprintNameOptional.Name); len(bpName) != 0 {
		workspace := int64Finder(fl.FlWorkspaceOptional.Name)
		bpResp := blueprint.FetchBlueprint(workspace, bpName, getBlueprintClient(stringFinder(fl.FlServerOptional.Name), stringFinder(fl.FlUsername.Name), stringFinder(fl.FlPassword.Name), stringFinder(fl.FlAuthTypeOptional.Name)))
		bp, err := base64.StdEncoding.DecodeString(bpResp.AmbariBlueprint)
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		nodes = getNodesByBlueprint(bp)
		template.Cluster.Ambari.BlueprintName = bpName
	} else if bpFile := stringFinder(fl.FlBlueprintFileOptional.Name); len(bpFile) != 0 {
		bp := utils.ReadFile(bpFile)
		nodes = getNodesByBlueprint(bp)
	}
	for _, node := range nodes {
		template.InstanceGroups = append(template.InstanceGroups, convertNodeToInstanceGroup(provider, node))
	}

	if params := provider.GetParamatersTemplate(); params != nil {
		template.Parameters = params
	}
	postExtendTemplateWithOptionalBlocks(&template, boolFinder)
	return &template
}

func generateAttachedTemplateImpl(stringFinder func(string) string, boolFinder func(string) bool, int64Finder func(string) int64, storageType cloud.CloudStorageType) error {
	datalake := fetchStack(int64Finder(fl.FlWorkspaceOptional.Name), stringFinder(fl.FlWithSourceCluster.Name), stackClient(stringFinder(fl.FlServerOptional.Name), stringFinder(fl.FlUsername.Name), stringFinder(fl.FlPassword.Name), stringFinder(fl.FlAuthTypeOptional.Name)))
	isSharedServiceReady, _ := datalake.Cluster.Blueprint.Tags["shared_services_ready"].(bool)
	if !isSharedServiceReady {
		utils.LogErrorMessageAndExit("The source cluster must be a datalake")
	} else if datalake.Status != "AVAILABLE" || datalake.Cluster.Status != "AVAILABLE" {
		utils.LogErrorMessageAndExit("Datalake must be in available state")
	} else {
		cloud.SetProviderType(cloud.CloudType(datalake.CloudPlatform))
		attachedClusterTemplate := generateStackTemplateImpl(cloud.EXISTING_NETWORK_EXISTING_SUBNET, stringFinder, boolFinder, int64Finder, getBlueprintClient, storageType)
		attachedClusterTemplate.Placement.Region = &datalake.Region
		attachedClusterTemplate.Placement.AvailabilityZone = datalake.AvailabilityZone
		attachedClusterTemplate.General.CredentialName = datalake.Credential.Name
		attachedClusterTemplate.Network = cloud.GetProvider().GenerateNetworkRequestFromNetworkResponse(datalake.Network)
		attachedClusterTemplate.Cluster.LdapConfigName = *datalake.Cluster.LdapConfig.Name
		attachedClusterTemplate.Cluster.CloudStorage = generateCloudStorage(datalake.Cluster.FileSystemResponse)
		for _, rds := range datalake.Cluster.RdsConfigs {
			if *rds.Type == "RANGER" || *rds.Type == "HIVE" {
				attachedClusterTemplate.Cluster.RdsConfigNames = append(attachedClusterTemplate.Cluster.RdsConfigNames, *rds.Name)
			}
		}
		attachedClusterTemplate.Cluster.SharedService = &model.SharedService{
			SharedCluster: stringFinder(fl.FlWithSourceCluster.Name),
		}

		return printTemplate(*attachedClusterTemplate)
	}
	return nil
}

func generateCloudStorage(fileSystem *model.FileSystemResponse) *model.CloudStorageRequest {
	if fileSystem != nil {
		return &model.CloudStorageRequest{
			S3:        fileSystem.S3,
			Adls:      fileSystem.Adls,
			Gcs:       fileSystem.Gcs,
			Wasb:      fileSystem.Wasb,
			Abfs:      fileSystem.Abfs,
			Locations: generateLocations(fileSystem.Locations),
		}
	}
	return nil
}

func generateLocations(datalake []*model.StorageLocationResponse) []*model.StorageLocationRequest {
	result := make([]*model.StorageLocationRequest, len(datalake))
	for i, loc := range datalake {
		result[i] = &model.StorageLocationRequest{
			PropertyFile: loc.PropertyFile,
			PropertyName: loc.PropertyName,
			Value:        loc.Value,
		}
	}
	return result
}

func printTemplate(template model.StackV2Request) error {
	resp, err := json.MarshalIndent(template, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
	return nil
}

func getNodesByBlueprint(bp []byte) []cloud.Node {
	var bpJson map[string]interface{}
	if err := json.Unmarshal(bp, &bpJson); err != nil {
		utils.LogErrorAndExit(err)
	}
	var nodes []*cloud.Node
	if bpJson["host_groups"] == nil {
		utils.LogErrorMessageAndExit("host_groups not found in blueprint")
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
					utils.LogErrorMessageAndExit("Unable to parse as number: " + cardinality)
				}
			}
		}
		if hg["name"] == nil {
			utils.LogErrorMessageAndExit("host group name not found in blueprint")
		}
		node := cloud.Node{Name: hg["name"].(string), GroupType: model.InstanceGroupResponseTypeCORE, Count: int32(count)}
		nodes = append(nodes, &node)
		if gateway == nil || gateway.Count > node.Count {
			gateway = &node
		}
	}
	gateway.GroupType = model.InstanceGroupResponseTypeGATEWAY
	var resp []cloud.Node
	for _, n := range nodes {
		resp = append(resp, *n)
	}
	return resp
}

func preExtendTemplateWithOptionalBlocks(template *model.StackV2Request, boolFinder func(string) bool, storageType cloud.CloudStorageType) {
	if withCustomDomain := boolFinder(fl.FlWithCustomDomainOptional.Name); withCustomDomain {
		template.CustomDomain = &model.CustomDomainSettings{
			CustomDomain:            "____",
			CustomHostname:          "____",
			ClusterNameAsSubdomain:  &(&types.B{B: false}).B,
			HostgroupNameAsHostname: &(&types.B{B: false}).B,
		}
	}
	if withTags := boolFinder(fl.FlWithTagsOptional.Name); withTags {
		template.Tags = &model.Tags{
			UserDefinedTags: map[string]string{
				"____": "____",
			},
		}
	}
	if withImage := boolFinder(fl.FlWithImageOptional.Name); withImage {
		template.ImageSettings = &model.ImageSettings{
			ImageCatalog: "____",
			ImageID:      "____",
		}
	}
	if withKerberosManaged := boolFinder(fl.FlWithKerberosManagedOptional.Name); withKerberosManaged {
		template.Cluster.Ambari.Kerberos = &model.KerberosRequest{
			TCPAllowed: &(&types.B{B: false}).B,
			MasterKey:  "____",
			Admin:      "____",
			Password:   "____",
		}
	}
	if withKerberosMIT := boolFinder(fl.FlWithKerberosExistingMITOptional.Name); withKerberosMIT {
		template.Cluster.Ambari.Kerberos = &model.KerberosRequest{
			TCPAllowed: &(&types.B{B: false}).B,
			Principal:  "____",
			Password:   "____",
			URL:        "____",
			AdminURL:   "____",
			Realm:      "____",
		}
	}
	if withKerberosAD := boolFinder(fl.FlWithKerberosExistingADOptional.Name); withKerberosAD {
		template.Cluster.Ambari.Kerberos = &model.KerberosRequest{
			TCPAllowed:  &(&types.B{B: false}).B,
			Principal:   "____",
			Password:    "____",
			URL:         "____",
			AdminURL:    "____",
			Realm:       "____",
			LdapURL:     "____",
			ContainerDn: "____",
		}
	}
	if withKerberosCustom := boolFinder(fl.FlWithKerberosCustomOptional.Name); withKerberosCustom {
		template.Cluster.Ambari.Kerberos = &model.KerberosRequest{
			TCPAllowed: &(&types.B{B: false}).B,
			Principal:  "____",
			Password:   "____",
			Descriptor: "____",
			Krb5Conf:   "____",
		}
	}
	extendTemplateWithStorageType(template, storageType)
}

func postExtendTemplateWithOptionalBlocks(template *model.StackV2Request, boolFinder func(string) bool) {
	extendTemplateWithEncryptionType(template, boolFinder)
}

func extendTemplateWithEncryptionType(template *model.StackV2Request, boolFinder func(string) bool) {
	if withCustomEncryption := boolFinder(fl.FlCustomEncryptionOptional.Name); withCustomEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.AwsParameters = &model.AwsParameters{
				Encrypted: &(&types.B{B: true}).B,
				Encryption: &model.AwsEncryption{
					Type: "CUSTOM",
					Key:  "____",
				},
			}
		}
	} else if withDefaultEncryption := boolFinder(fl.FlDefaultEncryptionOptional.Name); withDefaultEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.AwsParameters = &model.AwsParameters{
				Encrypted: &(&types.B{B: true}).B,
				Encryption: &model.AwsEncryption{
					Type: "DEFAULT",
				},
			}
		}
	} else if withRawEncryption := boolFinder(fl.FlRawEncryptionOptional.Name); withRawEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.GcpParameters = &model.GcpParameters{
				Encryption: &model.GcpEncryption{
					Type:                "CUSTOM",
					KeyEncryptionMethod: "RAW",
					Key:                 "____",
				},
			}
		}
	} else if withRsaEncryption := boolFinder(fl.FlRsaEncryptionOptional.Name); withRsaEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.GcpParameters = &model.GcpParameters{
				Encryption: &model.GcpEncryption{
					Type:                "CUSTOM",
					KeyEncryptionMethod: "RSA",
					Key:                 "____",
				},
			}
		}
	} else if withKmsEncryption := boolFinder(fl.FlKmsEncryptionOptional.Name); withKmsEncryption {
		for _, group := range template.InstanceGroups {
			group.Template.GcpParameters = &model.GcpParameters{
				Encryption: &model.GcpEncryption{
					Type:                "CUSTOM",
					KeyEncryptionMethod: "KMS",
					Key:                 "____",
				},
			}
		}
	}
}

func extendTemplateWithStorageType(template *model.StackV2Request, storageType cloud.CloudStorageType) {
	if storageType == cloud.WASB {
		template.Cluster.CloudStorage = &model.CloudStorageRequest{
			Wasb: &model.WasbCloudStorageParameters{
				AccountKey:  &(&types.S{S: "____"}).S,
				AccountName: &(&types.S{S: "____"}).S,
				Secure:      &(&types.B{B: false}).B,
			},
			Locations: []*model.StorageLocationRequest{},
		}
	} else if storageType == cloud.ADLS {
		template.Cluster.CloudStorage = &model.CloudStorageRequest{
			Adls: &model.AdlsCloudStorageParameters{
				AccountName: &(&types.S{S: "____"}).S,
				ClientID:    &(&types.S{S: "____"}).S,
				Credential:  &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationRequest{},
		}
	} else if storageType == cloud.S3 {
		template.Cluster.CloudStorage = &model.CloudStorageRequest{
			S3: &model.S3CloudStorageParameters{
				InstanceProfile: &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationRequest{},
		}
	} else if storageType == cloud.GCS {
		template.Cluster.CloudStorage = &model.CloudStorageRequest{
			Gcs: &model.GcsCloudStorageParameters{
				ServiceAccountEmail: &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationRequest{},
		}
	} else if storageType == cloud.ABFS {
		template.Cluster.CloudStorage = &model.CloudStorageRequest{
			Abfs: &model.AbfsCloudStorageParameters{
				AccountKey:  &(&types.S{S: "____"}).S,
				AccountName: &(&types.S{S: "____"}).S,
			},
			Locations: []*model.StorageLocationRequest{},
		}
	}
}

func convertNodeToInstanceGroup(provider cloud.CloudProvider, node cloud.Node) *model.InstanceGroupsV2 {
	ig := &model.InstanceGroupsV2{
		Template:      provider.GenerateDefaultTemplate(),
		Group:         &node.Name,
		NodeCount:     &node.Count,
		Type:          node.GroupType,
		SecurityGroup: provider.GenerateDefaultSecurityGroup(node),
		Parameters:    provider.GetInstanceGroupParamatersTemplate(node),
		RecipeNames:   []string{},
	}
	return ig
}

func GenerateReinstallTemplate(c *cli.Context) {

	template := &model.ReinstallRequestV2{
		BlueprintName:  &(&types.S{S: c.String(fl.FlBlueprintName.Name)}).S,
		InstanceGroups: []*model.InstanceGroupsV2{},
	}

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	stackName := c.String(fl.FlName.Name)
	stackResp := fetchStack(workspaceID, stackName, stackClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlUsername.Name), c.String(fl.FlPassword.Name), c.String(fl.FlAuthTypeOptional.Name)))
	provider, ok := cloud.CloudProviders[cloud.CloudType(stackResp.CloudPlatform)]
	if !ok {
		utils.LogErrorMessageAndExit("Not supported CloudProvider: " + stackResp.CloudPlatform)
	}

	bpName := c.String(fl.FlBlueprintNameOptional.Name)
	workspace := c.Int64(fl.FlWorkspaceOptional.Name)
	bpResp := blueprint.FetchBlueprint(workspace, bpName, getBlueprintClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlUsername.Name), c.String(fl.FlPassword.Name), c.String(fl.FlAuthTypeOptional.Name)))
	bp, err := base64.StdEncoding.DecodeString(bpResp.AmbariBlueprint)
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	for _, node := range getNodesByBlueprint(bp) {
		ig := &model.InstanceGroupsV2{
			Group:         &(&types.S{S: node.Name}).S,
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
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
}
