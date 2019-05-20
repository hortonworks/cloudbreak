package cloud

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/types"
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
	SHARED_NETWORK
	NO_NETWORK
)

type CloudStorageType int

const (
	WASB CloudStorageType = iota
	ADLS_GEN1
	ADLS_GEN2
	S3
	GCS
	NO_CLOUD_STORAGE
)

type EncryptionType int

const (
	REGION_FIELD            = "region"
	AVAILABILITY_ZONE_FIELD = "availabilityZone"
	DEFAULT_SUBNET_CIDR     = "10.0.0.0/16"
)

var currentCloud CloudType

func SetProviderType(ct CloudType) {
	currentCloud = ct
}

var CloudProviders = make(map[CloudType]CloudProvider)

type CloudProvider interface {
	GetName() *string
	GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV4Request, error)
	SkippedFields() map[string]bool
	GenerateDefaultTemplate() *model.InstanceTemplateV4Request
	GenerateDefaultNetwork(mode NetworkMode) *model.NetworkV4Request
	GenerateNetworkRequestFromNetworkResponse(response *model.NetworkV4Response) *model.NetworkV4Request
	GenerateDefaultSecurityGroup(node Node) *model.SecurityGroupV4Request
	SetParametersTemplate(request *model.StackV4Request)
	SetInstanceGroupParametersTemplate(request *model.InstanceGroupV4Request, node Node)
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

func (p *DefaultCloudProvider) GenerateDefaultTemplate() *model.InstanceTemplateV4Request {
	return &model.InstanceTemplateV4Request{
		InstanceType: "____",
		AttachedVolumes: []*model.VolumeV4Request{
			{
				Type:  "____",
				Count: 1,
				Size:  &(&types.I32{I: 10}).I,
			},
		},
	}
}

func (p *DefaultCloudProvider) GenerateDefaultSecurityGroup(node Node) *model.SecurityGroupV4Request {
	return &model.SecurityGroupV4Request{
		SecurityRules: getDefaultSecurityRules(node),
	}
}

func getDefaultSecurityRules(node Node) []*model.SecurityRuleV4Request {
	ruleGen := func(port string) *model.SecurityRuleV4Request {
		return &model.SecurityRuleV4Request{
			Subnet:   &(&types.S{S: "0.0.0.0/0"}).S,
			Protocol: &(&types.S{S: "tcp"}).S,
			Ports:    []string{port},
		}
	}
	rules := []*model.SecurityRuleV4Request{
		ruleGen("22"),
	}
	if node.GroupType == model.InstanceGroupV4ResponseTypeGATEWAY {
		rules = append(rules, ruleGen("443"))
		rules = append(rules, ruleGen("9443"))
	}
	return rules
}

func CreateBaseCredentialRequest(stringFinder func(string) string) *model.CredentialV4Request {
	name := stringFinder(fl.FlName.Name)
	return &model.CredentialV4Request{
		CredentialV4Base: model.CredentialV4Base{
			Name:          &name,
			Description:   &(&types.S{S: stringFinder(fl.FlDescriptionOptional.Name)}).S,
			CloudPlatform: GetProvider().GetName(),
		},
	}
}
