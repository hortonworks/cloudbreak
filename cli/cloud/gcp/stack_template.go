package gcp

import (
	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

func (p *GcpProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	switch mode {
	case cloud.EXISTING_NETWORK_NEW_SUBNET:
		return map[string]interface{}{"networkId": "____"}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return map[string]interface{}{"networkId": "____", "subnetId": "____", "noPublicIp": false, "noFirewallRules": false}
	case cloud.LEGACY_NETWORK:
		return map[string]interface{}{"networkId": "____"}
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

func (p *GcpProvider) GenerateNetworkRequestFromNetworkResponse(response *models_cloudbreak.NetworkResponse) *models_cloudbreak.NetworkV2Request {
	var parameters = make(map[string]interface{})
	parameters["networkId"] = response.Parameters["networkId"]
	parameters["subnetId"] = response.Parameters["subnetId"]
	parameters["noPublicIp"] = false
	parameters["noFirewallRules"] = false

	request := &models_cloudbreak.NetworkV2Request{
		Parameters: utils.CopyToByTargets(response.Parameters, "networkId", "subnetId", "noPublicIp", "noFirewallRules"),
	}
	return request
}
