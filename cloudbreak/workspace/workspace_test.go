package workspace

import (
	"fmt"
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v3workspaces"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/types"
	"github.com/hortonworks/cb-cli/utils"
)

type mockWorkspaceClient struct {
}

func (*mockWorkspaceClient) GetWorkspaceByName(params *v3workspaces.GetWorkspaceByNameParams) (*v3workspaces.GetWorkspaceByNameOK, error) {
	resp := &model.WorkspaceResponse{
		ID:          1,
		Name:        "test",
		Description: &(&types.S{S: "desc"}).S,
	}
	return &v3workspaces.GetWorkspaceByNameOK{Payload: resp}, nil
}

func (*mockWorkspaceClient) GetWorkspaces(params *v3workspaces.GetWorkspacesParams) (*v3workspaces.GetWorkspacesOK, error) {
	resp := []*model.WorkspaceResponse{
		{
			Name:        "test1",
			Description: &(&types.S{S: "desc1"}).S,
		},
		{
			Name:        "test2",
			Description: &(&types.S{S: "desc2"}).S,
		},
	}
	return &v3workspaces.GetWorkspacesOK{Payload: resp}, nil
}

func (*mockWorkspaceClient) CreateWorkspace(params *v3workspaces.CreateWorkspaceParams) (*v3workspaces.CreateWorkspaceOK, error) {
	return nil, nil
}

func TestListWorkspacesImpl(t *testing.T) {
	var rows []utils.Row
	listWorkspacesImpl(new(mockWorkspaceClient), func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 2 {
		t.Fatalf("row number doesn't match 2 == %d", len(rows))
	}
	for i, r := range rows {
		expected := []string{"test" + strconv.Itoa(i+1), "desc" + strconv.Itoa(i+1), fmt.Sprintln([]string{})}
		if strings.Join(r.DataAsStringArray(), " ") != strings.Join(expected, " ") {
			t.Errorf("row data not match %s == %s", strings.Join(expected, " "), strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

func TestDescribeWorkspaceImpl(t *testing.T) {
	var rows []utils.Row
	describeWorkspaceImpl(new(mockWorkspaceClient), func(h []string, r []utils.Row) { rows = r }, "test")
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}

	for _, r := range rows {
		expected := []string{"test", "desc", fmt.Sprintln([]string{}), "1"}
		if strings.Join(r.DataAsStringArray(), " ") != strings.Join(expected, " ") {
			t.Errorf("row data not match %s == %s", strings.Join(expected, " "), strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
