package openstack

import (
	"testing"

	"github.com/hortonworks/cb-cli/dataplane/cloud"
)

var provider cloud.CloudProvider = new(OpenstackProvider)

func init() {
	cloud.SetProviderType(cloud.OPENSTACK)
}

func TestKeystoneV2(t *testing.T) {
	t.Parallel()

	stringFinder := func(in string) string {
		switch in {
		case "facing":
			return ""
		case "user-domain":
			return ""
		default:
			return in
		}
	}

	request, _ := provider.GetCredentialRequest(stringFinder, false)
	keyStonev2Params := request.Openstack.KeystoneV2

	if keyStonev2Params == nil {
		t.Error("keystone version 2 should have been selected")
	}
	if *keyStonev2Params.TenantName != "tenant-name" {
		t.Errorf("tenantName not match tenant-name == %s", *keyStonev2Params.TenantName)
	}
}

func TestKeystoneV3(t *testing.T) {
	t.Parallel()

	stringFinder := func(in string) string {
		switch in {
		case "facing":
			return ""
		case "keystone-scope":
			return ""
		default:
			return in
		}
	}

	request, _ := provider.GetCredentialRequest(stringFinder, false)
	projectParams := request.Openstack.KeystoneV3.Project

	if projectParams == nil {
		t.Errorf("project params should have been selected")
	}
}

func TestValidateAndGetDefaultValue(t *testing.T) {
	t.Parallel()

	actualValue, _ := validateAndGet("", []string{"default", "not-default"})
	expectedValue := "default"

	if expectedValue != actualValue {
		t.Errorf("default value not match default == %s", actualValue)
	}
}

func TestValidateAndGetValidValue(t *testing.T) {
	t.Parallel()

	actualValue, _ := validateAndGet("valid", []string{"not-valid", "valid"})
	expectedValue := "valid"

	if expectedValue != actualValue {
		t.Errorf("allowed value not match valid == %s", actualValue)
	}
}

func TestValidateAndGetInvalidValue(t *testing.T) {
	t.Parallel()

	_, err := validateAndGet("not-valid", []string{"valid1", "valid2"})

	if err == nil {
		t.Error("error doesn't occurred")
	}
}
