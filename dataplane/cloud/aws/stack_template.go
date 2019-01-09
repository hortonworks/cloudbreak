package aws

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
)

func (p *AwsProvider) GenerateDefaultNetwork(mode cloud.NetworkMode) *model.NetworkV4Request {
	switch mode {
	case cloud.EXISTING_NETWORK_NEW_SUBNET:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
			Aws: &model.AwsNetworkV4Parameters{
				VpcID:             "____",
				InternetGatewayID: "____",
			},
		}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return &model.NetworkV4Request{
			Aws: &model.AwsNetworkV4Parameters{
				VpcID:    "____",
				SubnetID: "____",
			},
		}
	default:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
		}
	}
}

func (p *AwsProvider) SetParametersTemplate(request *model.StackV4Request) {
}

func (p *AwsProvider) SetInstanceGroupParametersTemplate(request *model.InstanceGroupV4Request, node cloud.Node) {
}

func (p *AwsProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkV4Response) *model.NetworkV4Request {
	if response.Aws == nil {
		return &model.NetworkV4Request{}
	}

	request := &model.NetworkV4Request{
		Aws: &model.AwsNetworkV4Parameters{
			VpcID:    response.Aws.VpcID,
			SubnetID: response.Aws.SubnetID,
		},
	}
	return request
}
