package yarn

import (
	"github.com/hortonworks/cb-cli/dataplane/api-environment/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

func (p *YarnProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV1Request, error) {
	parameters := &model.YarnV1Parameters{
		Endpoint: &(&types.S{S: stringFinder("endpoint")}).S,
	}
	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Yarn = parameters
	return credReq, nil
}
