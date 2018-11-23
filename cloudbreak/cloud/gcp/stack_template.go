package gcp

import (
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/cloud"
	"github.com/hortonworks/cb-cli/dp-cli-common/utils"
)

func (p *GcpProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	switch mode {
	case cloud.EXISTING_NETWORK_NEW_SUBNET:
		return map[string]interface{}{"networkId": "____"}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return map[string]interface{}{"networkId": "____", "subnetId": "____", "noPublicIp": false, "noFirewallRules": false}
	case cloud.LEGACY_NETWORK:
		return map[string]interface{}{"networkId": ""}
	case cloud.SHARED_NETWORK:
		return map[string]interface{}{"sharedProjectId": "____", "networkId": "____", "subnetId": "____", "noFirewallRules": true}
	default:
		return nil
	}
}

func (p *GcpProvider) GetParamatersTemplate() map[string]interface{} {
	return nil
}

func (p *GcpProvider) GetInstanceGroupParamatersTemplate(node cloud.Node) map[string]interface{} {
	return nil
}

func (p *GcpProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkResponse) *model.NetworkV2Request {
	var parameters = make(map[string]interface{})
	parameters["networkId"] = response.Parameters["networkId"]
	parameters["subnetId"] = response.Parameters["subnetId"]
	parameters["noPublicIp"] = false
	parameters["noFirewallRules"] = false

	request := &model.NetworkV2Request{
		Parameters: utils.CopyToByTargets(response.Parameters, "networkId", "subnetId", "noPublicIp", "noFirewallRules"),
	}
	return request
}
