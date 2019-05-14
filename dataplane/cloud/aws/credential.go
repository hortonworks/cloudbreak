package aws

import (
	"github.com/hortonworks/cb-cli/dataplane/api-environment/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

func (p *AwsProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV1Request, error) {
	var parameters *model.AwsCredentialV1Parameters
	if len(stringFinder("role-arn")) == 0 {
		parameters = &model.AwsCredentialV1Parameters{
			KeyBased: &model.KeyBasedV1Parameters{
				AccessKey: &(&types.S{S: stringFinder("access-key")}).S,
				SecretKey: &(&types.S{S: stringFinder("secret-key")}).S,
			},
			GovCloud: &govCloud,
		}
	} else {
		parameters = &model.AwsCredentialV1Parameters{
			RoleBased: &model.RoleBasedV1Parameters{
				RoleArn: &(&types.S{S: stringFinder("role-arn")}).S,
			},
			GovCloud: &govCloud,
		}
	}
	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Aws = parameters
	return credReq, nil
}
