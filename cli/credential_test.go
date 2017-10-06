package cli

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/cli/cloud"
	_ "github.com/hortonworks/hdc-cli/cli/cloud/aws"
	"github.com/hortonworks/hdc-cli/cli/types"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/credentials"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func init() {
	cloud.SetProviderType(cloud.AWS)
}

type mockCredentialCreate struct {
	request chan (*models_cloudbreak.CredentialRequest)
}

func (m *mockCredentialCreate) PostPublicCredential(params *credentials.PostPublicCredentialParams) (*credentials.PostPublicCredentialOK, error) {
	m.request <- params.Body
	defer close(m.request)
	return &credentials.PostPublicCredentialOK{Payload: &models_cloudbreak.CredentialResponse{ID: int64(1), Public: &(&types.B{B: true}).B}}, nil
}

func (m *mockCredentialCreate) PostPrivateCredential(params *credentials.PostPrivateCredentialParams) (*credentials.PostPrivateCredentialOK, error) {
	m.request <- params.Body
	defer close(m.request)
	return &credentials.PostPrivateCredentialOK{Payload: &models_cloudbreak.CredentialResponse{ID: int64(2), Public: &(&types.B{B: false}).B}}, nil
}

func TestCreateCredentialPublic(t *testing.T) {
	boolFinder := func(in string) bool {
		switch in {
		case FlPublic.Name:
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
	mock := mockCredentialCreate{request: make(chan *models_cloudbreak.CredentialRequest)}

	go func() {
		credential := createCredentialImpl(mockStringFinder, mockBoolFinder, &mock)
		if *credential.Public {
			t.Errorf("not private false == %t", *credential.Public)
		}
	}()

	<-mock.request
}

type mockGetPrivatesCredential struct {
}

func (m *mockGetPrivatesCredential) GetPrivatesCredential(params *credentials.GetPrivatesCredentialParams) (*credentials.GetPrivatesCredentialOK, error) {
	resp := make([]*models_cloudbreak.CredentialResponse, 0)
	for i := 0; i < 3; i++ {
		id := int64(i)
		resp = append(resp, &models_cloudbreak.CredentialResponse{
			ID:   id,
			Name: &(&types.S{S: "name" + strconv.Itoa(i)}).S,
		})
	}

	return &credentials.GetPrivatesCredentialOK{Payload: resp}, nil
}

func TestListPrivateCredentialsImpl(t *testing.T) {
	var rows []utils.Row

	listPrivateCredentialsImpl(new(mockGetPrivatesCredential), func(h []string, r []utils.Row) { rows = r })

	if len(rows) != 3 {
		t.Fatalf("row number not match 3 == %d", len(rows))
	}

	for i, r := range rows {
		expected := []string{strconv.Itoa(i), "name" + strconv.Itoa(i)}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}
