package aws

import (
	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

func (p *AwsProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	switch mode {
	case cloud.EXISTING_NETWORK_NEW_SUBNET:
		return map[string]interface{}{"vpcId": "____", "internetGatewayId": "____"}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return map[string]interface{}{"vpcId": "____", "subnetId": "____"}
	default:
		return nil
	}
}

func (p *AwsProvider) GetParamatersTemplate() map[string]interface{} {
	return map[string]interface{}{"instanceProfileStrategy": "CREATE"}
}

func (p *AwsProvider) GetInstanceGroupParamatersTemplate(node cloud.Node) map[string]interface{} {
	return nil
}

func (p *AwsProvider) GenerateNetworkRequestFromNetworkResponse(response *models_cloudbreak.NetworkResponse) *models_cloudbreak.NetworkV2Request {
	parameters := utils.CopyToByTargets(response.Parameters, "subnetId")
	parameters["vpcId"] = response.Parameters["networkId"]

	request := &models_cloudbreak.NetworkV2Request{
		Parameters: parameters,
	}
	return request
}
