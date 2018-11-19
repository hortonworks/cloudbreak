package mpack

import (
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v3_workspace_id_mpacks"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/types"
	"github.com/hortonworks/cb-cli/dps-common/utils"
)

type mockMpackClient struct {
	postPublicCapture  *model.ManagementPackRequest
	postPrivateCapture *model.ManagementPackRequest
}

func (m *mockMpackClient) CreateManagementPackInWorkspace(params *v3_workspace_id_mpacks.CreateManagementPackInWorkspaceParams) (*v3_workspace_id_mpacks.CreateManagementPackInWorkspaceOK, error) {
	m.postPublicCapture = params.Body
	return &v3_workspace_id_mpacks.CreateManagementPackInWorkspaceOK{
		Payload: &model.ManagementPackResponse{
			ID:   1,
			Name: &(&types.S{S: "mpack"}).S,
		},
	}, nil
}

func (m *mockMpackClient) ListManagementPacksByWorkspace(params *v3_workspace_id_mpacks.ListManagementPacksByWorkspaceParams) (*v3_workspace_id_mpacks.ListManagementPacksByWorkspaceOK, error) {
	yes := true
	resp := v3_workspace_id_mpacks.ListManagementPacksByWorkspaceOK{
		Payload: []*model.ManagementPackResponse{
			{
				Name:     &(&types.S{S: "mpack"}).S,
				MpackURL: &(&types.S{S: "http://localhost/mpack.tar.gz"}).S,
			},
			{
				Name:        &(&types.S{S: "mpack"}).S,
				MpackURL:    &(&types.S{S: "http://localhost/mpack.tar.gz"}).S,
				Description: &(&types.S{S: "my test mpack"}).S,
			},
			{
				Name:        &(&types.S{S: "mpack"}).S,
				MpackURL:    &(&types.S{S: "http://localhost/mpack.tar.gz"}).S,
				Description: &(&types.S{S: "my test mpack"}).S,
				Purge:       &yes,
			},
			{
				Name:        &(&types.S{S: "mpack"}).S,
				MpackURL:    &(&types.S{S: "http://localhost/mpack.tar.gz"}).S,
				Description: &(&types.S{S: "my test mpack"}).S,
				Purge:       &yes,
				PurgeList:   []string{"stack-definitions", "service-definitions", "mpacks"},
			},
			{
				Name:        &(&types.S{S: "mpack"}).S,
				MpackURL:    &(&types.S{S: "http://localhost/mpack.tar.gz"}).S,
				Description: &(&types.S{S: "my test mpack"}).S,
				Purge:       &yes,
				PurgeList:   []string{"stack-definitions", "service-definitions", "mpacks"},
				Force:       &yes,
			},
		},
	}
	return &resp, nil
}

func TestListMpacksImpl(t *testing.T) {
	t.Parallel()

	var mpackRows []utils.Row
	client := new(mockMpackClient)
	listMpacksImpl(client, func(header []string, rows []utils.Row) {
		mpackRows = rows
	}, int64(2))

	if len(mpackRows) != 5 {
		t.Errorf("invalid number of mpack rows returned %d == %d", len(mpackRows), 5)
	}
	for i := 0; i < 5; i++ {
		params := mpackRows[i].DataAsStringArray()
		name := params[0]
		desciption := params[1]
		url := params[2]
		purge := params[3]
		purgeList := params[4]
		force := params[5]
		if name != "mpack" {
			t.Errorf("mpack name does not match %s == %s", name, "mpack")
		}
		if i > 1 && desciption != "my test mpack" {
			t.Errorf("mpack description does not match %s == %s", desciption, "my test mpack")
		}
		if i > 2 && url != "http://localhost/mpack.tar.gz" {
			t.Errorf("mpack URL does not match %s == %s", url, "http://localhost/mpack.tar.gz")
		}
		if i > 3 && purge != "true" {
			t.Errorf("mpack purge does not match %s == %s", purge, "true")
		}
		if i > 4 && purgeList != "stack-definitions,service-definitions,mpacks" {
			t.Errorf("mpack purge list does not match %s == %s", purgeList, "stack-definitions,service-definitions,mpacks")
		}
		if i == 5 && force != "true" {
			t.Errorf("mpacl force does not match %s == %s", force, "true")
		}
		if i < 4 && force != "false" {
			t.Errorf("mpack force does not match %s == %s", force, "false")
		}
		if i < 3 && purgeList != "" {
			t.Errorf("mpack purge list does not match %s == %s", purgeList, "")
		}
		if i < 2 && purge != "false" {
			t.Errorf("mpack purge does not match %s == %s", purge, "false")
		}
		if i < 1 && desciption != "" {
			t.Errorf("mpack description does not match %s == %s", desciption, "")
		}
	}
}

func TestCreateMpackImplForPublic(t *testing.T) {
	t.Parallel()

	client := new(mockMpackClient)
	createMpackImpl(client, int64(2), "mpack", "mpack description", "http://localhost/mpack.tar.gz", true, "stack-definitions,service-definitions,mpacks", true)

	if client.postPublicCapture == nil {
		t.Errorf("no public mpack request was sent")
	}
	req := client.postPublicCapture
	checkReqParams(req, "stack-definitions,service-definitions,mpacks", t)
}

func TestCreateMpackForEmptyPurgeList(t *testing.T) {
	t.Parallel()

	client := new(mockMpackClient)
	createMpackImpl(client, int64(2), "mpack", "mpack description", "http://localhost/mpack.tar.gz", true, "", true)

	if client.postPublicCapture == nil {
		t.Errorf("no mpack request was sent")
	}
	req := client.postPublicCapture
	checkReqParams(req, "", t)
}

func checkReqParams(req *model.ManagementPackRequest, purgeList string, t *testing.T) {
	if *req.Name != "mpack" {
		t.Errorf("mpack name does not match %s == %s", *req.Name, "mpack")
	}
	if *req.Description != "mpack description" {
		t.Errorf("mapck description does not match %s == %s", *req.Description, "mpack description")
	}
	if *req.MpackURL != "http://localhost/mpack.tar.gz" {
		t.Errorf("mpack url does not match %s == %s", *req.MpackURL, "http://localhost/mpack.tar.gz")
	}
	if *req.Purge != true {
		t.Errorf("mpack purge does not match %v == %v", *req.Purge, true)
	}
	if strings.Join(req.PurgeList, ",") != purgeList {
		t.Errorf("mpack purge list does not match %s == %s", req.PurgeList, []string{"stack-definitions", "service-definitions", "mpacks"})
	}
}
