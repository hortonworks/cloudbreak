package aws

func (p *AwsProvider) CreateCredentialParameters(stringFinder func(string) string, boolFinder func(string) bool) map[string]interface{} {
	var credentialMap = make(map[string]interface{})
	if len(stringFinder("role-arn")) != 0 {
		credentialMap["selector"] = "role-based"
		credentialMap["roleArn"] = stringFinder("role-arn")
	} else {
		credentialMap["selector"] = "key-based"
		credentialMap["accessKey"] = stringFinder("access-key")
		credentialMap["secretKey"] = stringFinder("secret-key")
	}
	return credentialMap
}
