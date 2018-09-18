package cli

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_workspace_id_blueprints"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type mockBlueprintsClient struct {
}

func (*mockBlueprintsClient) ListBlueprintsByWorkspace(params *v3_workspace_id_blueprints.ListBlueprintsByWorkspaceParams) (*v3_workspace_id_blueprints.ListBlueprintsByWorkspaceOK, error) {
	resp := make([]*models_cloudbreak.BlueprintViewResponse, 0)
	for i := 0; i < 2; i++ {
		id := int64(i)
		resp = append(resp, &models_cloudbreak.BlueprintViewResponse{
			ID:             id,
			Name:           &(&types.S{S: "name" + strconv.Itoa(i)}).S,
			Description:    &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
			HostGroupCount: 3,
			Status:         "USER_MANAGED",
			StackType:      "HDP",
			StackVersion:   "2.6",
		})
	}
	return &v3_workspace_id_blueprints.ListBlueprintsByWorkspaceOK{Payload: resp}, nil
}

func (*mockBlueprintsClient) CreateBlueprintInWorkspace(params *v3_workspace_id_blueprints.CreateBlueprintInWorkspaceParams) (*v3_workspace_id_blueprints.CreateBlueprintInWorkspaceOK, error) {
	return &v3_workspace_id_blueprints.CreateBlueprintInWorkspaceOK{Payload: &models_cloudbreak.BlueprintResponse{ID: int64(1)}}, nil
}

func (*mockBlueprintsClient) DeleteBlueprintInWorkspace(params *v3_workspace_id_blueprints.DeleteBlueprintInWorkspaceParams) (*v3_workspace_id_blueprints.DeleteBlueprintInWorkspaceOK, error) {
	return nil, nil
}

func TestListBlueprintsImpl(t *testing.T) {
	t.Parallel()

	var rows []utils.Row

	workspaceID := int64(2)
	listBlueprintsImpl(workspaceID, new(mockBlueprintsClient), func(h []string, r []utils.Row) { rows = r })

	if len(rows) != 2 {
		t.Fatalf("row number doesn't match 2 == %d", len(rows))
	}

	for i, r := range rows {
		expected := []string{"name" + strconv.Itoa(i), "desc" + strconv.Itoa(i), "HDP", "2.6", "3", "USER_MANAGED"}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}
