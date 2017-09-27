package aws

func (p *AwsProvider) CreateCredentialParameters(stringFinder func(string) string, boolFinder func(string) bool) map[string]interface{} {
	var credentialMap = make(map[string]interface{})
	credentialMap["selector"] = "role-based"
	credentialMap["roleArn"] = stringFinder("role-arn")
	return credentialMap
}
