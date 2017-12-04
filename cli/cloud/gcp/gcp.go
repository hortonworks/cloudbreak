package gcp

import "github.com/hortonworks/cb-cli/cli/cloud"

var name string

func init() {
	name = string(cloud.GCP)
	cloud.CloudProviders[cloud.GCP] = new(GcpProvider)
}

type GcpProvider struct {
	cloud.DefaultCloudProvider
}

func (p *GcpProvider) GetName() *string {
	return &name
}
