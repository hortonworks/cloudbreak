package yarn

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

func (p *YarnProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV4Request, error) {
	parameters := &model.YarnCredentialV4Parameters{
		Endpoint: &(&types.S{S: stringFinder("yarn-endpoint")}).S,
	}
	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Yarn = parameters
	return credReq, nil
}
