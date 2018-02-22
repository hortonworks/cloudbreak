package cli

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1connectors"
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
	t.Parallel()

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
	t.Parallel()

	var rows []utils.Row
	listAvailabilityZonesImpl(new(mockConnectorsClient), func(h []string, r []utils.Row) { rows = r }, "credentialName", "region")
	if len(rows) != 2 {
		t.Fatalf("row number doesn't match 2 == %d", len(rows))
	}
	for i, r := range rows {
		expected := "av" + strconv.Itoa(i)
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

type mockDiskTypesClient struct {
}

func (*mockDiskTypesClient) GetDisktypes(params *v1connectors.GetDisktypesParams) (*v1connectors.GetDisktypesOK, error) {
	resp := &models_cloudbreak.PlatformDisksJSON{
		DisplayNames: map[string]map[string]string{
			"AWS": {
				"disk": "disk-name",
			},
		},
	}
	return &v1connectors.GetDisktypesOK{Payload: resp}, nil
}

func TestListVolumeTypesImpl(t *testing.T) {
	var rows []utils.Row
	cloud.SetProviderType(cloud.AWS)
	listVolumeTypesImpl(new(mockDiskTypesClient), func(h []string, r []utils.Row) { rows = r })

	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "disk disk-name"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

type mockInstanceTypesClient struct {
}

func (*mockInstanceTypesClient) GetVMTypesByCredentialID(*v2connectors.GetVMTypesByCredentialIDParams) (*v2connectors.GetVMTypesByCredentialIDOK, error) {
	resp := &models_cloudbreak.PlatformVmtypesResponse{
		VMTypes: map[string]models_cloudbreak.VirtualMachinesResponse{
			"region-a": models_cloudbreak.VirtualMachinesResponse{
				VirtualMachines: []*models_cloudbreak.VMTypeJSON{
					&models_cloudbreak.VMTypeJSON{
						Value: "machine",
						VMTypeMetaJSON: &models_cloudbreak.VMTypeMetaJSON{
							Properties: map[string]string{
								"Cpu":    "1",
								"Memory": "1",
							},
						},
					},
				},
			},
		},
	}
	return &v2connectors.GetVMTypesByCredentialIDOK{Payload: resp}, nil
}

func TestListInstanceTypesImpl(t *testing.T) {
	t.Parallel()

	inputs := []struct {
		Region, Avzone string
		Count          int
	}{
		{"region", "region-a", 1},
		{"region-a", "", 1},
		{"region", "", 1},
		{"region-b", "", 0},
		{"region", "region-b", 0},
	}
	var rows []utils.Row
	for _, s := range inputs {
		listInstanceTypesImpl(new(mockInstanceTypesClient), func(h []string, r []utils.Row) { rows = r }, "credential", s.Region, s.Avzone)

		if len(rows) != s.Count {
			t.Fatalf("row number doesn't match %d == %d", s.Count, len(rows))
		}
	}

	listInstanceTypesImpl(new(mockInstanceTypesClient), func(h []string, r []utils.Row) { rows = r }, "credential", "region", "region-a")
	for _, r := range rows {
		expected := "machine 1 1 region-a"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
