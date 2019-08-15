package sdx

import (
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/dataplane/api-sdx/client/sdx"
	sdxModel "github.com/hortonworks/cb-cli/dataplane/api-sdx/model"
	"github.com/hortonworks/dp-cli-common/utils"
)

type mockListSdxClustersClient struct {
}

func (*mockListSdxClustersClient) ListSdx(params *sdx.ListSdxParams) (*sdx.ListSdxOK, error) {
	resp := []*sdxModel.SdxClusterResponse{
		{
			Crn:                        "crn:altus:sdx:us-west-1:tenantName:sdxcluster:b8a64902-7765-4ddd-a4f3-df81ae585e10",
			Name:                       "ExampleClusterName",
			EnvironmentName:            "ExampleEnvironmentName",
			EnvironmentCrn:             "crn:altus:environment:us-west-1:tenantName:sdxcluster:aaa64902-1111-4567-123-1111111",
			StackCrn:                   "crn:altus:stack:us-west-1:tenantName:stack:aaa64902-1111-4567-123-000000000",
			DatabaseServerCrn:          "crn:altus:environment:us-west-1:tenantName:databaseserver:aaa64902-1111-4567-123-222222222",
			CloudStorageBaseLocation:   "cloudbreak-bucket/test",
			CloudStorageFileSystemType: "S3",
			Status:                     "AVAILABLE",
			StatusReason:               "Reason",
		},
	}
	return &sdx.ListSdxOK{Payload: resp}, nil
}

func TestListSdx(t *testing.T) {
	t.Parallel()
	envName := "aws-env"
	var rows []utils.Row
	listSdxClusterImpl(new(mockListSdxClustersClient), envName, func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		strings.Split("a,b,c", " ")
		expected := "crn:altus:sdx:us-west-1:tenantName:sdxcluster:b8a64902-7765-4ddd-a4f3-df81ae585e10 ExampleClusterName ExampleEnvironmentName crn:altus:environment:us-west-1:tenantName:sdxcluster:aaa64902-1111-4567-123-1111111 crn:altus:stack:us-west-1:tenantName:stack:aaa64902-1111-4567-123-000000000 crn:altus:environment:us-west-1:tenantName:databaseserver:aaa64902-1111-4567-123-222222222 cloudbreak-bucket/test S3 AVAILABLE Reason"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

func TestIfDatabaseNeeded(t *testing.T) {
	sdxRequest := &sdxModel.SdxClusterRequest{
		ClusterShape:     nil,
		Environment:      nil,
		Tags:             nil,
		CloudStorage:     nil,
		ExternalDatabase: nil,
	}
	setupExternalDbIfNeeded(true, false, &sdxRequest.ExternalDatabase)

	if !*sdxRequest.ExternalDatabase.Create {
		t.Errorf("external database not set")
	}
}

func TestIfDatabaseNeededFalse(t *testing.T) {
	sdxRequest := &sdxModel.SdxClusterRequest{
		ClusterShape:     nil,
		Environment:      nil,
		Tags:             nil,
		CloudStorage:     nil,
		ExternalDatabase: nil,
	}
	setupExternalDbIfNeeded(false, true, &sdxRequest.ExternalDatabase)

	if *sdxRequest.ExternalDatabase.Create {
		t.Errorf("external database set and should not be")
	}
}

func TestSetupCloudStorageIfNeeded(t *testing.T) {
	sdxRequest := &sdxModel.SdxClusterRequest{
		ClusterShape:     nil,
		Environment:      nil,
		Tags:             nil,
		CloudStorage:     nil,
		ExternalDatabase: nil,
	}
	setupCloudStorageIfNeeded("location", "instanceProfiule", sdxRequest)

	if sdxRequest.CloudStorage == nil {
		t.Errorf("CloudStorage was not set correctly")
	}
}
