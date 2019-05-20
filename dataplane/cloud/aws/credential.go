package aws

import (
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

func (p *AwsProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV4Request, error) {
	var parameters *model.AwsCredentialV4Parameters
	if len(stringFinder("role-arn")) == 0 {
		parameters = &model.AwsCredentialV4Parameters{
			KeyBased: &model.KeyBasedCredentialParameters{
				AccessKey: &(&types.S{S: stringFinder("access-key")}).S,
				SecretKey: &(&types.S{S: stringFinder("secret-key")}).S,
			},
			GovCloud: &govCloud,
		}
	} else {
		parameters = &model.AwsCredentialV4Parameters{
			RoleBased: &model.RoleBasedCredentialParameters{
				RoleArn: &(&types.S{S: stringFinder("role-arn")}).S,
			},
			GovCloud: &govCloud,
		}
	}
	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Aws = parameters
	return credReq, nil
}
