package yarn

import (
	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

var name string

var skippedFields = map[string]bool{
	cloud.AVAILABILITY_ZONE_FIELD: true,
	cloud.REGION_FIELD:            true,
}

func init() {
	name = string(cloud.YARN)
	cloud.CloudProviders[cloud.YARN] = new(YarnProvider)
}

type YarnProvider struct {
	cloud.DefaultCloudProvider
}

func (p *YarnProvider) GetName() *string {
	return &name
}

func (p *YarnProvider) SkippedFields() map[string]bool {
	return skippedFields
}

func (p *YarnProvider) GenerateDefaultTemplate() *models_cloudbreak.TemplateV2Request {
	return &models_cloudbreak.TemplateV2Request{
		CustomInstanceType: &models_cloudbreak.CustomInstanceType{
			Cpus:   4,
			Memory: 8192,
		},
		InstanceType: "",
		VolumeType:   "",
		VolumeCount:  0,
		VolumeSize:   0,
	}
}

func (p *YarnProvider) GenerateDefaultNetwork(networkParameters map[string]interface{}, mode cloud.NetworkMode) *models_cloudbreak.NetworkV2Request {
	return nil
}

func (p *YarnProvider) GenerateDefaultSecurityGroup(node cloud.Node) *models_cloudbreak.SecurityGroupV2Request {
	return nil
}
