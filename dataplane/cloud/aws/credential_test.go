package aws

import (
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"testing"
)

var provider cloud.CloudProvider = new(AwsProvider)

func init() {
	cloud.SetProviderType(cloud.AWS)
}

func TestCreateRoleBasedCredentialParameters(t *testing.T) {
	t.Parallel()

	stringFinder := func(in string) string {
		switch in {
		case "role-arn":
			return "role-arn"
		case "name":
			return "name"
		default:
			return ""
		}
	}

	request, _ := provider.GetCredentialRequest(stringFinder, false)
	roleArn := request.Aws.RoleBased.RoleArn
	if *roleArn != "role-arn" {
		t.Errorf("roleArn not match role-arn == %s", *roleArn)
	}
}

func TestCreateKeyBasedCredentialParameters(t *testing.T) {
	t.Parallel()

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

	request, _ := cloud.GetProvider().GetCredentialRequest(stringFinder, false)
	keyParameters := request.Aws.KeyBased
	accessKey := *keyParameters.AccessKey
	secretKey := *keyParameters.SecretKey

	if accessKey != "access-key" {
		t.Errorf("accessKey not match access-key == %s", accessKey)
	}
	if secretKey != "secret-key" {
		t.Errorf("secretKey not match secret-key == %s", secretKey)
	}
}
