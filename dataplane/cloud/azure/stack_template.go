package azure

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
)

func (p *AzureProvider) GenerateDefaultNetwork(mode cloud.NetworkMode) *model.NetworkV4Request {
	switch mode {
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return &model.NetworkV4Request{
			Azure: &model.AzureNetworkV4Parameters{
				ResourceGroupName: "____",
				NetworkID:         "____",
				SubnetID:          "____",
				NoPublicIP:        &(&types.B{B: false}).B,
				NoFirewallRules:   &(&types.B{B: false}).B,
			},
		}
	default:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
		}
	}
}

func (p *AzureProvider) SetParametersTemplate(request *model.StackV4Request) {
	request.Azure = &model.AzureStackV4Parameters{
		EncryptStorage: &(&types.B{B: true}).B,
	}
}

func (p *AzureProvider) SetInstanceGroupParametersTemplate(request *model.InstanceGroupV4Request, node cloud.Node) {
	if node.Count > 1 {
		request.Azure = &model.AzureInstanceGroupV4Parameters{
			AvailabilitySet: &model.AzureAvailabiltySetV4{
				Name:              node.Name + "-" + utils.RandStr(20) + "-as",
				FaultDomainCount:  2,
				UpdateDomainCount: 20,
			},
		}
	}
}

func (p *AzureProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkV4Response) *model.NetworkV4Request {
	azureParams := response.Azure
	if azureParams == nil {
		return &model.NetworkV4Request{}
	}

	request := &model.NetworkV4Request{
		Azure: &model.AzureNetworkV4Parameters{
			ResourceGroupName: azureParams.ResourceGroupName,
			NetworkID:         azureParams.NetworkID,
			SubnetID:          azureParams.SubnetID,
			NoPublicIP:        azureParams.NoPublicIP,
			NoFirewallRules:   azureParams.NoFirewallRules,
		},
	}
	return request
}
