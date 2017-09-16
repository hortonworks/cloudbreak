package cli

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client_cloudbreak/rdsconfigs"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func TestListRDSConfigsImpl(t *testing.T) {
	configs := make([]*models_cloudbreak.RDSConfigResponse, 0)
	for i := 0; i < 3; i++ {
		n := strconv.Itoa(i)
		configs = append(configs, &models_cloudbreak.RDSConfigResponse{
			HdpVersion:    &(&stringWrapper{"hdp-version" + n}).s,
			Name:          &(&stringWrapper{"rds-name" + n}).s,
			ConnectionURL: &(&stringWrapper{"jdbc:postgresql://lh:5432/p" + n}).s,
			DatabaseType:  &(&stringWrapper{POSTGRES + n}).s,
			Type:          HIVE_RDS,
		})
	}
	getConfigs := func(params *rdsconfigs.GetPublicsRdsParams) (*rdsconfigs.GetPublicsRdsOK, error) {
		return &rdsconfigs.GetPublicsRdsOK{
			Payload: configs,
		}, nil
	}
	var rows []Row

	listRDSConfigsImpl(getConfigs, func(h []string, r []Row) { rows = r })

	if len(rows) != len(configs) {
		t.Fatalf("row number not match %d == %d", len(configs), len(rows))
	}

	for i, r := range rows {
		n := strconv.Itoa(i)
		expected := []string{"rds-name" + n, rawRdsUrl("jdbc:postgresql://lh:5432/p" + n), POSTGRES + n, "hdp-version" + n, HIVE_RDS}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}

func TestCreateRDSConfigImpl(t *testing.T) {
	finder := func(in string) string {
		switch in {
		case FlRdsName.Name:
			return "name"
		case FlRdsUsername.Name:
			return "username"
		case FlRdsPassword.Name:
			return "password"
		case FlRdsUrl.Name:
			return "jdbc:url"
		case FlRdsDbType.Name:
			return POSTGRES
		case FlHdpVersion.Name:
			return "hdp-version"
		default:
			return ""
		}
	}
	expectedId := int64(1)
	var actual *models_cloudbreak.RDSConfig
	postConfig := func(params *rdsconfigs.PostPublicRdsParams) (*rdsconfigs.PostPublicRdsOK, error) {
		actual = params.Body
		return &rdsconfigs.PostPublicRdsOK{Payload: &models_cloudbreak.RDSConfigResponse{ID: expectedId}}, nil
	}

	createRDSConfigImpl(HIVE_RDS, finder, postConfig)

	if *actual.Name != finder(FlRdsName.Name) {
		t.Errorf("name not match %s == %s", finder(FlRdsName.Name), *actual.Name)
	}
	if *actual.ConnectionUserName != finder(FlRdsUsername.Name) {
		t.Errorf("user name not match %s == %s", finder(FlRdsUsername.Name), *actual.ConnectionUserName)
	}
	if *actual.ConnectionPassword != finder(FlRdsPassword.Name) {
		t.Errorf("password not match %s == %s", finder(FlRdsPassword.Name), *actual.ConnectionPassword)
	}
	if *actual.ConnectionURL != finder(FlRdsUrl.Name) {
		t.Errorf("url not match %s == %s", finder(FlRdsUrl.Name), *actual.ConnectionURL)
	}
	if *actual.DatabaseType != finder(FlRdsDbType.Name) {
		t.Errorf("database type not match %s == %s", finder(FlRdsDbType.Name), *actual.DatabaseType)
	}
	if *actual.Validated != false {
		t.Error("validated not match false")
	}
	if *actual.HdpVersion != finder(FlHdpVersion.Name) {
		t.Errorf("database type not match %s == %s", finder(FlHdpVersion.Name), *actual.HdpVersion)
	}
}

func TestExtendRdsUrlJdbc(t *testing.T) {
	expected := "jdbc:postgresql"
	actual := extendRdsUrl(expected)
	if *actual != expected {
		t.Errorf("url not match %s == %s", expected, *actual)
	}
}

func TestExtendRdsUrlPostgressql(t *testing.T) {
	actual := extendRdsUrl("lh:5432/p")
	expected := "jdbc:postgresql://lh:5432/p"
	if *actual != expected {
		t.Errorf("url not match %s == %s", expected, *actual)
	}
}

func TestRawRdsUtlNotJdbc(t *testing.T) {
	expected := "url"
	actual := rawRdsUrl(expected)
	if actual != expected {
		t.Errorf("url not match %s == %s", expected, actual)
	}
}

func TestRawRdsUtlPostgresql(t *testing.T) {
	actual := rawRdsUrl("jdbc:postgresql://lh:5432/p")
	expected := "lh:5432/p"
	if actual != expected {
		t.Errorf("url not match %s == %s", expected, actual)
	}
}
