package openstack

import "github.com/hortonworks/cb-cli/cli/cloud"

func (p *OpenstackProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	switch mode {
	case cloud.NEW_NETWORK_NEW_SUBNET:
		return map[string]interface{}{"publicNetId": ""}
	case cloud.EXISTING_NETWORK_NEW_SUBNET:
		return map[string]interface{}{"publicNetId": "", "networkId": "____", "routerId": "____"}
	case cloud.EXISTING_NETWORK_EXISTING_SUBNET:
		return map[string]interface{}{"publicNetId": "", "networkId": "____", "subnetId": "____", "networkingOption": ""}
	default:
		return nil
	}
}

func (p *OpenstackProvider) GetParamatersTemplate() map[string]string {
	return nil
}
