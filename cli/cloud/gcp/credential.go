package gcp

import (
	"encoding/base64"

	"github.com/hortonworks/cb-cli/cli/utils"
)

func (p *GcpProvider) CreateCredentialParameters(stringFinder func(string) string, boolFinder func(string) bool) (map[string]interface{}, error) {
	var credentialMap = make(map[string]interface{})
	credentialMap["projectId"] = stringFinder("project-id")
	credentialMap["serviceAccountId"] = stringFinder("service-account-id")
	fileContent := utils.ReadFile(stringFinder("service-account-private-key-file"))
	credentialMap["serviceAccountPrivateKey"] = base64.StdEncoding.EncodeToString(fileContent)
	return credentialMap, nil
}
