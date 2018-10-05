package cloud

import (
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/types"
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

type CloudStorageType int

const (
	WASB CloudStorageType = iota
	ADLS
	ADLS_GEN2
	S3
	GCS
	NO_CLOUD_STORAGE
)

type EncryptionType int

const (
	DEFAULT_ENCRYPTION EncryptionType = iota
	NONE_ENCRYPTION
	CUSTOM_ENCRYPTION
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
	GetCredentialParameters(func(string) string) (map[string]interface{}, error)
	GetNetworkParamatersTemplate(NetworkMode) map[string]interface{}
	GetInstanceGroupParamatersTemplate(node Node) map[string]interface{}
	GetParamatersTemplate() map[string]interface{}
	SkippedFields() map[string]bool
	GenerateDefaultTemplate() *model.TemplateV2Request
	GenerateDefaultNetwork(networkParameters map[string]interface{}, mode NetworkMode) *model.NetworkV2Request
	GenerateNetworkRequestFromNetworkResponse(response *model.NetworkResponse) *model.NetworkV2Request
	GenerateDefaultSecurityGroup(node Node) *model.SecurityGroupV2Request
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

func (p *DefaultCloudProvider) GetCredentialParameters(func(string) string) (map[string]interface{}, error) {
	return make(map[string]interface{}), nil
}

func (p *DefaultCloudProvider) GenerateDefaultTemplate() *model.TemplateV2Request {
	return &model.TemplateV2Request{
		InstanceType: "____",
		VolumeType:   "____",
		VolumeCount:  1,
		VolumeSize:   10,
	}
}

func (p *DefaultCloudProvider) GenerateDefaultNetwork(networkParameters map[string]interface{}, mode NetworkMode) *model.NetworkV2Request {
	network := &model.NetworkV2Request{
		Parameters: networkParameters,
	}
	if mode != EXISTING_NETWORK_EXISTING_SUBNET && mode != LEGACY_NETWORK {
		network.SubnetCIDR = "10.0.0.0/16"
	}
	return network
}

func (p *DefaultCloudProvider) GenerateDefaultSecurityGroup(node Node) *model.SecurityGroupV2Request {
	return &model.SecurityGroupV2Request{
		SecurityRules: getDefaultSecurityRules(node),
	}
}

func (p *DefaultCloudProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkResponse) *model.NetworkV2Request {
	return &model.NetworkV2Request{
		Parameters: response.Parameters,
	}
}

func getDefaultSecurityRules(node Node) []*model.SecurityRuleRequest {
	ruleGen := func(port string) *model.SecurityRuleRequest {
		return &model.SecurityRuleRequest{
			Subnet:   &(&types.S{S: "0.0.0.0/0"}).S,
			Protocol: &(&types.S{S: "tcp"}).S,
			Ports:    &port,
		}
	}
	rules := []*model.SecurityRuleRequest{
		ruleGen("22"),
	}
	if node.GroupType == model.InstanceGroupResponseTypeGATEWAY {
		rules = append(rules, ruleGen("443"))
		rules = append(rules, ruleGen("9443"))
	}
	return rules
}
