package clusterdefinition

import (
	"strconv"
	"strings"
	"testing"

	v4bp "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_clusterdefinitions"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
)

type mockClusterDefinitionsClient struct {
}

func (*mockClusterDefinitionsClient) ListClusterDefinitionsByWorkspace(params *v4bp.ListClusterDefinitionsByWorkspaceParams) (*v4bp.ListClusterDefinitionsByWorkspaceOK, error) {
	resp := make([]*model.ClusterDefinitionV4ViewResponse, 0)
	for i := 0; i < 2; i++ {
		id := int64(i)
		resp = append(resp, &model.ClusterDefinitionV4ViewResponse{
			ID:             id,
			Name:           &(&types.S{S: "name" + strconv.Itoa(i)}).S,
			Description:    &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
			HostGroupCount: 3,
			Status:         "USER_MANAGED",
			StackType:      "HDP",
			StackVersion:   "2.6",
		})
	}
	return &v4bp.ListClusterDefinitionsByWorkspaceOK{Payload: &model.ClusterDefinitionV4ViewResponses{Responses: resp}}, nil
}

func (*mockClusterDefinitionsClient) CreateClusterDefinitionInWorkspace(params *v4bp.CreateClusterDefinitionInWorkspaceParams) (*v4bp.CreateClusterDefinitionInWorkspaceOK, error) {
	return &v4bp.CreateClusterDefinitionInWorkspaceOK{Payload: &model.ClusterDefinitionV4Response{ID: int64(1)}}, nil
}

func (*mockClusterDefinitionsClient) DeleteClusterDefinitionInWorkspace(params *v4bp.DeleteClusterDefinitionInWorkspaceParams) (*v4bp.DeleteClusterDefinitionInWorkspaceOK, error) {
	return nil, nil
}

func TestListClusterDefinitionsImpl(t *testing.T) {
	t.Parallel()

	var rows []utils.Row

	workspaceID := int64(2)
	listClusterDefinitionsImpl(workspaceID, new(mockClusterDefinitionsClient), func(h []string, r []utils.Row) { rows = r })

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
