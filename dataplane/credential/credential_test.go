package credential

import (
	"strconv"
	"strings"
	"testing"

	v4cred "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_credentials"

	"errors"

	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/aws"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
)

func init() {
	cloud.SetProviderType(cloud.AWS)
}

type mockCredentialCreate struct {
	request chan *model.CredentialV4Request
}

func (m *mockCredentialCreate) CreateCredentialInWorkspace(params *v4cred.CreateCredentialInWorkspaceParams) (*v4cred.CreateCredentialInWorkspaceOK, error) {
	m.request <- params.Body
	defer close(m.request)
	return &v4cred.CreateCredentialInWorkspaceOK{Payload: &model.CredentialV4Response{ID: int64(1), Name: &(&types.S{S: ""}).S}}, nil
}

func TestCreateCredentialPublic(t *testing.T) {
	t.Parallel()

	mockStringFinder := func(in string) (r string) {
		switch in {
		case fl.FlName.Name:
			return "name"
		case fl.FlDescriptionOptional.Name:
			return "descritption"
		default:
			return ""
		}
	}

	mockInt64Finder := func(in string) int64 {
		switch in {
		case fl.FlWorkspaceOptional.Name:
			return int64(2)
		default:
			t.Error("workspace option expected")
			return int64(-1)
		}
	}

	mock := mockCredentialCreate{request: make(chan *model.CredentialV4Request)}
	go func() {
		createCredentialImpl(mockStringFinder, mockInt64Finder, &mock, false)
	}()

	actualCredential := <-mock.request

	if *actualCredential.Name != "name" {
		t.Errorf("name not match name == %s", *actualCredential.Name)
	}
	if *actualCredential.Description != "descritption" {
		t.Errorf("descritption not match descritption == %s", *actualCredential.Description)
	}
	if *actualCredential.CloudPlatform != "AWS" {
		t.Errorf("cloud platform not match AWS == %s", *actualCredential.CloudPlatform)
	}
}

type mockListCredentialsByWorkspace struct {
}

func (m *mockListCredentialsByWorkspace) ListCredentialsByWorkspace(params *v4cred.ListCredentialsByWorkspaceParams) (*v4cred.ListCredentialsByWorkspaceOK, error) {
	resp := make([]*model.CredentialV4Response, 0)
	for i := 0; i < 3; i++ {
		resp = append(resp, &model.CredentialV4Response{
			Name:          &(&types.S{S: "name" + strconv.Itoa(i)}).S,
			Description:   &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
			CloudPlatform: &(&types.S{S: "AWS"}).S,
		})
	}

	return &v4cred.ListCredentialsByWorkspaceOK{Payload: &model.CredentialResponses{Responses: resp}}, nil
}

func TestListCredentialsImpl(t *testing.T) {
	t.Parallel()

	var rows []utils.Row

	listCredentialsImpl(new(mockListCredentialsByWorkspace), int64(2), func(h []string, r []utils.Row) { rows = r })

	if len(rows) != 3 {
		t.Fatalf("row number not match 3 == %d", len(rows))
	}

	for i, r := range rows {
		expected := []string{"name" + strconv.Itoa(i), "desc" + strconv.Itoa(i), "AWS"}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}

type mockCredentialModifyClient struct {
}

func (m *mockCredentialModifyClient) PutCredentialInWorkspace(params *v4cred.PutCredentialInWorkspaceParams) (*v4cred.PutCredentialInWorkspaceOK, error) {
	return &v4cred.PutCredentialInWorkspaceOK{Payload: &model.CredentialV4Response{
		ID:            int64(1),
		Name:          params.Body.Name,
		Description:   params.Body.Description,
		CloudPlatform: params.Body.CloudPlatform,
		Aws:           params.Body.Aws,
		Azure: &model.AzureCredentialV4ResponseParameters{
			SubscriptionID: "some",
			TenantID:       "tenantid",
		},
		Gcp:       params.Body.Gcp,
		Openstack: params.Body.Openstack,
		Yarn:      params.Body.Yarn,
	},
	}, nil
}

func (m *mockCredentialModifyClient) GetCredentialInWorkspace(params *v4cred.GetCredentialInWorkspaceParams) (*v4cred.GetCredentialInWorkspaceOK, error) {
	if strings.Contains(params.Name, "invalid") {
		return nil, errors.New("credential does not exist")
	}

	return &v4cred.GetCredentialInWorkspaceOK{Payload: &model.CredentialV4Response{
		ID:            int64(1),
		Name:          &(&types.S{S: "name"}).S,
		Description:   &(&types.S{S: "default description"}).S,
		CloudPlatform: &(&types.S{S: "AWS"}).S,
		Aws: &model.AwsCredentialV4Parameters{
			RoleBased: &model.RoleBasedCredentialParameters{
				RoleArn: &(&types.S{S: "default-role-arn"}).S,
			},
		},
	},
	}, nil
}

func TestModifyCredentialImplForValidChange(t *testing.T) {
	t.Parallel()

	expectedArn := "my-role-arn"
	expectedDesc := "default description"

	stringFinder := func(in string) string {
		switch in {
		case fl.FlName.Name:
			return "name"
		case fl.FlDescriptionOptional.Name:
			return ""
		case fl.FlRoleARN.Name:
			return expectedArn
		default:
			return "empty"
		}
	}

	mockInt64Finder := func(in string) int64 {
		switch in {
		case fl.FlWorkspaceOptional.Name:
			return int64(2)
		default:
			t.Error("workspace option expected")
			return int64(-1)
		}
	}

	CredentialV4Response := modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), false)
	resultArn := CredentialV4Response.Aws.RoleBased.RoleArn
	if resultArn == nil || *resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", *resultArn, expectedArn)
	}
	resultDesc := *CredentialV4Response.Description
	if resultDesc != expectedDesc {
		t.Errorf("description does not match %s != %s", resultDesc, expectedDesc)
	}
}

func TestModifyCredentialImplForDescriptionChange(t *testing.T) {
	t.Parallel()

	expectedArn := "my-role-arn"
	expectedDesc := "new-description"

	stringFinder := func(in string) string {
		switch in {
		case fl.FlName.Name:
			return "name"
		case fl.FlDescriptionOptional.Name:
			return "new-description"
		case fl.FlRoleARN.Name:
			return expectedArn
		default:
			return "empty"
		}
	}

	mockInt64Finder := func(in string) int64 {
		switch in {
		case fl.FlWorkspaceOptional.Name:
			return int64(2)
		default:
			t.Error("workspace option expected")
			return int64(-1)
		}
	}

	CredentialV4Response := modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), false)
	resultArn := CredentialV4Response.Aws.RoleBased.RoleArn
	if resultArn == nil || *resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", *resultArn, expectedArn)
	}
	resultDesc := *CredentialV4Response.Description
	if resultDesc != expectedDesc {
		t.Errorf("description does not match %s != %s", resultDesc, expectedDesc)
	}
}

func TestModifyCredentialImplForDescriptionPublicChange(t *testing.T) {
	t.Parallel()

	expectedArn := "my-role-arn"
	expectedDesc := "new-description"

	stringFinder := func(in string) string {
		switch in {
		case fl.FlName.Name:
			return "public-name"
		case fl.FlDescriptionOptional.Name:
			return "new-description"
		case fl.FlRoleARN.Name:
			return expectedArn
		default:
			return "empty"
		}
	}

	mockInt64Finder := func(in string) int64 {
		switch in {
		case fl.FlWorkspaceOptional.Name:
			return int64(2)
		default:
			t.Error("workspace option expected")
			return int64(-1)
		}
	}

	CredentialV4Response := modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), false)
	resultArn := CredentialV4Response.Aws.RoleBased.RoleArn
	if resultArn == nil || *resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", *resultArn, expectedArn)
	}
	resultDesc := *CredentialV4Response.Description
	if resultDesc != expectedDesc {
		t.Errorf("description does not match %s != %s", resultDesc, expectedDesc)
	}
}

func TestModifyCredentialImplForInvalidCredential(t *testing.T) {
	t.Parallel()

	defer func() {
		if r := recover(); r != nil {
			errorMessage := r.(string)
			if errorMessage != "credential does not exist" {
				t.Error("the error message does not match")
			}
		}
	}()

	stringFinder := func(in string) string {
		switch in {
		case fl.FlName.Name:
			return "invalid-name"
		case fl.FlDescriptionOptional.Name:
			return "new-description"
		default:
			return "empty"
		}
	}

	mockInt64Finder := func(in string) int64 {
		switch in {
		case fl.FlWorkspaceOptional.Name:
			return int64(2)
		default:
			t.Error("workspace option expected")
			return int64(-1)
		}
	}

	modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), false)
	t.Error("the credential modification should fail")
}

func TestAssembleCredentialRequest(t *testing.T) {
	t.Parallel()

	credReq := assembleCredentialRequest("../testdata/os-cred.json", "")
	if credReq.Name == nil || *credReq.Name != "test-openstack-credential" {
		t.Error("credential name parameter does not match")
	}
	if credReq.Description == nil || *credReq.Description != "Test OS keystone v3 credential" {
		t.Error("credential description parameter does not match")
	}
	if credReq.CloudPlatform == nil || *credReq.CloudPlatform != "OPENSTACK" {
		t.Error("credential platform parameter does not match")
	}
}

func TestAssembleCredentialRequestWithNameFlag(t *testing.T) {
	t.Parallel()

	credReq := assembleCredentialRequest("../testdata/os-cred-noname.json", "MyCredential")
	if credReq.Name == nil || *credReq.Name != "MyCredential" {
		t.Error("credential name parameter does not match")
	}
	if credReq.Description == nil || *credReq.Description != "Test OS keystone v3 credential" {
		t.Error("credential description parameter does not match")
	}
	if credReq.CloudPlatform == nil || *credReq.CloudPlatform != "OPENSTACK" {
		t.Error("credential platform parameter does not match")
	}
}
