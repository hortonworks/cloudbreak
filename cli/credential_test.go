package cli

import (
	"strconv"
	"strings"
	"testing"

	"errors"
	"github.com/hortonworks/cb-cli/cli/cloud"
	_ "github.com/hortonworks/cb-cli/cli/cloud/aws"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1credentials"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

func init() {
	cloud.SetProviderType(cloud.AWS)
}

type mockCredentialCreate struct {
	request chan *models_cloudbreak.CredentialRequest
}

func (m *mockCredentialCreate) PostPublicCredential(params *v1credentials.PostPublicCredentialParams) (*v1credentials.PostPublicCredentialOK, error) {
	m.request <- params.Body
	defer close(m.request)
	return &v1credentials.PostPublicCredentialOK{Payload: &models_cloudbreak.CredentialResponse{ID: int64(1), Name: &(&types.S{S: ""}).S, Public: &(&types.B{B: true}).B}}, nil
}

func (m *mockCredentialCreate) PostPrivateCredential(params *v1credentials.PostPrivateCredentialParams) (*v1credentials.PostPrivateCredentialOK, error) {
	m.request <- params.Body
	defer close(m.request)
	return &v1credentials.PostPrivateCredentialOK{Payload: &models_cloudbreak.CredentialResponse{ID: int64(2), Name: &(&types.S{S: ""}).S, Public: &(&types.B{B: false}).B}}, nil
}

func TestCreateCredentialPublic(t *testing.T) {
	t.Parallel()

	boolFinder := func(in string) bool {
		switch in {
		case FlPublicOptional.Name:
			return true
		default:
			return false
		}
	}

	mock := mockCredentialCreate{request: make(chan *models_cloudbreak.CredentialRequest)}

	go func() {
		credential := createCredentialImpl(mockStringFinder, boolFinder, &mock)
		if !*credential.Public {
			t.Errorf("not public true == %t", *credential.Public)
		}
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

func TestCreateCredentialPrivate(t *testing.T) {
	t.Parallel()

	mock := mockCredentialCreate{request: make(chan *models_cloudbreak.CredentialRequest)}

	go func() {
		credential := createCredentialImpl(mockStringFinder, mockBoolFinder, &mock)
		if *credential.Public {
			t.Errorf("not private false == %t", *credential.Public)
		}
	}()

	<-mock.request
}

type mockGetPublicsCredential struct {
}

func (m *mockGetPublicsCredential) GetPublicsCredential(params *v1credentials.GetPublicsCredentialParams) (*v1credentials.GetPublicsCredentialOK, error) {
	resp := make([]*models_cloudbreak.CredentialResponse, 0)
	for i := 0; i < 3; i++ {
		resp = append(resp, &models_cloudbreak.CredentialResponse{
			Name:          &(&types.S{S: "name" + strconv.Itoa(i)}).S,
			Description:   &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
			CloudPlatform: &(&types.S{S: "AWS"}).S,
		})
	}

	return &v1credentials.GetPublicsCredentialOK{Payload: resp}, nil
}

func TestListCredentialsImpl(t *testing.T) {
	t.Parallel()

	var rows []utils.Row

	listCredentialsImpl(new(mockGetPublicsCredential), func(h []string, r []utils.Row) { rows = r })

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

func (m *mockCredentialModifyClient) PutPrivateCredential(params *v1credentials.PutPrivateCredentialParams) (*v1credentials.PutPrivateCredentialOK, error) {
	return &v1credentials.PutPrivateCredentialOK{Payload: &models_cloudbreak.CredentialResponse{
		ID:            int64(1),
		Name:          params.Body.Name,
		Description:   params.Body.Description,
		CloudPlatform: params.Body.CloudPlatform,
		Parameters:    params.Body.Parameters,
		Public:        &(&types.B{B: false}).B,
	},
	}, nil
}

func (m *mockCredentialModifyClient) PutPublicCredential(params *v1credentials.PutPublicCredentialParams) (*v1credentials.PutPublicCredentialOK, error) {
	return &v1credentials.PutPublicCredentialOK{Payload: &models_cloudbreak.CredentialResponse{
		ID:            int64(1),
		Name:          params.Body.Name,
		Description:   params.Body.Description,
		CloudPlatform: params.Body.CloudPlatform,
		Parameters:    params.Body.Parameters,
		Public:        &(&types.B{B: true}).B,
	},
	}, nil
}

func (m *mockCredentialModifyClient) GetPublicCredential(params *v1credentials.GetPublicCredentialParams) (*v1credentials.GetPublicCredentialOK, error) {
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

	return &v1credentials.GetPublicCredentialOK{Payload: &models_cloudbreak.CredentialResponse{
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
		case FlName.Name:
			return "name"
		case FlDescriptionOptional.Name:
			return ""
		case FlRoleARN.Name:
			return expectedArn
		default:
			return "empty"
		}
	}

	credentialResponse := modifyCredentialImpl(stringFinder, mockBoolFinder, new(mockCredentialModifyClient))
	resultArn := credentialResponse.Parameters["roleArn"].(string)
	if resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", resultArn, expectedArn)
	}
	resultDesc := *credentialResponse.Description
	if resultDesc != expectedDesc {
		t.Errorf("description does not match %s != %s", resultDesc, expectedDesc)
	}
	if *credentialResponse.Public {
		t.Error("the credential is not supposed to be public")
	}
}

func TestModifyCredentialImplForDescriptionChange(t *testing.T) {
	t.Parallel()

	expectedArn := "my-role-arn"
	expectedDesc := "new-description"

	stringFinder := func(in string) string {
		switch in {
		case FlName.Name:
			return "name"
		case FlDescriptionOptional.Name:
			return "new-description"
		case FlRoleARN.Name:
			return expectedArn
		default:
			return "empty"
		}
	}

	credentialResponse := modifyCredentialImpl(stringFinder, mockBoolFinder, new(mockCredentialModifyClient))
	resultArn := credentialResponse.Parameters["roleArn"].(string)
	if resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", resultArn, expectedArn)
	}
	resultDesc := *credentialResponse.Description
	if resultDesc != expectedDesc {
		t.Errorf("description does not match %s != %s", resultDesc, expectedDesc)
	}
	if *credentialResponse.Public {
		t.Error("the credential is not supposed to be public")
	}
}

func TestModifyCredentialImplForDescriptionPublicChange(t *testing.T) {
	t.Parallel()

	expectedArn := "my-role-arn"
	expectedDesc := "new-description"

	stringFinder := func(in string) string {
		switch in {
		case FlName.Name:
			return "public-name"
		case FlDescriptionOptional.Name:
			return "new-description"
		case FlRoleARN.Name:
			return expectedArn
		default:
			return "empty"
		}
	}

	credentialResponse := modifyCredentialImpl(stringFinder, mockBoolFinder, new(mockCredentialModifyClient))
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
		case FlName.Name:
			return "invalid-name"
		case FlDescriptionOptional.Name:
			return "new-description"
		default:
			return "empty"
		}
	}

	modifyCredentialImpl(stringFinder, mockBoolFinder, new(mockCredentialModifyClient))
	t.Error("the credential modification should fail")
}

func TestAssembleCredentialRequest(t *testing.T) {
	t.Parallel()

	credReq := assembleCredentialRequest("testdata/os-cred.json", "")
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

	credReq := assembleCredentialRequest("testdata/os-cred-noname.json", "MyCredential")
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
