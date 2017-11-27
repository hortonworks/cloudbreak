package cli

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v2connectors"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type mockConnectorsClient struct {
}

func (*mockConnectorsClient) GetRegionsByCredentialID(params *v2connectors.GetRegionsByCredentialIDParams) (*v2connectors.GetRegionsByCredentialIDOK, error) {
	resp := &models_cloudbreak.RegionResponse{
		DisplayNames: map[string]string{
			"region": "region-name",
		},
		AvailabilityZones: map[string][]string{
			"region": []string{"av0", "av1"},
		},
	}
	return &v2connectors.GetRegionsByCredentialIDOK{Payload: resp}, nil
}

func TestListRegionsImpl(t *testing.T) {
	var rows []utils.Row
	listRegionsImpl(new(mockConnectorsClient), func(h []string, r []utils.Row) { rows = r }, "credentialName")
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "region region-name"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

func TestListAvailabilityZonesImpl(t *testing.T) {
	var rows []utils.Row
	listAvailabilityZonesImpl(new(mockConnectorsClient), func(h []string, r []utils.Row) { rows = r }, "credentialName", "region")
	if len(rows) != 2 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for i, r := range rows {
		expected := "av" + strconv.Itoa(i)
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
