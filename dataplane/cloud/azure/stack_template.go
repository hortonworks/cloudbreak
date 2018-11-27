package azure

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/dp-cli-common/utils"
)

func (p *AzureProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	switch mode {
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return map[string]interface{}{"resourceGroupName": "____", "networkId": "____", "subnetId": "____", "noPublicIp": false, "noFirewallRules": false}
	default:
		return nil
	}
}

func (p *AzureProvider) GetParamatersTemplate() map[string]interface{} {
	return map[string]interface{}{"encryptStorage": "true"}
}

func (p *AzureProvider) GetInstanceGroupParamatersTemplate(node cloud.Node) map[string]interface{} {
	if node.Count > 1 {
		return map[string]interface{}{
			"availabilitySet": map[string]interface{}{
				"faultDomainCount":  2,
				"updateDomainCount": 20,
				"name":              node.Name + "-" + utils.RandStr(20) + "-as",
			},
		}
	}
	return nil
}

func (p *AzureProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkResponse) *model.NetworkV2Request {
	request := &model.NetworkV2Request{
		Parameters: utils.CopyToByTargets(response.Parameters, "resourceGroupName", "networkId", "subnetId", "noPublicIp", "noFirewallRules"),
	}
	return request
}
