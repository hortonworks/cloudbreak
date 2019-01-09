package rds

import (
	"strings"
	"testing"

	v4db "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_databases"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/aws"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
)

type mockRdsClient struct {
}

func (*mockRdsClient) ListDatabasesByWorkspace(params *v4db.ListDatabasesByWorkspaceParams) (*v4db.ListDatabasesByWorkspaceOK, error) {
	resp := []*model.DatabaseV4Response{
		{
			Name:             &(&types.S{S: "test"}).S,
			Description:      &(&types.S{S: "description"}).S,
			ConnectionURL:    &(&types.S{S: "connectionURL"}).S,
			DatabaseEngine:   &(&types.S{S: "databaseEngine"}).S,
			Type:             &(&types.S{S: "type"}).S,
			ConnectionDriver: nil,
		},
	}
	return &v4db.ListDatabasesByWorkspaceOK{Payload: &model.DatabaseV4Responses{Responses: resp}}, nil
}

func (*mockRdsClient) CreateDatabaseInWorkspace(params *v4db.CreateDatabaseInWorkspaceParams) (*v4db.CreateDatabaseInWorkspaceOK, error) {
	return nil, nil
}

func (*mockRdsClient) TestDatabaseConnectionInWorkspace(params *v4db.TestDatabaseConnectionInWorkspaceParams) (*v4db.TestDatabaseConnectionInWorkspaceOK, error) {
	return nil, nil
}

func TestListRdsImpl(t *testing.T) {
	var rows []utils.Row
	listAllRdsImpl(new(mockRdsClient), func(h []string, r []utils.Row) { rows = r }, int64(2))
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "test description connectionURL databaseEngine type  "
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
