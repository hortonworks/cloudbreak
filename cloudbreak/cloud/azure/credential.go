package azure

func (p *AzureProvider) GetCredentialParameters(stringFinder func(string) string) (map[string]interface{}, error) {
	var credentialMap = make(map[string]interface{})
	credentialMap["subscriptionId"] = stringFinder("subscription-id")
	credentialMap["tenantId"] = stringFinder("tenant-id")
	credentialMap["accessKey"] = stringFinder("app-id")
	credentialMap["secretKey"] = stringFinder("app-password")
	return credentialMap, nil
}
