package aws

import "github.com/hortonworks/cb-cli/cli/cloud"

var name string

func init() {
	name = string(cloud.AWS)
	cloud.CloudProviders[cloud.AWS] = new(AwsProvider)
}

type AwsProvider struct {
	cloud.DefaultCloudProvider
}

func (p *AwsProvider) GetName() *string {
	return &name
}
