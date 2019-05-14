package kerberos

import (
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1kerberos"
	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/model"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
)

type mockKerberosClient struct {
}

func MockKerberosResponse(name string, environment string) *model.DescribeKerberosConfigV1Response {
	return &model.DescribeKerberosConfigV1Response{
		AdminURL:    "AdminURL",
		ContainerDn: "CD",
		Description: &(&types.S{S: "testdescription"}).S,
		Domain:      "Domain",
		Crn:         "123",
		Name:        name,
		Type:        &(&types.S{S: "ACTIVE_DIRECTORY"}).S,
		URL:         "testUrl",
		Password: &model.SecretResponse{
			EnginePath: "",
			SecretPath: "",
		},
		EnvironmentCrn: &environment,
	}
}

func CheckResponseRow(row utils.Row, expected string, t *testing.T) {
	if strings.Join(row.DataAsStringArray(), " ") != expected {
		t.Errorf("row data not match %s == %s", expected, strings.Join(row.DataAsStringArray(), " "))
	}
}

func (*mockKerberosClient) GetKerberosConfigForEnvironment(params *v1kerberos.GetKerberosConfigForEnvironmentParams) (*v1kerberos.GetKerberosConfigForEnvironmentOK, error) {
	resp := MockKerberosResponse("kerberos_name", *params.EnvironmentCrn)
	return &v1kerberos.GetKerberosConfigForEnvironmentOK{Payload: resp}, nil
}

func (*mockKerberosClient) CreateKerberosConfigForEnvironment(params *v1kerberos.CreateKerberosConfigForEnvironmentParams) (*v1kerberos.CreateKerberosConfigForEnvironmentOK, error) {
	resp := MockKerberosResponse("created", *params.Body.EnvironmentCrn)
	return &v1kerberos.CreateKerberosConfigForEnvironmentOK{Payload: resp}, nil
}

func (*mockKerberosClient) DeleteKerberosConfigForEnvironment(params *v1kerberos.DeleteKerberosConfigForEnvironmentParams) error {
	return nil
}

func TestGetKerberosImpl(t *testing.T) {
	var row utils.Row
	GetKerberosImpl(new(mockKerberosClient), "testget", func(h []string, r utils.Row) { row = r })
	expected := "kerberos_name testdescription ACTIVE_DIRECTORY testget 123"
	CheckResponseRow(row, expected, t)
}

func TestDeleteKerberosImpl(t *testing.T) {
	err := DeleteKerberosImpl(new(mockKerberosClient), 1, "deleted")
	if nil != err {
		t.Errorf("not expected error from delete")
	}
}

func TestCreateKerberosImpl(t *testing.T) {
	var row utils.Row
	environment := "env"
	SendCreateKerberosRequestImpl(new(mockKerberosClient), &model.CreateKerberosConfigV1Request{EnvironmentCrn: &environment}, func(h []string, r utils.Row) { row = r })
	expected := "created testdescription ACTIVE_DIRECTORY env 123"
	CheckResponseRow(row, expected, t)
}
