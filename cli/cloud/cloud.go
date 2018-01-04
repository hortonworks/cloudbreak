package cloud

import (
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type CloudType string

const (
	AWS       = CloudType("AWS")
	AZURE     = CloudType("AZURE")
	GCP       = CloudType("GCP")
	OPENSTACK = CloudType("OPENSTACK")
	YARN      = CloudType("YARN")
)

type NetworkMode int

const (
	NEW_NETWORK_NEW_SUBNET NetworkMode = iota
	EXISTING_NETWORK_NEW_SUBNET
	EXISTING_NETWORK_EXISTING_SUBNET
	LEGACY_NETWORK
	NO_NETWORK
)

const (
	REGION_FIELD            = "region"
	AVAILABILITY_ZONE_FIELD = "availabilityZone"
)

var currentCloud CloudType

func SetProviderType(ct CloudType) {
	currentCloud = ct
}

var CloudProviders = make(map[CloudType]CloudProvider)

type CloudProvider interface {
	GetName() *string
	GetCredentialParameters(func(string) string, func(string) bool) (map[string]interface{}, error)
	GetNetworkParamatersTemplate(NetworkMode) map[string]interface{}
	GetInstanceGroupParamatersTemplate(node Node) map[string]interface{}
	GetParamatersTemplate() map[string]string
	SkippedFields() map[string]bool
	GenerateDefaultTemplate() *models_cloudbreak.TemplateV2Request
	GenerateDefaultNetwork(networkParameters map[string]interface{}, mode NetworkMode) *models_cloudbreak.NetworkV2Request
	GenerateDefaultSecurityGroup(node Node) *models_cloudbreak.SecurityGroupV2Request
}

func GetProvider() CloudProvider {
	return CloudProviders[currentCloud]
}

type Node struct {
	Name      string
	GroupType string
	Count     int32
}

type DefaultCloudProvider struct {
}

func (p *DefaultCloudProvider) SkippedFields() map[string]bool {
	return make(map[string]bool)
}

func (p *DefaultCloudProvider) GetCredentialParameters(func(string) string, func(string) bool) (map[string]interface{}, error) {
	return make(map[string]interface{}), nil
}

func (p *DefaultCloudProvider) GenerateDefaultTemplate() *models_cloudbreak.TemplateV2Request {
	return &models_cloudbreak.TemplateV2Request{
		InstanceType: "____",
		VolumeType:   "____",
		VolumeCount:  1,
		VolumeSize:   10,
	}
}

func (p *DefaultCloudProvider) GenerateDefaultNetwork(networkParameters map[string]interface{}, mode NetworkMode) *models_cloudbreak.NetworkV2Request {
	network := &models_cloudbreak.NetworkV2Request{
		Parameters: networkParameters,
	}
	if mode != EXISTING_NETWORK_EXISTING_SUBNET && mode != LEGACY_NETWORK {
		network.SubnetCIDR = "10.0.0.0/16"
	}
	return network
}

func (p *DefaultCloudProvider) GenerateDefaultSecurityGroup(node Node) *models_cloudbreak.SecurityGroupV2Request {
	return &models_cloudbreak.SecurityGroupV2Request{
		SecurityRules: getDefaultSecurityRules(node),
	}
}

func getDefaultSecurityRules(node Node) []*models_cloudbreak.SecurityRuleRequest {
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
	if node.GroupType == models_cloudbreak.InstanceGroupResponseTypeGATEWAY {
		rules = append(rules, ruleGen("443"))
		rules = append(rules, ruleGen("9443"))
	}
	return rules
}
