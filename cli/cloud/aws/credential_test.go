package aws

import (
	"testing"

	"github.com/hortonworks/hdc-cli/cli/cloud"
)

var provider cloud.CloudProvider = new(AwsProvider)

func TestCreateCredentialParameters(t *testing.T) {
	stringFinder := func(in string) string {
		switch in {
		case "role-arn":
			return "role-arn"
		default:
			return ""
		}
	}

	actualMap := provider.CreateCredentialParameters(stringFinder, nil)

	if actualMap["roleArn"] != "role-arn" {
		t.Errorf("roleArn not match role-arn == %s", actualMap["roleArn"])
	}

}
