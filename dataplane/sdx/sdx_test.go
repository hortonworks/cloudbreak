package sdx

import (
	"github.com/hortonworks/cb-cli/dataplane/api-sdx/client/sdx"
	sdxModel "github.com/hortonworks/cb-cli/dataplane/api-sdx/model"
	"github.com/hortonworks/dp-cli-common/utils"
	"strings"
	"testing"
)

type mockListSdxClustersClient struct {
}

func (*mockListSdxClustersClient) ListSdx(params *sdx.ListSdxParams) (*sdx.ListSdxOK, error) {
	resp := []*sdxModel.SdxClusterResponse{
		{
			SdxName: "ExampleClusterName",
			Status:  "AVAILABLE",
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
		expected := "ExampleClusterName"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
