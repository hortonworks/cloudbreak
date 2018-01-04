package aws

import "github.com/hortonworks/cb-cli/cli/cloud"

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

func (p *AwsProvider) GetParamatersTemplate() map[string]string {
	return map[string]string{"instanceProfileStrategy": "CREATE"}
}

func (p *AwsProvider) GetInstanceGroupParamatersTemplate(node cloud.Node) map[string]interface{} {
	return nil
}
