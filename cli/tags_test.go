package cli

import (
	"fmt"
	"sort"
	"strings"
	"testing"

	_ "github.com/hortonworks/cb-cli/cli/cloud/aws"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1accountpreferences"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type mockAccountTagsClient struct {
	params chan map[string]string
}

func (*mockAccountTagsClient) GetAccountPreferencesEndpoint(params *v1accountpreferences.GetAccountPreferencesEndpointParams) (*v1accountpreferences.GetAccountPreferencesEndpointOK, error) {
	resp := &models_cloudbreak.AccountPreference{
		DefaultTags: map[string]string{
			"key0": "val0",
			"key1": "val1",
			"key2": "val2",
		},
	}
	return &v1accountpreferences.GetAccountPreferencesEndpointOK{Payload: resp}, nil
}

func (m *mockAccountTagsClient) PutAccountPreferencesEndpoint(params *v1accountpreferences.PutAccountPreferencesEndpointParams) (*v1accountpreferences.PutAccountPreferencesEndpointOK, error) {
	resp := &models_cloudbreak.AccountPreference{}
	m.params <- params.Body.DefaultTags
	defer close(m.params)
	return &v1accountpreferences.PutAccountPreferencesEndpointOK{Payload: resp}, nil
}

func TestListAccountTagsImpl(t *testing.T) {
	var rows []utils.Row
	listAccountTagsImpl(new(mockAccountTagsClient), func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 3 {
		t.Fatalf("row number doesn't match 3 == %d", len(rows))
	}
	sort.Slice(rows, func(i int, j int) bool {
		return rows[i].DataAsStringArray()[0] < rows[j].DataAsStringArray()[0]
	})
	for i, r := range rows {
		expected := fmt.Sprintf("key%d val%d", i, i)
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

func TestAddAccountTagsImpl(t *testing.T) {
	client := &mockAccountTagsClient{params: make(chan map[string]string)}
	go addAccountTagsImpl(client, "key", "value")
	m := <-client.params

	if len(m) != 4 {
		t.Fatalf("row number doesn't match 4 == %d", len(m))
	}
	if _, ok := m["key"]; !ok {
		t.Fatalf("key not added to request")
	}
}

func TestDeleteAccountTagsImpl(t *testing.T) {
	client := &mockAccountTagsClient{params: make(chan map[string]string)}
	go deleteAccountTagsImpl(client, "key1")
	m := <-client.params

	if len(m) != 2 {
		t.Fatalf("row number doesn't match 2 == %d", len(m))
	}
	if _, ok := m["key1"]; ok {
		t.Fatalf("key not deleted from request")
	}
}
