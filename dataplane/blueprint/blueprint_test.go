package blueprint

import (
	"strconv"
	"strings"
	"testing"

	v4bp "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_blueprints"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
)

type mockBlueprintsClient struct {
}

func (*mockBlueprintsClient) ListBlueprintsByWorkspace(params *v4bp.ListBlueprintsByWorkspaceParams) (*v4bp.ListBlueprintsByWorkspaceOK, error) {
	resp := make([]*model.BlueprintV4ViewResponse, 0)
	for i := 0; i < 2; i++ {
		id := int64(i)
		resp = append(resp, &model.BlueprintV4ViewResponse{
			ID:             id,
			Name:           &(&types.S{S: "name" + strconv.Itoa(i)}).S,
			Description:    &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
			HostGroupCount: 3,
			Status:         "USER_MANAGED",
			StackType:      "HDP",
			StackVersion:   "2.6",
		})
	}
	return &v4bp.ListBlueprintsByWorkspaceOK{Payload: &model.BlueprintV4ViewResponses{Responses: resp}}, nil
}

func (*mockBlueprintsClient) CreateBlueprintInWorkspace(params *v4bp.CreateBlueprintInWorkspaceParams) (*v4bp.CreateBlueprintInWorkspaceOK, error) {
	return &v4bp.CreateBlueprintInWorkspaceOK{Payload: &model.BlueprintV4Response{Crn: &(&types.S{S: "crn"}).S}}, nil
}

func (*mockBlueprintsClient) DeleteBlueprintsInWorkspace(params *v4bp.DeleteBlueprintsInWorkspaceParams) (*v4bp.DeleteBlueprintsInWorkspaceOK, error) {
	return nil, nil
}

func TestListBlueprintsImpl(t *testing.T) {
	t.Parallel()

	var rows []utils.Row

	workspaceID := int64(2)
	listBlueprintsImpl(workspaceID, true, new(mockBlueprintsClient), func(h []string, r []utils.Row) { rows = r })

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
