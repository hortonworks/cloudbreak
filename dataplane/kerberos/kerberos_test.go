package kerberos

import (
	"github.com/hortonworks/cb-cli/dataplane/api/client/v3_workspace_id_kerberos"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
	"strings"
	"testing"
)

type mockKerberosClient struct {
}

func MockKerberosResponse(name string, environments []string) *model.KerberosResponse {
	return &model.KerberosResponse{
		AdminURL:    "AdminURL",
		ContainerDn: "CD",
		Description: &(&types.S{S: "testdescription"}).S,
		Domain:      "Domain",
		ID:          123,
		Name:        name,
		Type:        &(&types.S{S: "ACTIVE_DIRECTORY"}).S,
		URL:         "testUrl",
		Admin: &model.SecretResponse{
			EnginePath: "",
			SecretPath: "",
		},
		Password: &model.SecretResponse{
			EnginePath: "",
			SecretPath: "",
		},
		Environments: environments,
	}
}

func CheckResponseRow(row utils.Row, expected string, t *testing.T) {
	if strings.Join(row.DataAsStringArray(), " ") != expected {
		t.Errorf("row data not match %s == %s", expected, strings.Join(row.DataAsStringArray(), " "))
	}
}

func (*mockKerberosClient) ListKerberosConfigByWorkspace(params *v3_workspace_id_kerberos.ListKerberosConfigByWorkspaceParams) (*v3_workspace_id_kerberos.ListKerberosConfigByWorkspaceOK, error) {
	resp := []*model.KerberosViewResponse{
		{
			Name:        &(&types.S{S: "testkdc"}).S,
			Description: &(&types.S{S: "testdescription"}).S,
			ID:          123,
			Type:        "ACTIVE_DIRECTORY",
		},
	}
	return &v3_workspace_id_kerberos.ListKerberosConfigByWorkspaceOK{Payload: resp}, nil
}

func (*mockKerberosClient) GetKerberosConfigInWorkspace(params *v3_workspace_id_kerberos.GetKerberosConfigInWorkspaceParams) (*v3_workspace_id_kerberos.GetKerberosConfigInWorkspaceOK, error) {
	resp := MockKerberosResponse(params.Name, nil)
	return &v3_workspace_id_kerberos.GetKerberosConfigInWorkspaceOK{Payload: resp}, nil
}

func (*mockKerberosClient) CreateKerberosConfigInWorkspace(params *v3_workspace_id_kerberos.CreateKerberosConfigInWorkspaceParams) (*v3_workspace_id_kerberos.CreateKerberosConfigInWorkspaceOK, error) {
	resp := MockKerberosResponse("created", params.Body.Environments)
	return &v3_workspace_id_kerberos.CreateKerberosConfigInWorkspaceOK{Payload: resp}, nil
}

func (*mockKerberosClient) AttachKerberosConfigToEnvironments(params *v3_workspace_id_kerberos.AttachKerberosConfigToEnvironmentsParams) (*v3_workspace_id_kerberos.AttachKerberosConfigToEnvironmentsOK, error) {
	resp := MockKerberosResponse(params.Name, params.Body)
	return &v3_workspace_id_kerberos.AttachKerberosConfigToEnvironmentsOK{Payload: resp}, nil
}

func (*mockKerberosClient) DetachKerberosConfigFromEnvironments(params *v3_workspace_id_kerberos.DetachKerberosConfigFromEnvironmentsParams) (*v3_workspace_id_kerberos.DetachKerberosConfigFromEnvironmentsOK, error) {
	resp := MockKerberosResponse(params.Name, params.Body)
	return &v3_workspace_id_kerberos.DetachKerberosConfigFromEnvironmentsOK{Payload: resp}, nil
}

func (*mockKerberosClient) DeleteKerberosConfigInWorkspace(params *v3_workspace_id_kerberos.DeleteKerberosConfigInWorkspaceParams) (*v3_workspace_id_kerberos.DeleteKerberosConfigInWorkspaceOK, error) {
	resp := MockKerberosResponse(params.Name, nil)
	return &v3_workspace_id_kerberos.DeleteKerberosConfigInWorkspaceOK{Payload: resp}, nil
}

func TestListKerberosImpl(t *testing.T) {
	var rows []utils.Row
	ListKerberosImpl(new(mockKerberosClient), 1, "", true, func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "testkdc testdescription ACTIVE_DIRECTORY  123"
		CheckResponseRow(r, expected, t)
	}
}

func TestGetKerberosImpl(t *testing.T) {
	var row utils.Row
	GetKerberosImpl(new(mockKerberosClient), 1, "testget", func(h []string, r utils.Row) { row = r })
	expected := "testget testdescription ACTIVE_DIRECTORY  123"
	CheckResponseRow(row, expected, t)
}

func TestAttachKerberosImpl(t *testing.T) {
	var row utils.Row
	AttachKerberosImpl(new(mockKerberosClient), 1, "testattach", []string{"env1", "env2"}, func(h []string, r utils.Row) { row = r })
	expected := "testattach testdescription ACTIVE_DIRECTORY env1,env2 123"
	CheckResponseRow(row, expected, t)
}

func TestDetachKerberosImpl(t *testing.T) {
	var row utils.Row
	DetachKerberosImpl(new(mockKerberosClient), 1, "testdetach", []string{"env1", "env2"}, func(h []string, r utils.Row) { row = r })
	expected := "testdetach testdescription ACTIVE_DIRECTORY env1,env2 123"
	CheckResponseRow(row, expected, t)
}

func TestDeleteKerberosImpl(t *testing.T) {
	var row utils.Row
	DeleteKerberosImpl(new(mockKerberosClient), 1, "deleted", func(h []string, r utils.Row) { row = r })
	expected := "deleted testdescription ACTIVE_DIRECTORY  123"
	CheckResponseRow(row, expected, t)
}

func TestCreateKerberosImpl(t *testing.T) {
	var row utils.Row
	SendCreateKerberosRequestImpl(new(mockKerberosClient), 1, &model.KerberosRequest{}, func(h []string, r utils.Row) { row = r })
	expected := "created testdescription ACTIVE_DIRECTORY  123"
	CheckResponseRow(row, expected, t)
}
