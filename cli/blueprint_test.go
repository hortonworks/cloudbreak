package cli

import (
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/blueprints"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"strconv"
	"strings"
	"testing"
)

type mockBlueprintsClient struct {
}

func (*mockBlueprintsClient) GetPrivatesBlueprint(params *blueprints.GetPrivatesBlueprintParams) (*blueprints.GetPrivatesBlueprintOK, error) {
	resp := make([]*models_cloudbreak.BlueprintResponse, 0)
	for i := 0; i < 2; i++ {
		id := int64(i)
		resp = append(resp, &models_cloudbreak.BlueprintResponse{
			ID:              id,
			Name:            &(&types.S{S: "name" + strconv.Itoa(i)}).S,
			Description:     &(&types.S{S: "desc" + strconv.Itoa(i)}).S,
			HostGroupCount:  3,
			Status:          "USER_MANAGED",
			AmbariBlueprint: "eyJCbHVlcHJpbnRzIjp7ImJsdWVwcmludF9uYW1lIjoiaGRwMjYtZGF0YS1zY2llbmNlLXNwYXJrMiIsInN0YWNrX25hbWUiOiJIRFAiLCJzdGFja192ZXJzaW9uIjoiMi42In0sInNldHRpbmdzIjpbeyJyZWNvdmVyeV9zZXR0aW5ncyI6W119LHsic2VydmljZV9zZXR0aW5ncyI6W3sibmFtZSI6IkhJVkUiLCJjcmVkZW50aWFsX3N0b3JlX2VuYWJsZWQiOiJmYWxzZSJ9XX0seyJjb21wb25lbnRfc2V0dGluZ3MiOltdfV0sImNvbmZpZ3VyYXRpb25zIjpbeyJjb3JlLXNpdGUiOnsiZnMudHJhc2guaW50ZXJ2YWwiOiI0MzIwIn19LHsiaGRmcy1zaXRlIjp7ImRmcy5uYW1lbm9kZS5zYWZlbW9kZS50aHJlc2hvbGQtcGN0IjoiMC45OSJ9fSx7ImhpdmUtc2l0ZSI6eyJoaXZlLmV4ZWMuY29tcHJlc3Mub3V0cHV0IjoidHJ1ZSIsImhpdmUubWVyZ2UubWFwZmlsZXMiOiJ0cnVlIiwiaGl2ZS5zZXJ2ZXIyLnRlei5pbml0aWFsaXplLmRlZmF1bHQuc2Vzc2lvbnMiOiJ0cnVlIiwiaGl2ZS5zZXJ2ZXIyLnRyYW5zcG9ydC5tb2RlIjoiaHR0cCJ9fSx7Im1hcHJlZC1zaXRlIjp7Im1hcHJlZHVjZS5qb2IucmVkdWNlLnNsb3dzdGFydC5jb21wbGV0ZWRtYXBzIjoiMC43IiwibWFwcmVkdWNlLm1hcC5vdXRwdXQuY29tcHJlc3MiOiJ0cnVlIiwibWFwcmVkdWNlLm91dHB1dC5maWxlb3V0cHV0Zm9ybWF0LmNvbXByZXNzIjoidHJ1ZSJ9fSx7Inlhcm4tc2l0ZSI6eyJ5YXJuLmFjbC5lbmFibGUiOiJ0cnVlIn19XSwiaG9zdF9ncm91cHMiOlt7Im5hbWUiOiJtYXN0ZXIiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJBUFBfVElNRUxJTkVfU0VSVkVSIn0seyJuYW1lIjoiSENBVCJ9LHsibmFtZSI6IkhERlNfQ0xJRU5UIn0seyJuYW1lIjoiSElTVE9SWVNFUlZFUiJ9LHsibmFtZSI6IkhJVkVfQ0xJRU5UIn0seyJuYW1lIjoiSElWRV9NRVRBU1RPUkUifSx7Im5hbWUiOiJISVZFX1NFUlZFUiJ9LHsibmFtZSI6IkpPVVJOQUxOT0RFIn0seyJuYW1lIjoiTUFQUkVEVUNFMl9DTElFTlQifSx7Im5hbWUiOiJNRVRSSUNTX0NPTExFQ1RPUiJ9LHsibmFtZSI6Ik1FVFJJQ1NfTU9OSVRPUiJ9LHsibmFtZSI6Ik1ZU1FMX1NFUlZFUiJ9LHsibmFtZSI6Ik5BTUVOT0RFIn0seyJuYW1lIjoiUElHIn0seyJuYW1lIjoiUkVTT1VSQ0VNQU5BR0VSIn0seyJuYW1lIjoiU0VDT05EQVJZX05BTUVOT0RFIn0seyJuYW1lIjoiTElWWV9TRVJWRVIifSx7Im5hbWUiOiJTUEFSSzJfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUksyX0pPQkhJU1RPUllTRVJWRVIifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJTUEFSS19KT0JISVNUT1JZU0VSVkVSIn0seyJuYW1lIjoiU1FPT1AifSx7Im5hbWUiOiJURVpfQ0xJRU5UIn0seyJuYW1lIjoiV0VCSENBVF9TRVJWRVIifSx7Im5hbWUiOiJZQVJOX0NMSUVOVCJ9LHsibmFtZSI6IlpFUFBFTElOX01BU1RFUiJ9LHsibmFtZSI6IlpPT0tFRVBFUl9DTElFTlQifSx7Im5hbWUiOiJaT09LRUVQRVJfU0VSVkVSIn1dLCJjYXJkaW5hbGl0eSI6IjEifSx7Im5hbWUiOiJ3b3JrZXIiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IlRFWl9DTElFTlQifSx7Im5hbWUiOiJTUEFSSzJfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUktfQ0xJRU5UIn0seyJuYW1lIjoiREFUQU5PREUifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9LHsibmFtZSI6ImNvbXB1dGUiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IlRFWl9DTElFTlQifSx7Im5hbWUiOiJTUEFSSzJfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUktfQ0xJRU5UIn0seyJuYW1lIjoiTUVUUklDU19NT05JVE9SIn0seyJuYW1lIjoiTk9ERU1BTkFHRVIifV0sImNhcmRpbmFsaXR5IjoiMSsifV19",
		})
	}
	return &blueprints.GetPrivatesBlueprintOK{Payload: resp}, nil
}

func (*mockBlueprintsClient) PostPrivateBlueprint(params *blueprints.PostPrivateBlueprintParams) (*blueprints.PostPrivateBlueprintOK, error) {
	return &blueprints.PostPrivateBlueprintOK{Payload: &models_cloudbreak.BlueprintResponse{ID: int64(1)}}, nil
}

func (*mockBlueprintsClient) PostPublicBlueprint(params *blueprints.PostPublicBlueprintParams) (*blueprints.PostPublicBlueprintOK, error) {
	return &blueprints.PostPublicBlueprintOK{Payload: &models_cloudbreak.BlueprintResponse{ID: int64(1)}}, nil
}

func (*mockBlueprintsClient) DeletePrivateBlueprint(params *blueprints.DeletePrivateBlueprintParams) error {
	return nil
}

func TestListBlueprintsImpl(t *testing.T) {
	var rows []utils.Row

	listBlueprintsImpl(new(mockBlueprintsClient), func(h []string, r []utils.Row) { rows = r })

	if len(rows) != 2 {
		t.Fatalf("row number doesn't match 2 == %d", len(rows))
	}

	for i, r := range rows {
		expected := []string{"name" + strconv.Itoa(i), "desc" + strconv.Itoa(i), "2.6", "3", "USER_MANAGED"}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}
