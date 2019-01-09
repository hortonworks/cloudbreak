package gcp

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

func (p *GcpProvider) GenerateDefaultNetwork(mode cloud.NetworkMode) *model.NetworkV4Request {
	switch mode {
	case cloud.EXISTING_NETWORK_NEW_SUBNET:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
			Gcp: &model.GcpNetworkV4Parameters{
				NetworkID: "____",
			},
		}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return &model.NetworkV4Request{
			Gcp: &model.GcpNetworkV4Parameters{
				NetworkID:       "____",
				SubnetID:        "____",
				NoPublicIP:      &(&types.B{B: false}).B,
				NoFirewallRules: &(&types.B{B: false}).B,
			},
		}
	case cloud.LEGACY_NETWORK:
		return &model.NetworkV4Request{
			Gcp: &model.GcpNetworkV4Parameters{
				NetworkID: "____",
			},
		}
	case cloud.SHARED_NETWORK:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
			Gcp: &model.GcpNetworkV4Parameters{
				NetworkID:       "____",
				SubnetID:        "____",
				NoFirewallRules: &(&types.B{B: true}).B,
				SharedProjectID: "____",
			},
		}
	default:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
		}
	}
}

func (p *GcpProvider) SetParametersTemplate(request *model.StackV4Request) {
}

func (p *GcpProvider) SetInstanceGroupParametersTemplate(request *model.InstanceGroupV4Request, node cloud.Node) {
}

func (p *GcpProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkV4Response) *model.NetworkV4Request {
	gcpParams := response.Gcp
	if gcpParams == nil {
		return &model.NetworkV4Request{}
	}

	request := &model.NetworkV4Request{
		Gcp: &model.GcpNetworkV4Parameters{
			NetworkID:       gcpParams.NetworkID,
			SubnetID:        gcpParams.SubnetID,
			NoPublicIP:      gcpParams.NoPublicIP,
			NoFirewallRules: gcpParams.NoFirewallRules,
		},
	}
	return request
}
