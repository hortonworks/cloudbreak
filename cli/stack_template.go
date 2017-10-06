package cli

import (
	"encoding/json"
	"fmt"

	"github.com/hortonworks/hdc-cli/cli/cloud"
	"github.com/hortonworks/hdc-cli/cli/types"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

func GenerateAwsStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AWS)
	return generateStackTemplate(c, getNetworkMode(c))
}

func GenerateAzureStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AZURE)
	return generateStackTemplate(c, getNetworkMode(c))
}

func GenerateGcpStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.GCP)
	return generateStackTemplate(c, getNetworkMode(c))
}

func GenerateOpenstackStackTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.OPENSTACK)
	return generateStackTemplate(c, getNetworkMode(c))
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

func generateStackTemplate(c *cli.Context, mode cloud.NetworkMode) error {
	provider := cloud.GetProvider()

	template := models_cloudbreak.StackRequest{
		ClusterRequest: &models_cloudbreak.ClusterRequest{
			Name:          &(&types.S{S: "____"}).S,
			BlueprintName: "____",
			HostGroups: []*models_cloudbreak.HostGroupRequest{
				getDefaultHostGroup("master"),
				getDefaultHostGroup("slave"),
			},
			UserName: &(&types.S{S: "____"}).S,
			Password: &(&types.S{S: ""}).S,
			Gateway: &models_cloudbreak.GatewayJSON{
				EnableGateway: &(&types.B{B: false}).B,
				GatewayType:   "INDIVIDUAL",
			},
		},
		CloudPlatform:    *provider.GetName(),
		Name:             &(&types.S{S: "____"}).S,
		CredentialName:   "____",
		AvailabilityZone: "____",
		Region:           "____",
		Orchestrator:     &models_cloudbreak.OrchestratorRequest{Type: &(&types.S{S: "SALT"}).S},
		Parameters:       map[string]string{"instanceProfileStrategy": "CREATE"},
		InstanceGroups: []*models_cloudbreak.InstanceGroups{
			getDefaultInstanceGroup("master", models_cloudbreak.InstanceGroupResponseTypeGATEWAY),
			getDefaultInstanceGroup("slave", models_cloudbreak.InstanceGroupResponseTypeCORE),
		},
		Network: &models_cloudbreak.NetworkRequest{
			CloudPlatform: provider.GetName(),
			Parameters:    provider.GetNetworkParamatersTemplate(mode),
		},
		StackAuthentication: &models_cloudbreak.StackAuthentication{PublicKey: "____"},
	}

	if mode != cloud.EXISTING_NETWORK_EXISTING_SUBNET && mode != cloud.LEGACY_NETWORK {
		template.Network.SubnetCIDR = "10.0.0.0/16"
	}

	resp, err := json.MarshalIndent(template, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
	return nil
}

func getDefaultHostGroup(name string) *models_cloudbreak.HostGroupRequest {
	return &models_cloudbreak.HostGroupRequest{
		Name:         &name,
		RecoveryMode: "AUTO",
		Constraint: &models_cloudbreak.Constraint{
			InstanceGroupName: name,
			HostCount:         &(&types.I32{I: 1}).I,
		},
	}
}

func getDefaultInstanceGroup(name string, groupType string) *models_cloudbreak.InstanceGroups {
	provider := cloud.GetProvider()

	return &models_cloudbreak.InstanceGroups{
		Template: &models_cloudbreak.TemplateRequest{
			CloudPlatform: provider.GetName(),
			InstanceType:  &(&types.S{S: "____"}).S,
			VolumeCount:   1,
			VolumeSize:    10,
			Parameters:    map[string]interface{}{"sshLocation": "0.0.0.0/0", "encrypted": false},
		},
		Group:     &name,
		NodeCount: &(&types.I32{I: 1}).I,
		Type:      groupType,
		SecurityGroup: &models_cloudbreak.SecurityGroupRequest{
			CloudPlatform: provider.GetName(),
			SecurityRules: getDefaultSecurityRules(),
		},
	}
}

func getDefaultSecurityRules() []*models_cloudbreak.SecurityRuleRequest {
	return []*models_cloudbreak.SecurityRuleRequest{
		getDefaultSecurityRule("22"),
		getDefaultSecurityRule("443"),
		getDefaultSecurityRule("9443"),
	}
}

func getDefaultSecurityRule(port string) *models_cloudbreak.SecurityRuleRequest {
	return &models_cloudbreak.SecurityRuleRequest{
		Subnet:   &(&types.S{S: "0.0.0.0/0"}).S,
		Protocol: &(&types.S{S: "tcp"}).S,
		Ports:    &port,
	}
}
