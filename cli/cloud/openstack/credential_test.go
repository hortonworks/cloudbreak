package openstack

import (
	"testing"

	"github.com/hortonworks/hdc-cli/cli/cloud"
)

var provider cloud.CloudProvider = new(OpenstackProvider)

func TestKeystoneV2(t *testing.T) {
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

	actualMap, _ := provider.CreateCredentialParameters(stringFinder, nil)

	if actualMap["selector"] != KEYSTONE_V2 {
		t.Errorf("selector not match %s == %s", KEYSTONE_V2, actualMap["selector"])
	}
	if actualMap["keystoneVersion"] != KEYSTONE_V2 {
		t.Errorf("keystoneVersion not match %s == %s", KEYSTONE_V2, actualMap["keystoneVersion"])
	}
	if actualMap["tenantName"] != "tenant-name" {
		t.Errorf("tenantName not match tenant-name == %s", actualMap["tenantName"])
	}
}

func TestKeystoneV3(t *testing.T) {
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

	actualMap, _ := provider.CreateCredentialParameters(stringFinder, nil)

	if actualMap["selector"] != "cb-keystone-v3-default-scope" {
		t.Errorf("selector not match cb-keystone-v3-default-scope == %s", actualMap["selector"])
	}
	if actualMap["keystoneVersion"] != KEYSTONE_V3 {
		t.Errorf("keystoneVersion not match %s == %s", KEYSTONE_V3, actualMap["keystoneVersion"])
	}
	if actualMap["keystoneAuthScope"] != "cb-keystone-v3-default-scope" {
		t.Errorf("keystoneAuthScope not match cb-keystone-v3-default-scope == %s", actualMap["keystoneAuthScope"])
	}
}

func TestValidateAndGetDefaultValue(t *testing.T) {
	actualValue, _ := validateAndGet("", []string{"default", "not-default"})
	expectedValue := "default"

	if expectedValue != actualValue {
		t.Errorf("default value not match default == %s", actualValue)
	}
}

func TestValidateAndGetValidValue(t *testing.T) {
	actualValue, _ := validateAndGet("valid", []string{"not-valid", "valid"})
	expectedValue := "valid"

	if expectedValue != actualValue {
		t.Errorf("allowed value not match valid == %s", actualValue)
	}
}

func TestValidateAndGetInvalidValue(t *testing.T) {
	_, err := validateAndGet("not-valid", []string{"valid1", "valid2"})

	if err == nil {
		t.Error("error doesn't occured")
	}
}
