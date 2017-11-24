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

type node struct {
	name      string
	groupType string
	count     int32
}

var defaultNodes = []node{
	{"master", models_cloudbreak.InstanceGroupResponseTypeGATEWAY, 1},
	{"worker", models_cloudbreak.InstanceGroupResponseTypeCORE, 3},
	{"compute", models_cloudbreak.InstanceGroupResponseTypeCORE, 0},
}

var maxCardinality = map[string]int{
	"1":   1,
	"0-1": 1,
	"1-2": 2,
	"0+":  3,
	"1+":  3,
	"ALL": 3,
}

var getBlueprintClient = func(server, userName, password string) getPublicBlueprint {
	cbClient := NewCloudbreakOAuth2HTTPClient(server, userName, password)
	return cbClient.Cloudbreak.V1blueprints
}

func GenerateAwsStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AWS)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, getBlueprintClient)
}

func GenerateAzureStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AZURE)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, getBlueprintClient)
}

func GenerateGcpStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.GCP)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, getBlueprintClient)
}

func GenerateOpenstackStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.OPENSTACK)
	return generateStackTemplateImpl(getNetworkMode(c), c.String, getBlueprintClient)
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
		utils.LogErrorMessage("network mode not found")
		panic(2)
	}
}

func generateStackTemplateImpl(mode cloud.NetworkMode, stringFinder func(string) string, getBlueprintClient func(string, string, string) getPublicBlueprint) error {
	provider := cloud.GetProvider()

	template := models_cloudbreak.StackV2Request{
		ClusterRequest: &models_cloudbreak.ClusterV2Request{
			AmbariRequest: &models_cloudbreak.AmbariV2Request{
				BlueprintName: "____",
				UserName:      &(&types.S{S: "____"}).S,
				Password:      &(&types.S{S: ""}).S,
			},
		},
		Name:             &(&types.S{S: ""}).S,
		CredentialName:   "____",
		AvailabilityZone: "____",
		Region:           "____",
		Orchestrator:     &models_cloudbreak.OrchestratorRequest{Type: &(&types.S{S: "SALT"}).S},
		InstanceGroups:   []*models_cloudbreak.InstanceGroupsV2{},
		Network: &models_cloudbreak.NetworkV2Request{
			Parameters: provider.GetNetworkParamatersTemplate(mode),
		},
		StackAuthentication: &models_cloudbreak.StackAuthentication{PublicKey: "____"},
	}

	nodes := defaultNodes
	if bpName := stringFinder(FlBlueprintNameOptional.Name); len(bpName) != 0 {
		bpResp := fetchBlueprint(bpName, getBlueprintClient(stringFinder(FlServerOptional.Name), stringFinder(FlUsername.Name), stringFinder(FlPassword.Name)))
		bp, err := base64.StdEncoding.DecodeString(bpResp.AmbariBlueprint)
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		nodes = getNodesByBlueprint(bp)
	} else if bpFile := stringFinder(FlBlueprintFileOptional.Name); len(bpFile) != 0 {
		bp := utils.ReadFile(bpFile)
		nodes = getNodesByBlueprint(bp)
	}
	for _, node := range nodes {
		template.InstanceGroups = append(template.InstanceGroups, convertNodeToInstanceGroup(node))
	}

	if mode != cloud.EXISTING_NETWORK_EXISTING_SUBNET && mode != cloud.LEGACY_NETWORK {
		template.Network.SubnetCIDR = "10.0.0.0/16"
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

func getNodesByBlueprint(bp []byte) []node {
	var bpJson map[string]interface{}
	json.Unmarshal(bp, &bpJson)
	nodes := []*node{}
	if bpJson["host_groups"] == nil {
		utils.LogErrorMessageAndExit("host_groups not found in blueprint")
	}
	var gateway *node
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
		node := node{hg["name"].(string), models_cloudbreak.InstanceGroupResponseTypeCORE, int32(count)}
		nodes = append(nodes, &node)
		if gateway == nil || gateway.count > node.count {
			gateway = &node
		}
	}
	gateway.groupType = models_cloudbreak.InstanceGroupResponseTypeGATEWAY
	resp := []node{}
	for _, n := range nodes {
		resp = append(resp, *n)
	}
	return resp
}

func convertNodeToHostGroup(node node) *models_cloudbreak.HostGroupRequest {
	return &models_cloudbreak.HostGroupRequest{
		Name:         &node.name,
		RecoveryMode: "AUTO",
		Constraint: &models_cloudbreak.Constraint{
			InstanceGroupName: node.name,
			HostCount:         &node.count,
		},
	}
}

func convertNodeToInstanceGroup(node node) *models_cloudbreak.InstanceGroupsV2 {
	ig := &models_cloudbreak.InstanceGroupsV2{
		Template:  getDefaultTemplate(),
		Group:     &node.name,
		NodeCount: &node.count,
		Type:      node.groupType,
		SecurityGroup: &models_cloudbreak.SecurityGroupV2Request{
			SecurityRules: getDefaultSecurityRules(node),
		},
	}
	return ig
}

func getDefaultTemplate() *models_cloudbreak.TemplateV2Request {
	return &models_cloudbreak.TemplateV2Request{
		InstanceType: &(&types.S{S: "____"}).S,
		VolumeType:   "____",
		VolumeCount:  1,
		VolumeSize:   10,
	}
}

func getDefaultSecurityRules(node node) []*models_cloudbreak.SecurityRuleRequest {
	ruleGen := func(port string) *models_cloudbreak.SecurityRuleRequest {
		return &models_cloudbreak.SecurityRuleRequest{
			Subnet:   &(&types.S{S: "0.0.0.0/0"}).S,
			Protocol: &(&types.S{S: "tcp"}).S,
			Ports:    &port,
		}
	}
	rules := []*models_cloudbreak.SecurityRuleRequest{
		ruleGen("22"),
	}
	if node.groupType == models_cloudbreak.InstanceGroupResponseTypeGATEWAY {
		rules = append(rules, ruleGen("443"))
		rules = append(rules, ruleGen("9443"))
	}
	return rules
}

func GenerateReinstallTemplate(c *cli.Context) {
	checkRequiredFlags(c)

	template := &models_cloudbreak.ReinstallRequestV2{
		BlueprintName:  &(&types.S{S: c.String(FlBlueprintName.Name)}).S,
		InstanceGroups: []*models_cloudbreak.InstanceGroupsV2{},
	}

	bpName := c.String(FlBlueprintNameOptional.Name)
	bpResp := fetchBlueprint(bpName, getBlueprintClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name)))
	bp, err := base64.StdEncoding.DecodeString(bpResp.AmbariBlueprint)
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	for _, node := range getNodesByBlueprint(bp) {
		ig := &models_cloudbreak.InstanceGroupsV2{
			Group:        &(&types.S{S: node.name}).S,
			NodeCount:    &(&types.I32{I: node.count}).I,
			RecoveryMode: "AUTO",
			SecurityGroup: &models_cloudbreak.SecurityGroupV2Request{
				SecurityRules: getDefaultSecurityRules(node),
			},
			Template: getDefaultTemplate(),
			Type:     node.groupType,
		}
		template.InstanceGroups = append(template.InstanceGroups, ig)
	}

	resp, err := json.MarshalIndent(template, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
}
