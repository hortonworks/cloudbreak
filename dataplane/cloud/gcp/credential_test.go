package gcp

import (
	"encoding/base64"
	"testing"

	"github.com/hortonworks/cb-cli/dataplane/cloud"
)

var provider cloud.CloudProvider = new(GcpProvider)

func init() {
	cloud.SetProviderType(cloud.GCP)
}

func TestCreateCredentialParameters(t *testing.T) {
	t.Parallel()

	stringFinder := func(in string) string {
		switch in {
		case "service-account-private-key-file":
			return "testdata/gcp.p12"
		default:
			return in
		}
	}

	request, _ := provider.GetCredentialRequest(stringFinder, false)
	p12Parameters := request.Gcp.P12

	if *p12Parameters.ProjectID != "project-id" {
		t.Errorf("projectId not match project-id == %s", *p12Parameters.ProjectID)
	}
	if *p12Parameters.ServiceAccountID != "service-account-id" {
		t.Errorf("serviceAccountId not match service-account-id == %s", *p12Parameters.ServiceAccountID)
	}
	if *p12Parameters.ServiceAccountPrivateKey != base64.StdEncoding.EncodeToString([]byte("p12\n")) {
		t.Errorf("serviceAccountPrivateKey not match %s == %s", base64.StdEncoding.EncodeToString([]byte("p12\n")), *p12Parameters.ServiceAccountPrivateKey)
	}
}
