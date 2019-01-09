package yarn

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
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

func (p *YarnProvider) GenerateDefaultTemplate() *model.InstanceTemplateV4Request {
	return &model.InstanceTemplateV4Request{
		Yarn: &model.YarnInstanceTemplateV4Parameters{
			Cpus:   4,
			Memory: 8192,
		},
		InstanceType: "",
		AttachedVolumes: []*model.VolumeV4Request{
			{
				Type:  "",
				Count: 0,
				Size:  &(&types.I32{I: 10}).I,
			},
		},
	}
}

func (p *YarnProvider) GenerateDefaultNetwork(mode cloud.NetworkMode) *model.NetworkV4Request {
	return nil
}

func (p *YarnProvider) GenerateDefaultSecurityGroup(node cloud.Node) *model.SecurityGroupV4Request {
	return nil
}

func (p *YarnProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkV4Response) *model.NetworkV4Request {
	return nil
}
