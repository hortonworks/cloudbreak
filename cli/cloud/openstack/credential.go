package openstack

import (
	"errors"
	"fmt"
)

const (
	KEYSTONE_V2    = "cb-keystone-v2"
	KEYSTONE_V3    = "cb-keystone-v3"
	SCOPE_TEMPLATE = "cb-keystone-v3-%s-scope"
)

var FACINGS []string = []string{"public", "admin", "internal"}
var SCOPES []string = []string{"default", "project", "domain"}

func (p *OpenstackProvider) GetCredentialParameters(stringFinder func(string) string, boolFinder func(string) bool) (map[string]interface{}, error) {
	facing, err := validateAndGet(stringFinder("facing"), FACINGS)
	if err != nil {
		return nil, err
	}
	var credentialMap = make(map[string]interface{})
	credentialMap["facing"] = facing
	credentialMap["userName"] = stringFinder("tenant-user")
	credentialMap["password"] = stringFinder("tenant-password")
	credentialMap["endpoint"] = stringFinder("endpoint")
	if len(stringFinder("user-domain")) != 0 {
		credentialMap["keystoneVersion"] = KEYSTONE_V3
		scope, err := validateAndGet(stringFinder("keystone-scope"), SCOPES)
		if err != nil {
			return nil, err
		}
		credentialMap["keystoneAuthScope"] = fmt.Sprintf(SCOPE_TEMPLATE, scope)
		credentialMap["selector"] = credentialMap["keystoneAuthScope"]
		credentialMap["userDomain"] = stringFinder("user-domain")
	} else {
		credentialMap["keystoneVersion"] = KEYSTONE_V2
		credentialMap["selector"] = KEYSTONE_V2
		credentialMap["tenantName"] = stringFinder("tenant-name")
	}
	return credentialMap, nil
}

func validateAndGet(value string, values []string) (string, error) {
	if len(value) == 0 {
		return values[0], nil
	} else {
		for _, v := range values {
			if v == value {
				return value, nil
			}
		}
	}
	return "", errors.New(fmt.Sprintf("%s not allowed", value))
}
