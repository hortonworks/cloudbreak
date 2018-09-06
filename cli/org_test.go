package cli

import (
	"fmt"
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3organizations"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type mockOrgClient struct {
}

func (*mockOrgClient) GetOrganizationByName(params *v3organizations.GetOrganizationByNameParams) (*v3organizations.GetOrganizationByNameOK, error) {
	resp := &models_cloudbreak.OrganizationResponse{
		ID:          1,
		Name:        "test",
		Description: &(&types.S{S: "desc"}).S,
	}
	return &v3organizations.GetOrganizationByNameOK{Payload: resp}, nil
}

func (*mockOrgClient) GetOrganizations(params *v3organizations.GetOrganizationsParams) (*v3organizations.GetOrganizationsOK, error) {
	resp := []*models_cloudbreak.OrganizationResponse{
		{
			Name:        "test1",
			Description: &(&types.S{S: "desc1"}).S,
		},
		{
			Name:        "test2",
			Description: &(&types.S{S: "desc2"}).S,
		},
	}
	return &v3organizations.GetOrganizationsOK{Payload: resp}, nil
}

func (*mockOrgClient) CreateOrganization(params *v3organizations.CreateOrganizationParams) (*v3organizations.CreateOrganizationOK, error) {
	return nil, nil
}

func TestListOrgsImpl(t *testing.T) {
	var rows []utils.Row
	listOrgsImpl(new(mockOrgClient), func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 2 {
		t.Fatalf("row number doesn't match 2 == %d", len(rows))
	}
	for i, r := range rows {
		expected := []string{"test" + strconv.Itoa(i+1), "desc" + strconv.Itoa(i+1), fmt.Sprintln([]string{})}
		if strings.Join(r.DataAsStringArray(), " ") != strings.Join(expected, " ") {
			t.Errorf("row data not match %s == %s", strings.Join(expected, " "), strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

func TestDescribeOrgImpl(t *testing.T) {
	var rows []utils.Row
	describeOrgImpl(new(mockOrgClient), func(h []string, r []utils.Row) { rows = r }, "test")
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}

	for _, r := range rows {
		expected := []string{"test", "desc", fmt.Sprintln([]string{}), "1"}
		if strings.Join(r.DataAsStringArray(), " ") != strings.Join(expected, " ") {
			t.Errorf("row data not match %s == %s", strings.Join(expected, " "), strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
