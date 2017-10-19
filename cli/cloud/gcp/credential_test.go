package gcp

import (
	"encoding/base64"
	"testing"

	"github.com/hortonworks/cb-cli/cli/cloud"
)

var provider cloud.CloudProvider = new(GcpProvider)

func TestCreateCredentialParameters(t *testing.T) {
	stringFinder := func(in string) string {
		switch in {
		case "service-account-private-key-file":
			return "testdata/gcp.p12"
		default:
			return in
		}
	}

	actualMap, _ := provider.GetCredentialParameters(stringFinder, nil)

	if actualMap["projectId"] != "project-id" {
		t.Errorf("projectId not match project-id == %s", actualMap["projectId"])
	}
	if actualMap["serviceAccountId"] != "service-account-id" {
		t.Errorf("serviceAccountId not match service-account-id == %s", actualMap["serviceAccountId"])
	}
	if actualMap["serviceAccountPrivateKey"] != base64.StdEncoding.EncodeToString([]byte("p12\n")) {
		t.Errorf("serviceAccountPrivateKey not match %s == %s", base64.StdEncoding.EncodeToString([]byte("p12\n")), actualMap["serviceAccountPrivateKey"])
	}
}
