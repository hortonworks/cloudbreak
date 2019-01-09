package workspace

import (
	"fmt"
	"strconv"
	"strings"
	"testing"

	v4ws "github.com/hortonworks/cb-cli/dataplane/api/client/v4workspaces"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
)

type mockWorkspaceClient struct {
}

func (*mockWorkspaceClient) GetWorkspaceByName(params *v4ws.GetWorkspaceByNameParams) (*v4ws.GetWorkspaceByNameOK, error) {
	resp := &model.WorkspaceV4Response{
		ID:          1,
		Name:        &(&types.S{S: "test"}).S,
		Description: &(&types.S{S: "desc"}).S,
	}
	return &v4ws.GetWorkspaceByNameOK{Payload: resp}, nil
}

func (*mockWorkspaceClient) GetWorkspaces(params *v4ws.GetWorkspacesParams) (*v4ws.GetWorkspacesOK, error) {
	resp := []*model.WorkspaceV4Response{
		{
			Name:        &(&types.S{S: "test1"}).S,
			Description: &(&types.S{S: "desc1"}).S,
		},
		{
			Name:        &(&types.S{S: "test2"}).S,
			Description: &(&types.S{S: "desc2"}).S,
		},
	}
	return &v4ws.GetWorkspacesOK{Payload: &model.WorkspaceV4Responses{Responses: resp}}, nil
}

func (*mockWorkspaceClient) CreateWorkspace(params *v4ws.CreateWorkspaceParams) (*v4ws.CreateWorkspaceOK, error) {
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
