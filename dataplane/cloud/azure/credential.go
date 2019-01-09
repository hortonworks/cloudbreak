package azure

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

func (p *AzureProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV4Request, error) {
	parameters := &model.AzureCredentialV4Parameters{
		SubscriptionID: &(&types.S{S: stringFinder("subscription-id")}).S,
		TenantID:       &(&types.S{S: stringFinder("tenant-id")}).S,
		AppBased: &model.AppBased{
			AccessKey: &(&types.S{S: stringFinder("app-id")}).S,
			SecretKey: &(&types.S{S: stringFinder("app-password")}).S,
		},
	}
	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Azure = parameters
	return credReq, nil
}
