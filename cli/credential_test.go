package cli

import (
	"io/ioutil"
	"net/http"
	"reflect"
	"regexp"
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client/credentials"
	"github.com/hortonworks/hdc-cli/models"
)

func TestCreateCredential(t *testing.T) {
	finder := func(in string) string {
		switch in {
		case FlSSHKeyURL.Name:
			return "url"
		case FlRoleARN.Name:
			return "role"
		case FlCredentialName.Name:
			return "credential"
		default:
			return ""
		}
	}
	expectedId := int64(1)
	expectedSshKey := "ssh-key"
	var actualSshKeyName string
	getResponse := func(name string) (*http.Response, error) {
		actualSshKeyName = name
		return &http.Response{
			Body: ioutil.NopCloser(strings.NewReader(expectedSshKey)),
		}, nil
	}

	var actualName string
	var actualCredential models.CredentialResponse
	var actualExistingKey string
	var actualPublic bool
	createCred := func(name string, defaultCredential models.CredentialResponse, existingKey string, public bool) int64 {
		actualName = name
		actualCredential = defaultCredential
		actualExistingKey = existingKey
		actualPublic = public
		return expectedId
	}

	createCredential(finder, getResponse, createCred)

	if actualSshKeyName != finder(FlSSHKeyURL.Name) {
		t.Errorf("ssh key name not match %s == %s", finder(FlSSHKeyURL.Name), actualSshKeyName)
	}
	if actualName != finder(FlCredentialName.Name) {
		t.Errorf("name not match %s == %s", finder(FlCredentialName.Name), actualName)
	}
	expectedParams := make(map[string]interface{})
	expectedParams["roleArn"] = finder(FlRoleARN.Name)
	if !reflect.DeepEqual(actualCredential.Parameters, expectedParams) {
		t.Errorf("params not match %s == %s", expectedParams, actualCredential.Parameters)
	}
	if actualCredential.PublicKey != expectedSshKey {
		t.Errorf("ssh key not match %s == %s", expectedSshKey, actualCredential.PublicKey)
	}
	if actualExistingKey != finder(FlSSHKeyPair.Name) {
		t.Errorf("key name name not match %s == %s", finder(FlSSHKeyPair.Name), actualExistingKey)
	}
	if actualPublic != true {
		t.Error("public is false")
	}
}

func TestCopyDefaultCredentialImpl(t *testing.T) {
	skeleton := ClusterSkeleton{ClusterSkeletonBase: ClusterSkeletonBase{SSHKeyName: "ssh-key"}}
	c := make(chan int64, 1)
	getCred := func(name string) models.CredentialResponse {
		return models.CredentialResponse{}
	}
	expectedId := int64(1)
	var actualName string
	var actualExistingKey string
	var actualPublic bool
	createCred := func(name string, defaultCredential models.CredentialResponse, existingKey string, public bool) int64 {
		actualName = name
		actualExistingKey = existingKey
		actualPublic = public
		return expectedId
	}

	copyDefaultCredentialImpl(skeleton, c, getCred, createCred)

	actualId := <-c
	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
	if m, _ := regexp.MatchString("cred([0-9]{10,20})", actualName); m == false {
		t.Errorf("name not match cred([0-9]{10,20}) == %s", actualName)
	}
	if actualExistingKey != skeleton.SSHKeyName {
		t.Errorf("ssh key not match %s == %s", skeleton.SSHKeyName, actualExistingKey)
	}
	if actualPublic != true {
		t.Error("public is false")
	}
}

func TestCreateCredentialImplPublic(t *testing.T) {
	expectedId := int64(1)
	var actualCredential *models.CredentialRequest
	postAccountCredentail := func(params *credentials.PostCredentialsAccountParams) (*credentials.PostCredentialsAccountOK, error) {
		actualCredential = params.Body
		return &credentials.PostCredentialsAccountOK{Payload: &models.CredentialResponse{ID: &expectedId}}, nil
	}
	defaultParams := make(map[string]interface{})
	defaultParams["roleArn"] = "role"
	defaultCredential := models.CredentialResponse{
		Parameters: defaultParams,
		PublicKey:  "pub-key",
	}

	actualId := createCredentialImpl("name", defaultCredential, "ssh-key", true, postAccountCredentail, nil)

	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
	if actualCredential.Name != "name" {
		t.Errorf("name not match name == %s", actualCredential.Name)
	}
	if actualCredential.CloudPlatform != "AWS" {
		t.Errorf("cloud platform not match AWS == %s", actualCredential.CloudPlatform)
	}
	if actualCredential.PublicKey != defaultCredential.PublicKey {
		t.Errorf("public key not match %s == %s", defaultCredential.PublicKey, actualCredential.PublicKey)
	}
	var expectedParams = make(map[string]interface{})
	expectedParams["selector"] = "role-based"
	expectedParams["roleArn"] = defaultParams["roleArn"]
	expectedParams["existingKeyPairName"] = "ssh-key"
	if !reflect.DeepEqual(actualCredential.Parameters, expectedParams) {
		t.Errorf("params not match %s == %s", expectedParams, actualCredential.Parameters)
	}
}

func TestCreateCredentialImplPrivate(t *testing.T) {
	expectedId := int64(1)
	postUserCredential := func(params *credentials.PostCredentialsUserParams) (*credentials.PostCredentialsUserOK, error) {
		return &credentials.PostCredentialsUserOK{Payload: &models.CredentialResponse{ID: &expectedId}}, nil
	}

	actualId := createCredentialImpl("name", models.CredentialResponse{}, "ssh-key", false, nil, postUserCredential)

	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
}

func TestListPrivateCredentialsImpl(t *testing.T) {
	credentials := make([]*models.CredentialResponse, 0)
	for i := 0; i < 3; i++ {
		id := int64(i)
		credentials = append(credentials, &models.CredentialResponse{
			ID:   &id,
			Name: "name" + strconv.Itoa(i),
		})
	}
	var rows []Row

	listPrivateCredentialsImpl(func() []*models.CredentialResponse { return credentials }, func(h []string, r []Row) { rows = r })

	if len(rows) != len(credentials) {
		t.Fatalf("row number not match %d == %d", len(credentials), len(rows))
	}

	for i, r := range rows {
		expected := []string{strconv.Itoa(i), "name" + strconv.Itoa(i)}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}
