package aws

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/dp-cli-common/utils"
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
	return nil
}

func (p *AwsProvider) GetInstanceGroupParamatersTemplate(node cloud.Node) map[string]interface{} {
	return nil
}

func (p *AwsProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkResponse) *model.NetworkV2Request {
	parameters := utils.CopyToByTargets(response.Parameters, "subnetId")
	parameters["vpcId"] = response.Parameters["networkId"]

	request := &model.NetworkV2Request{
		Parameters: parameters,
	}
	return request
}
