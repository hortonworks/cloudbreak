package azure

import "github.com/hortonworks/cb-cli/cli/cloud"

func (p *AzureProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	switch mode {
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return map[string]interface{}{"resourceGroupName": "____", "networkId": "____", "subnetId": "____", "noPublicIp": false, "noFirewallRules": false}
	default:
		return nil
	}
}
