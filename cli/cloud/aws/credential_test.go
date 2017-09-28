package aws

import (
	"testing"

	"github.com/hortonworks/hdc-cli/cli/cloud"
)

var provider cloud.CloudProvider = new(AwsProvider)

func TestCreateRoleBasedCredentialParameters(t *testing.T) {
	stringFinder := func(in string) string {
		switch in {
		case "role-arn":
			return "role-arn"
		default:
			return ""
		}
	}

	actualMap := provider.CreateCredentialParameters(stringFinder, nil)

	if actualMap["selector"] != "role-based" {
		t.Errorf("selector not match role-based == %s", actualMap["selector"])
	}
	if actualMap["roleArn"] != "role-arn" {
		t.Errorf("roleArn not match role-arn == %s", actualMap["roleArn"])
	}
}

func TestCreateKeyBasedCredentialParameters(t *testing.T) {
	stringFinder := func(in string) string {
		switch in {
		case "access-key":
			return "access-key"
		case "secret-key":
			return "secret-key"
		default:
			return ""
		}
	}

	actualMap := provider.CreateCredentialParameters(stringFinder, nil)

	if actualMap["selector"] != "key-based" {
		t.Errorf("selector not match key-based == %s", actualMap["selector"])
	}
	if actualMap["accessKey"] != "access-key" {
		t.Errorf("accessKey not match access-key == %s", actualMap["accessKey"])
	}
	if actualMap["secretKey"] != "secret-key" {
		t.Errorf("secretKey not match secret-key == %s", actualMap["secretKey"])
	}
}
