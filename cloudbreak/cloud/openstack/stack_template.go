package openstack

import (
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/cloud"
	"github.com/hortonworks/cb-cli/utils"
)

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

func (p *OpenstackProvider) GetParamatersTemplate() map[string]interface{} {
	return nil
}

func (p *OpenstackProvider) GetInstanceGroupParamatersTemplate(node cloud.Node) map[string]interface{} {
	return nil
}

func (p *OpenstackProvider) GenerateNetworkRequestFromNetworkResponse(response *model.NetworkResponse) *model.NetworkV2Request {
	request := &model.NetworkV2Request{
		Parameters: utils.CopyToByTargets(response.Parameters, "publicNetId", "networkId", "subnetId", "networkingOption"),
	}
	return request
}
