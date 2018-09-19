package azure

import "github.com/hortonworks/cb-cli/cloudbreak/cloud"

var name string

func init() {
	name = string(cloud.AZURE)
	cloud.CloudProviders[cloud.AZURE] = new(AzureProvider)
}

type AzureProvider struct {
	cloud.DefaultCloudProvider
}

func (p *AzureProvider) GetName() *string {
	return &name
}
