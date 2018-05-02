package cli

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var defaultNodes = []cloud.Node{
	{Name: "master", GroupType: models_cloudbreak.InstanceGroupResponseTypeGATEWAY, Count: 1},
	{Name: "worker", GroupType: models_cloudbreak.InstanceGroupResponseTypeCORE, Count: 3},
	{Name: "compute", GroupType: models_cloudbreak.InstanceGroupResponseTypeCORE, Count: 0},
}

var maxCardinality = map[string]int{
	"1":   1,
	"0-1": 1,
	"1-2": 2,
	"0+":  3,
	"1+":  3,
	"ALL": 3,
}

var getBlueprintClient = func(server, userName, password, authType string) getPublicBlueprint {
	cbClient := NewCloudbreakHTTPClient(server, userName, password, authType)
	return cbClient.Cloudbreak.V1blueprints
}

var stackClient = func(server, userName, password, authType string) getPublicStack {
	cbClient := NewCloudbreakHTTPClient(server, userName, password, authType)
	return cbClient.Cloudbreak.V1stacks
}

func GenerateAwsStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AWS)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, getBlueprintClient)
}

func GenerateAzureStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AZURE)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, getBlueprintClient)
}

func GenerateGcpStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.GCP)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, getBlueprintClient)
}

func GenerateOpenstackStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.OPENSTACK)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, getBlueprintClient)
}

func GenerateYarnStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.YARN)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, c.Bool, getBlueprintClient)
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

func generateStackTemplateImpl(mode cloud.NetworkMode, stringFinder func(string) string, boolFinder func(string) bool, getBlueprintClient func(string, string, string, string) getPublicBlueprint) error {
	provider := cloud.GetProvider()
	skippedFields := provider.SkippedFields()

	template := models_cloudbreak.StackV2Request{
		Cluster: &models_cloudbreak.ClusterV2Request{
			Ambari: &models_cloudbreak.AmbariV2Request{
				BlueprintName:   "____",
				BlueprintInputs: []*models_cloudbreak.BlueprintInput{},
				UserName:        &(&types.S{S: "____"}).S,
				Password:        &(&types.S{S: ""}).S,
			},
		},
		General: &models_cloudbreak.GeneralSettings{
			Name:           &(&types.S{S: ""}).S,
			CredentialName: &(&types.S{S: "____"}).S,
		},
		Placement: &models_cloudbreak.PlacementSettings{
			Region:           getStringPointer(skippedFields, cloud.REGION_FIELD, "____"),
			AvailabilityZone: getString(skippedFields, cloud.AVAILABILITY_ZONE_FIELD, "____"),
		},
		InstanceGroups:      []*models_cloudbreak.InstanceGroupsV2{},
		Network:             provider.GenerateDefaultNetwork(provider.GetNetworkParamatersTemplate(mode), mode),
		StackAuthentication: &models_cloudbreak.StackAuthentication{PublicKey: "____"},
	}

	extendTemplateWithOptionalBlocks(&template, boolFinder)

	nodes := defaultNodes
	if bpName := stringFinder(FlBlueprintNameOptional.Name); len(bpName) != 0 {
		bpResp := fetchBlueprint(bpName, getBlueprintClient(stringFinder(FlServerOptional.Name), stringFinder(FlUsername.Name), stringFinder(FlPassword.Name), stringFinder(FlAuthTypeOptional.Name)))
		bp, err := base64.StdEncoding.DecodeString(bpResp.AmbariBlueprint)
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		nodes = getNodesByBlueprint(bp)
		template.Cluster.Ambari.BlueprintName = bpName
	} else if bpFile := stringFinder(FlBlueprintFileOptional.Name); len(bpFile) != 0 {
		bp := utils.ReadFile(bpFile)
		nodes = getNodesByBlueprint(bp)
	}
	for _, node := range nodes {
		template.InstanceGroups = append(template.InstanceGroups, convertNodeToInstanceGroup(provider, node))
	}

	if params := provider.GetParamatersTemplate(); params != nil {
		template.Parameters = params
	}

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
	nodes := []*cloud.Node{}
	if bpJson["host_groups"] == nil {
		utils.LogErrorMessageAndExit("host_groups not found in blueprint")
	}
	var gateway *cloud.Node
	for _, e := range bpJson["host_groups"].([]interface{}) {
		var hg map[string]interface{} = e.(map[string]interface{})
		var count int = 1
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
		node := cloud.Node{Name: hg["name"].(string), GroupType: models_cloudbreak.InstanceGroupResponseTypeCORE, Count: int32(count)}
		nodes = append(nodes, &node)
		if gateway == nil || gateway.Count > node.Count {
			gateway = &node
		}
	}
	gateway.GroupType = models_cloudbreak.InstanceGroupResponseTypeGATEWAY
	resp := []cloud.Node{}
	for _, n := range nodes {
		resp = append(resp, *n)
	}
	return resp
}

func extendTemplateWithOptionalBlocks(template *models_cloudbreak.StackV2Request, boolFinder func(string) bool) {
	if withCustomDomain := boolFinder(FlWithCustomDomainOptional.Name); withCustomDomain {
		template.CustomDomain = &models_cloudbreak.CustomDomainSettings{
			CustomDomain:            "____",
			CustomHostname:          "____",
			ClusterNameAsSubdomain:  &(&types.B{B: false}).B,
			HostgroupNameAsHostname: &(&types.B{B: false}).B,
		}
	}
	if withTags := boolFinder(FlWithTagsOptional.Name); withTags {
		template.Tags = &models_cloudbreak.Tags{
			UserDefinedTags: map[string]string{
				"____": "____",
			},
		}
	}
	if withImage := boolFinder(FlWithImageOptional.Name); withImage {
		template.ImageSettings = &models_cloudbreak.ImageSettings{
			ImageCatalog: "____",
			ImageID:      "____",
		}
	}
	if withKerberosManaged := boolFinder(FlWithKerberosManagedOptional.Name); withKerberosManaged {
		template.Cluster.Ambari.Kerberos = &models_cloudbreak.KerberosRequest{
			TCPAllowed: &(&types.B{B: false}).B,
			MasterKey:  "____",
			Admin:      "____",
			Password:   "____",
		}
	}
	if withKerberosMIT := boolFinder(FlWithKerberosExistingMITOptional.Name); withKerberosMIT {
		template.Cluster.Ambari.Kerberos = &models_cloudbreak.KerberosRequest{
			TCPAllowed: &(&types.B{B: false}).B,
			Principal:  "____",
			Password:   "____",
			URL:        "____",
			AdminURL:   "____",
			Realm:      "____",
		}
	}
	if withKerberosAD := boolFinder(FlWithKerberosExistingADOptional.Name); withKerberosAD {
		template.Cluster.Ambari.Kerberos = &models_cloudbreak.KerberosRequest{
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
	if withKerberosCustom := boolFinder(FlWithKerberosCustomOptional.Name); withKerberosCustom {
		template.Cluster.Ambari.Kerberos = &models_cloudbreak.KerberosRequest{
			TCPAllowed: &(&types.B{B: false}).B,
			Principal:  "____",
			Password:   "____",
			Descriptor: "____",
			Krb5Conf:   "____",
		}
	}
}

func convertNodeToHostGroup(node cloud.Node) *models_cloudbreak.HostGroupRequest {
	return &models_cloudbreak.HostGroupRequest{
		Name:         &node.Name,
		RecoveryMode: "AUTO",
		Constraint: &models_cloudbreak.Constraint{
			InstanceGroupName: node.Name,
			HostCount:         &node.Count,
		},
	}
}

func convertNodeToInstanceGroup(provider cloud.CloudProvider, node cloud.Node) *models_cloudbreak.InstanceGroupsV2 {
	ig := &models_cloudbreak.InstanceGroupsV2{
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
	checkRequiredFlagsAndArguments(c)

	template := &models_cloudbreak.ReinstallRequestV2{
		BlueprintName:  &(&types.S{S: c.String(FlBlueprintName.Name)}).S,
		InstanceGroups: []*models_cloudbreak.InstanceGroupsV2{},
	}

	stackName := c.String(FlName.Name)
	stackResp := fetchStack(stackName, stackClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name), c.String(FlAuthTypeOptional.Name)))
	provider, ok := cloud.CloudProviders[cloud.CloudType(stackResp.CloudPlatform)]
	if !ok {
		utils.LogErrorMessageAndExit("Not supported CloudProvider: " + stackResp.CloudPlatform)
	}

	bpName := c.String(FlBlueprintNameOptional.Name)
	bpResp := fetchBlueprint(bpName, getBlueprintClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name), c.String(FlAuthTypeOptional.Name)))
	bp, err := base64.StdEncoding.DecodeString(bpResp.AmbariBlueprint)
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	for _, node := range getNodesByBlueprint(bp) {
		ig := &models_cloudbreak.InstanceGroupsV2{
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
