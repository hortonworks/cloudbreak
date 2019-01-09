package openstack

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
)

func (p *OpenstackProvider) GenerateDefaultNetwork(mode cloud.NetworkMode) *model.NetworkV4Request {
	switch mode {
	case cloud.NEW_NETWORK_NEW_SUBNET:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
			Openstack: &model.OpenStackNetworkV4Parameters{
				PublicNetID: "____",
			},
		}
	case cloud.EXISTING_NETWORK_NEW_SUBNET:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
			Openstack: &model.OpenStackNetworkV4Parameters{
				PublicNetID: "____",
				NetworkID:   "____",
				RouterID:    "____",
			},
		}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return &model.NetworkV4Request{
			Openstack: &model.OpenStackNetworkV4Parameters{
				PublicNetID:      "____",
				NetworkID:        "____",
				SubnetID:         "____",
				NetworkingOption: "____",
			},
		}
	default:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
		}
	}
}

func (p *OpenstackProvider) SetParametersTemplate(request *model.StackV4Request) {
}

func (p *OpenstackProvider) SetInstanceGroupParametersTemplate(request *model.InstanceGroupV4Request, node cloud.Node) {
}

func (p *OpenstackProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkV4Response) *model.NetworkV4Request {
	osParams := response.Openstack
	if osParams == nil {
		return &model.NetworkV4Request{}
	}

	request := &model.NetworkV4Request{
		Openstack: &model.OpenStackNetworkV4Parameters{
			PublicNetID:      osParams.PublicNetID,
			NetworkID:        osParams.NetworkID,
			SubnetID:         osParams.SubnetID,
			NetworkingOption: osParams.NetworkingOption,
		},
	}
	return request
}
