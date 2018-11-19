package credential

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v3_workspace_id_credentials"

	"errors"

	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/cloud"
	_ "github.com/hortonworks/cb-cli/cloudbreak/cloud/aws"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/types"
	"github.com/hortonworks/cb-cli/dps-common/utils"
)

func init() {
	cloud.SetProviderType(cloud.AWS)
}

type mockCredentialCreate struct {
	request chan *model.CredentialRequest
}

func (m *mockCredentialCreate) CreateCredentialInWorkspace(params *v3_workspace_id_credentials.CreateCredentialInWorkspaceParams) (*v3_workspace_id_credentials.CreateCredentialInWorkspaceOK, error) {
	m.request <- params.Body
	defer close(m.request)
	return &v3_workspace_id_credentials.CreateCredentialInWorkspaceOK{Payload: &model.CredentialResponse{ID: int64(1), Name: &(&types.S{S: ""}).S, Public: &(&types.B{B: true}).B}}, nil
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

	mock := mockCredentialCreate{request: make(chan *model.CredentialRequest)}
	parameters := map[string]interface{}{}
	go func() {
		createCredentialImpl(mockStringFinder, mockInt64Finder, &mock, parameters)
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

func (m *mockListCredentialsByWorkspace) ListCredentialsByWorkspace(params *v3_workspace_id_credentials.ListCredentialsByWorkspaceParams) (*v3_workspace_id_credentials.ListCredentialsByWorkspaceOK, error) {
	resp := make([]*model.CredentialResponse, 0)
	for i := 0; i < 3; i++ {
		resp = append(resp, &model.CredentialResponse{
			Name:          &(&types.S{S: "name" + strconv.Itoa(i)}).S,
			Description:   &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
			CloudPlatform: &(&types.S{S: "AWS"}).S,
		})
	}

	return &v3_workspace_id_credentials.ListCredentialsByWorkspaceOK{Payload: resp}, nil
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

func (m *mockCredentialModifyClient) PutCredentialInWorkspace(params *v3_workspace_id_credentials.PutCredentialInWorkspaceParams) (*v3_workspace_id_credentials.PutCredentialInWorkspaceOK, error) {
	return &v3_workspace_id_credentials.PutCredentialInWorkspaceOK{Payload: &model.CredentialResponse{
		ID:            int64(1),
		Name:          params.Body.Name,
		Description:   params.Body.Description,
		CloudPlatform: params.Body.CloudPlatform,
		Parameters:    params.Body.Parameters,
		Public:        &(&types.B{B: true}).B,
	},
	}, nil
}

func (m *mockCredentialModifyClient) GetCredentialInWorkspace(params *v3_workspace_id_credentials.GetCredentialInWorkspaceParams) (*v3_workspace_id_credentials.GetCredentialInWorkspaceOK, error) {
	var credentialMap = make(map[string]interface{})
	credentialMap["selector"] = "role-based"
	credentialMap["roleArn"] = "default-role-arn"

	public := false
	if strings.Contains(params.Name, "public") {
		public = true
	}
	if strings.Contains(params.Name, "invalid") {
		return nil, errors.New("credential does not exist")
	}

	return &v3_workspace_id_credentials.GetCredentialInWorkspaceOK{Payload: &model.CredentialResponse{
		ID:            int64(1),
		Name:          &(&types.S{S: "name"}).S,
		Description:   &(&types.S{S: "default description"}).S,
		CloudPlatform: &(&types.S{S: "AWS"}).S,
		Parameters:    credentialMap,
		Public:        &public,
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

	parameters := map[string]interface{}{}
	credentialResponse := modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), parameters)
	resultArn := credentialResponse.Parameters["roleArn"].(string)
	if resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", resultArn, expectedArn)
	}
	resultDesc := *credentialResponse.Description
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

	parameters := map[string]interface{}{}
	credentialResponse := modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), parameters)
	resultArn := credentialResponse.Parameters["roleArn"].(string)
	if resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", resultArn, expectedArn)
	}
	resultDesc := *credentialResponse.Description
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

	parameters := map[string]interface{}{}
	credentialResponse := modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), parameters)
	resultArn := credentialResponse.Parameters["roleArn"].(string)
	if resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", resultArn, expectedArn)
	}
	resultDesc := *credentialResponse.Description
	if resultDesc != expectedDesc {
		t.Errorf("description does not match %s != %s", resultDesc, expectedDesc)
	}
	if !*credentialResponse.Public {
		t.Error("the credential is not supposed to be private")
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

	parameters := map[string]interface{}{}
	modifyCredentialImpl(stringFinder, mockInt64Finder, new(mockCredentialModifyClient), parameters)
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
	params := credReq.Parameters
	if params["selector"] != "cb-keystone-v3-project-scope" {
		t.Error("credential selector parameter does not match")
	}
	if params["keystoneAuthScope"] != "cb-keystone-v3-project-scope" {
		t.Error("credential auth scope parameter does not match")
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
	params := credReq.Parameters
	if params["selector"] != "cb-keystone-v3-project-scope" {
		t.Error("credential selector parameter does not match")
	}
	if params["keystoneAuthScope"] != "cb-keystone-v3-project-scope" {
		t.Error("credential auth scope parameter does not match")
	}
}
