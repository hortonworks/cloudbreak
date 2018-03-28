package cli

import (
	"strings"
	"testing"

	_ "github.com/hortonworks/cb-cli/cli/cloud/aws"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1rdsconfigs"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type mockRdsClient struct {
}

func (*mockRdsClient) GetPublicsRds(params *v1rdsconfigs.GetPublicsRdsParams) (*v1rdsconfigs.GetPublicsRdsOK, error) {
	resp := []*models_cloudbreak.RDSConfigResponse{
		&models_cloudbreak.RDSConfigResponse{
			Name:             &(&types.S{S: "test"}).S,
			ConnectionURL:    &(&types.S{S: "connectionURL"}).S,
			DatabaseEngine:   &(&types.S{S: "databaseEngine"}).S,
			Type:             &(&types.S{S: "type"}).S,
			ConnectionDriver: nil,
		},
	}
	return &v1rdsconfigs.GetPublicsRdsOK{Payload: resp}, nil
}

func (*mockRdsClient) PostPrivateRds(params *v1rdsconfigs.PostPrivateRdsParams) (*v1rdsconfigs.PostPrivateRdsOK, error) {
	return nil, nil
}

func (*mockRdsClient) PostPublicRds(params *v1rdsconfigs.PostPublicRdsParams) (*v1rdsconfigs.PostPublicRdsOK, error) {
	return nil, nil
}

func (*mockRdsClient) TestRdsConnection(params *v1rdsconfigs.TestRdsConnectionParams) (*v1rdsconfigs.TestRdsConnectionOK, error) {
	return nil, nil
}

func TestListRdsImpl(t *testing.T) {
	var rows []utils.Row
	listAllRdsImpl(new(mockRdsClient), func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "test connectionURL databaseEngine type "
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
