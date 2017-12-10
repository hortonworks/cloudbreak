package yarn

import "github.com/hortonworks/cb-cli/cli/cloud"

func (p *YarnProvider) GetNetworkParamatersTemplate(mode cloud.NetworkMode) map[string]interface{} {
	return nil
}

func (p *YarnProvider) GetParamatersTemplate() map[string]string {
	return map[string]string{"yarnQueue": "default-developers"}
}
