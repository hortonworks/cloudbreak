package gcp

import (
	"encoding/base64"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"

	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
)

func (p *GcpProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV4Request, error) {
	var parameters *model.GcpCredentialV4Parameters

	if len(stringFinder(fl.FlServiceAccountPrivateKeyFile.Name)) != 0 {
		fileContent := utils.ReadFile(stringFinder(fl.FlServiceAccountPrivateKeyFile.Name))
		b64CredContent := base64.StdEncoding.EncodeToString(fileContent)
		parameters = &model.GcpCredentialV4Parameters{
			P12: &model.P12Parameters{
				ProjectID:                &(&types.S{S: stringFinder("project-id")}).S,
				ServiceAccountID:         &(&types.S{S: stringFinder("service-account-id")}).S,
				ServiceAccountPrivateKey: &b64CredContent,
			},
		}
	} else {
		fileContent := utils.ReadFile(stringFinder(fl.FlServiceAccountJsonFile.Name))
		b64CredContent := base64.StdEncoding.EncodeToString(fileContent)
		parameters = &model.GcpCredentialV4Parameters{
			JSON: &model.JSONParameters{
				CredentialJSON: &b64CredContent,
			},
		}
	}

	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Gcp = parameters
	return credReq, nil
}
