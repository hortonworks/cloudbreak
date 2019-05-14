package gcp

import (
	"encoding/base64"

	"github.com/hortonworks/cb-cli/dataplane/api-environment/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"

	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
)

func (p *GcpProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV1Request, error) {
	var parameters *model.GcpV1Parameters

	if len(stringFinder(fl.FlServiceAccountPrivateKeyFile.Name)) != 0 {
		fileContent := utils.ReadFile(stringFinder(fl.FlServiceAccountPrivateKeyFile.Name))
		b64CredContent := base64.StdEncoding.EncodeToString(fileContent)
		parameters = &model.GcpV1Parameters{
			P12: &model.P12V1Parameters{
				ProjectID:                &(&types.S{S: stringFinder("project-id")}).S,
				ServiceAccountID:         &(&types.S{S: stringFinder("service-account-id")}).S,
				ServiceAccountPrivateKey: &b64CredContent,
			},
		}
	} else {
		fileContent := utils.ReadFile(stringFinder(fl.FlServiceAccountJsonFile.Name))
		b64CredContent := base64.StdEncoding.EncodeToString(fileContent)
		parameters = &model.GcpV1Parameters{
			JSON: &model.JSONV1Parameters{
				CredentialJSON: &b64CredContent,
			},
		}
	}

	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Gcp = parameters
	return credReq, nil
}
