package gcp

import (
	"encoding/base64"

	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/utils"
)

func (p *GcpProvider) GetCredentialParameters(stringFinder func(string) string) (map[string]interface{}, error) {
	var credentialMap = make(map[string]interface{})
	if len(stringFinder(fl.FlServiceAccountPrivateKeyFile.Name)) != 0 {
		credentialMap["projectId"] = stringFinder("project-id")
		credentialMap["serviceAccountId"] = stringFinder("service-account-id")
		credentialMap["selector"] = "credential-p12"
		fileContent := utils.ReadFile(stringFinder(fl.FlServiceAccountPrivateKeyFile.Name))
		credentialMap["serviceAccountPrivateKey"] = base64.StdEncoding.EncodeToString(fileContent)
	} else {
		fileContent := utils.ReadFile(stringFinder(fl.FlServiceAccountJsonFile.Name))
		credentialMap["selector"] = "credential-json"
		credentialMap["credentialJson"] = base64.StdEncoding.EncodeToString(fileContent)
	}
	return credentialMap, nil
}
