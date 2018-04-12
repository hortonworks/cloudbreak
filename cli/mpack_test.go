package cli

import (
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1mpacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"testing"
)

type mockMpackClient struct {
	postPublicCapture  *models_cloudbreak.MpackRequest
	postPrivateCapture *models_cloudbreak.MpackRequest
}

func (m *mockMpackClient) PostPublicManagementPack(params *v1mpacks.PostPublicManagementPackParams) (*v1mpacks.PostPublicManagementPackOK, error) {
	m.postPublicCapture = params.Body
	return &v1mpacks.PostPublicManagementPackOK{
		Payload: &models_cloudbreak.MpackResponse{
			ID:   1,
			Name: &(&types.S{S: "mpack"}).S,
		},
	}, nil
}

func (m *mockMpackClient) PostPrivateManagementPack(params *v1mpacks.PostPrivateManagementPackParams) (*v1mpacks.PostPrivateManagementPackOK, error) {
	m.postPrivateCapture = params.Body
	return &v1mpacks.PostPrivateManagementPackOK{
		Payload: &models_cloudbreak.MpackResponse{
			ID:   1,
			Name: &(&types.S{S: "mpack"}).S,
		},
	}, nil
}

func (m *mockMpackClient) GetPublicManagementPacks(params *v1mpacks.GetPublicManagementPacksParams) (*v1mpacks.GetPublicManagementPacksOK, error) {
	yes := true
	resp := v1mpacks.GetPublicManagementPacksOK{
		Payload: []*models_cloudbreak.MpackResponse{
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
	})

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
	createMpackImpl(client, "mpack", "mpack description", "http://localhost/mpack.tar.gz", true, "stack-definitions,service-definitions,mpacks", true, true)

	if client.postPublicCapture == nil {
		t.Errorf("no public mpack request was sent")
	}
	req := client.postPublicCapture
	checkReqParams(req, t)
}

func TestCreateMpackImplForPrivate(t *testing.T) {
	t.Parallel()

	client := new(mockMpackClient)
	createMpackImpl(client, "mpack", "mpack description", "http://localhost/mpack.tar.gz", true, "stack-definitions,service-definitions,mpacks", true, false)

	if client.postPrivateCapture == nil {
		t.Errorf("no private mpack request was sent")
	}
	req := client.postPrivateCapture
	checkReqParams(req, t)
}

func checkReqParams(req *models_cloudbreak.MpackRequest, t *testing.T) {
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
	if req.PurgeList[0] != "stack-definitions" || req.PurgeList[1] != "service-definitions" || req.PurgeList[2] != "mpacks" {
		t.Errorf("mpack purge list does not match %s == %s", req.PurgeList, []string{"stack-definitions", "service-definitions", "mpacks"})
	}
}
