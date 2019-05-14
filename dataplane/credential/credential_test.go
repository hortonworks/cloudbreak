package credential

import (
	"strconv"
	"strings"
	"testing"

	v1cred "github.com/hortonworks/cb-cli/dataplane/api-environment/client/v1credentials"

	"errors"

	model "github.com/hortonworks/cb-cli/dataplane/api-environment/model"
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
	request chan *model.CredentialV1Request
}

func (m *mockCredentialCreate) CreateCredentialV1(params *v1cred.CreateCredentialV1Params) (*v1cred.CreateCredentialV1OK, error) {
	m.request <- params.Body
	defer close(m.request)
	return &v1cred.CreateCredentialV1OK{Payload: &model.CredentialV1Response{ID: int64(1), CredentialBase: model.CredentialBase{Name: &(&types.S{S: ""}).S}}}, nil
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

	mock := mockCredentialCreate{request: make(chan *model.CredentialV1Request)}
	go func() {
		createCredentialImpl(mockStringFinder, &mock, false)
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

type mockListCredentials struct {
}

func (m *mockListCredentials) ListCredentialsV1(params *v1cred.ListCredentialsV1Params) (*v1cred.ListCredentialsV1OK, error) {
	resp := make([]*model.CredentialV1Response, 0)
	for i := 0; i < 3; i++ {
		resp = append(resp, &model.CredentialV1Response{
			CredentialBase: model.CredentialBase{
				Name:          &(&types.S{S: "name" + strconv.Itoa(i)}).S,
				Description:   &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
				CloudPlatform: &(&types.S{S: "AWS"}).S,
			},
		})
	}

	return &v1cred.ListCredentialsV1OK{Payload: &model.CredentialV1Responses{Responses: resp}}, nil
}

func TestListCredentialsImpl(t *testing.T) {
	t.Parallel()

	var rows []utils.Row

	listCredentialsImpl(new(mockListCredentials), func(h []string, r []utils.Row) { rows = r })

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

func (m *mockCredentialModifyClient) PutCredentialV1(params *v1cred.PutCredentialV1Params) (*v1cred.PutCredentialV1OK, error) {
	return &v1cred.PutCredentialV1OK{Payload: &model.CredentialV1Response{
		ID: int64(1),
		CredentialBase: model.CredentialBase{
			Name:          params.Body.Name,
			Description:   params.Body.Description,
			CloudPlatform: params.Body.CloudPlatform,
			Aws:           params.Body.Aws,
		},
		Azure: &model.AzureCredentialV1ResponseParameters{
			SubscriptionID: "some",
			TenantID:       "tenantid",
		},
	},
	}, nil
}

func (m *mockCredentialModifyClient) GetCredentialV1(params *v1cred.GetCredentialV1Params) (*v1cred.GetCredentialV1OK, error) {
	if strings.Contains(params.Name, "invalid") {
		return nil, errors.New("credential does not exist")
	}

	return &v1cred.GetCredentialV1OK{Payload: &model.CredentialV1Response{
		ID: int64(1),
		CredentialBase: model.CredentialBase{
			Name:          &(&types.S{S: "name"}).S,
			Description:   &(&types.S{S: "default description"}).S,
			CloudPlatform: &(&types.S{S: "AWS"}).S,
			Aws: &model.AwsCredentialV1Parameters{
				RoleBased: &model.RoleBasedV1Parameters{
					RoleArn: &(&types.S{S: "default-role-arn"}).S,
				},
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

	CredentialV1Response := modifyCredentialImpl(stringFinder, new(mockCredentialModifyClient), false)
	resultArn := CredentialV1Response.Aws.RoleBased.RoleArn
	if resultArn == nil || *resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", *resultArn, expectedArn)
	}
	resultDesc := *CredentialV1Response.Description
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

	CredentialV1Response := modifyCredentialImpl(stringFinder, new(mockCredentialModifyClient), false)
	resultArn := CredentialV1Response.Aws.RoleBased.RoleArn
	if resultArn == nil || *resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", *resultArn, expectedArn)
	}
	resultDesc := *CredentialV1Response.Description
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

	CredentialV1Response := modifyCredentialImpl(stringFinder, new(mockCredentialModifyClient), false)
	resultArn := CredentialV1Response.Aws.RoleBased.RoleArn
	if resultArn == nil || *resultArn != expectedArn {
		t.Errorf("roleArn does not match %s != %s", *resultArn, expectedArn)
	}
	resultDesc := *CredentialV1Response.Description
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

	modifyCredentialImpl(stringFinder, new(mockCredentialModifyClient), false)
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
