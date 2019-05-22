package yarn

import (
	envmodel "github.com/hortonworks/cb-cli/dataplane/api-environment/model"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
)

func (p *YarnProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	return nil
}

func (p *YarnProvider) SetParametersTemplate(request *model.StackV4Request) {
	request.Yarn = &model.YarnStackV4Parameters{
		YarnQueue: "default-developers",
	}
}

func (p *YarnProvider) SetInstanceGroupParametersTemplate(request *model.InstanceGroupV4Request, node cloud.Node) {
}

func (p *YarnProvider) GenerateDefaultNetworkWithParams(getFlags func(string) string, mode cloud.NetworkMode) *envmodel.EnvironmentNetworkV1Request {
	return &envmodel.EnvironmentNetworkV1Request{}
}
