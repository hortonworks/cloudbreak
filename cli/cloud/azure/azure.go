package azure

import "github.com/hortonworks/cb-cli/cli/cloud"

var name string

func init() {
	name = string(cloud.AZURE)
	cloud.CloudProviders[cloud.AZURE] = new(AzureProvider)
}

type AzureProvider struct {
}

func (p *AzureProvider) GetName() *string {
	return &name
}
