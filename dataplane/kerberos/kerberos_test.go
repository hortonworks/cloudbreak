package kerberos

import (
	v4krb "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_kerberos"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
	"strings"
	"testing"
)

type mockKerberosClient struct {
}

func MockKerberosResponse(name string, environments []string) *model.KerberosV4Response {
	return &model.KerberosV4Response{
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

func (*mockKerberosClient) ListKerberosConfigByWorkspace(params *v4krb.ListKerberosConfigByWorkspaceParams) (*v4krb.ListKerberosConfigByWorkspaceOK, error) {
	resp := []*model.KerberosViewV4Response{
		{
			Name:        &(&types.S{S: "testkdc"}).S,
			Description: &(&types.S{S: "testdescription"}).S,
			ID:          123,
			Type:        "ACTIVE_DIRECTORY",
		},
	}
	return &v4krb.ListKerberosConfigByWorkspaceOK{Payload: &model.KerberosViewV4Responses{Responses: resp}}, nil
}

func (*mockKerberosClient) GetKerberosConfigInWorkspace(params *v4krb.GetKerberosConfigInWorkspaceParams) (*v4krb.GetKerberosConfigInWorkspaceOK, error) {
	resp := MockKerberosResponse(params.Name, nil)
	return &v4krb.GetKerberosConfigInWorkspaceOK{Payload: resp}, nil
}

func (*mockKerberosClient) CreateKerberosConfigInWorkspace(params *v4krb.CreateKerberosConfigInWorkspaceParams) (*v4krb.CreateKerberosConfigInWorkspaceOK, error) {
	resp := MockKerberosResponse("created", params.Body.Environments)
	return &v4krb.CreateKerberosConfigInWorkspaceOK{Payload: resp}, nil
}

func (*mockKerberosClient) AttachKerberosConfigToEnvironments(params *v4krb.AttachKerberosConfigToEnvironmentsParams) (*v4krb.AttachKerberosConfigToEnvironmentsOK, error) {
	resp := MockKerberosResponse(params.Name, params.Body.EnvironmentNames)
	return &v4krb.AttachKerberosConfigToEnvironmentsOK{Payload: resp}, nil
}

func (*mockKerberosClient) DetachKerberosConfigFromEnvironments(params *v4krb.DetachKerberosConfigFromEnvironmentsParams) (*v4krb.DetachKerberosConfigFromEnvironmentsOK, error) {
	resp := MockKerberosResponse(params.Name, params.Body.EnvironmentNames)
	return &v4krb.DetachKerberosConfigFromEnvironmentsOK{Payload: resp}, nil
}

func (*mockKerberosClient) DeleteKerberosConfigInWorkspace(params *v4krb.DeleteKerberosConfigInWorkspaceParams) (*v4krb.DeleteKerberosConfigInWorkspaceOK, error) {
	resp := MockKerberosResponse(params.Name, nil)
	return &v4krb.DeleteKerberosConfigInWorkspaceOK{Payload: resp}, nil
}

func TestListKerberosImpl(t *testing.T) {
	var rows []utils.Row
	ListKerberosImpl(new(mockKerberosClient), 1, func(h []string, r []utils.Row) { rows = r })
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
	SendCreateKerberosRequestImpl(new(mockKerberosClient), 1, &model.KerberosV4Request{}, func(h []string, r utils.Row) { row = r })
	expected := "created testdescription ACTIVE_DIRECTORY  123"
	CheckResponseRow(row, expected, t)
}
