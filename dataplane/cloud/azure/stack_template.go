package azure

import (
	envmodel "github.com/hortonworks/cb-cli/dataplane/api-environment/model"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
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
				NoPublicIP:        false,
				NoFirewallRules:   false,
			},
		}
	default:
		return &model.NetworkV4Request{
			SubnetCIDR: cloud.DEFAULT_SUBNET_CIDR,
		}
	}
}

func (p *AzureProvider) GenerateDefaultNetworkWithParams(getFlags func(string) string, mode cloud.NetworkMode) *envmodel.EnvironmentNetworkV1Request {
	switch mode {
	case cloud.NEW_NETWORK_NEW_SUBNET:
		networkCidr := getFlags(fl.FlNetworkCidr.Name)
		return &envmodel.EnvironmentNetworkV1Request{
			NetworkCidr: &networkCidr,
		}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		networkId := getFlags(fl.FlNetworkId.Name)
		resourceGroup := getFlags(fl.FlResourceGroupName.Name)
		subnetIds := utils.DelimitedStringToArray(getFlags(fl.FlSubnetIds.Name), ",")
		return &envmodel.EnvironmentNetworkV1Request{
			Azure: &envmodel.EnvironmentNetworkAzureV1Params{
				NetworkID:         &networkId,
				ResourceGroupName: &resourceGroup,
				NoPublicIP:        &(&types.B{B: false}).B,
				NoFirewallRules:   &(&types.B{B: false}).B,
			},
			SubnetIds: subnetIds,
		}
	default:
		return &envmodel.EnvironmentNetworkV1Request{
			Azure: &envmodel.EnvironmentNetworkAzureV1Params{
				NetworkID:         &(&types.S{S: "____"}).S,
				ResourceGroupName: &(&types.S{S: "____"}).S,
				NoPublicIP:        &(&types.B{B: false}).B,
				NoFirewallRules:   &(&types.B{B: false}).B,
			},
			SubnetIds:   []string{"____"},
			NetworkCidr: &(&types.S{S: "____"}).S,
		}
	}
}

func (p *AzureProvider) SetParametersTemplate(request *model.StackV4Request) {
	request.Azure = &model.AzureStackV4Parameters{
		EncryptStorage: true,
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
