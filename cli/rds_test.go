package cli

import (
	"strings"
	"testing"

	_ "github.com/hortonworks/cb-cli/cli/cloud/aws"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_workspace_id_rdsconfigs"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type mockRdsClient struct {
}

func (*mockRdsClient) ListRdsConfigsByWorkspace(params *v3_workspace_id_rdsconfigs.ListRdsConfigsByWorkspaceParams) (*v3_workspace_id_rdsconfigs.ListRdsConfigsByWorkspaceOK, error) {
	resp := []*models_cloudbreak.RDSConfigResponse{
		{
			Name:             &(&types.S{S: "test"}).S,
			ConnectionURL:    &(&types.S{S: "connectionURL"}).S,
			DatabaseEngine:   &(&types.S{S: "databaseEngine"}).S,
			Type:             &(&types.S{S: "type"}).S,
			ConnectionDriver: nil,
		},
	}
	return &v3_workspace_id_rdsconfigs.ListRdsConfigsByWorkspaceOK{Payload: resp}, nil
}

func (*mockRdsClient) CreateRdsConfigInWorkspace(params *v3_workspace_id_rdsconfigs.CreateRdsConfigInWorkspaceParams) (*v3_workspace_id_rdsconfigs.CreateRdsConfigInWorkspaceOK, error) {
	return nil, nil
}

func (*mockRdsClient) TestRdsConnectionInWorkspace(params *v3_workspace_id_rdsconfigs.TestRdsConnectionInWorkspaceParams) (*v3_workspace_id_rdsconfigs.TestRdsConnectionInWorkspaceOK, error) {
	return nil, nil
}

func TestListRdsImpl(t *testing.T) {
	var rows []utils.Row
	listAllRdsImpl(new(mockRdsClient), func(h []string, r []utils.Row) { rows = r }, int64(2))
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "test connectionURL databaseEngine type "
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
