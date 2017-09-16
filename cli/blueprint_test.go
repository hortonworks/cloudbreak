package cli

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client_cloudbreak/blueprints"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func TestListBlueprintsImplPrefixed(t *testing.T) {
	prints := make([]*models_cloudbreak.BlueprintResponse, 0)
	prints = append(prints, &models_cloudbreak.BlueprintResponse{Name: &(&stringWrapper{"blueprint-name"}).s})
	var rows []Row

	listBlueprintsImpl(func() []*models_cloudbreak.BlueprintResponse { return prints }, func(h []string, r []Row) { rows = r })

	if len(rows) != 0 {
		t.Errorf("rows not empty %s", rows)
	}
}

func TestListBlueprintsImplNotPrefixed(t *testing.T) {
	prints := make([]*models_cloudbreak.BlueprintResponse, 0)
	for i := 0; i < 3; i++ {
		prints = append(prints, &models_cloudbreak.BlueprintResponse{
			Name:            &(&stringWrapper{"name" + strconv.Itoa(i)}).s,
			AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\": \"blueprint-name\", \"stack_version\": \"" + "version" + strconv.Itoa(i) + "\"}}",
		})
	}
	var rows []Row

	listBlueprintsImpl(func() []*models_cloudbreak.BlueprintResponse { return prints }, func(h []string, r []Row) { rows = r })

	if len(rows) != len(prints) {
		t.Fatalf("row number not match %d == %d", len(prints), len(rows))
	}

	for i, r := range rows {
		expected := []string{"name" + strconv.Itoa(i), "version" + strconv.Itoa(i)}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}

func TestCreateBlueprintImplDefaultBlueprint(t *testing.T) {
	skeleton := ClusterSkeleton{}
	blueprintId := make(chan int64, 1)

	id := int64(123)
	blueprint := models_cloudbreak.BlueprintResponse{
		ID:              id,
		AmbariBlueprint: "",
	}
	resolver := func(params *blueprints.PostPublicBlueprintParams) (*blueprints.PostPublicBlueprintOK, error) {
		return nil, nil
	}

	createBlueprintImpl(skeleton, &blueprint, blueprintId, resolver)

	actual := <-blueprintId
	if actual != actual {
		t.Errorf("id not maatch %d == %d", id, actual)
	}
}

func TestCreateBlueprintImplNonDefaultBlueprint(t *testing.T) {
	confs := make([]Configurations, 0)
	confs = append(confs, Configurations{})
	skeleton := ClusterSkeleton{
		Configurations: confs,
	}
	blueprintId := make(chan int64, 1)
	id := int64(123)
	blueprint := models_cloudbreak.BlueprintResponse{
		ID:              id,
		AmbariBlueprint: "",
	}
	expected := int64(321)
	resolver := func(params *blueprints.PostPublicBlueprintParams) (*blueprints.PostPublicBlueprintOK, error) {
		return &blueprints.PostPublicBlueprintOK{Payload: &models_cloudbreak.BlueprintResponse{ID: expected}}, nil
	}

	createBlueprintImpl(skeleton, &blueprint, blueprintId, resolver)

	actual := <-blueprintId
	if actual != actual {
		t.Errorf("id not maatch %d == %d", expected, actual)
	}
}

func TestGetFancyBlueprintNameIsEmpty(t *testing.T) {
	bp := &models_cloudbreak.BlueprintResponse{
		Name:            &(&stringWrapper{"name"}).s,
		AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\": \"\"}}",
	}

	name := getFancyBlueprintName(bp)

	expected := "name"
	if name != expected {
		t.Errorf("name not match %s == %s", expected, name)
	}
}

func TestGetFancyBlueprintNameNotExists(t *testing.T) {
	bp := &models_cloudbreak.BlueprintResponse{
		Name:            &(&stringWrapper{"name"}).s,
		AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\": \"blueprint-name\"}}",
	}

	name := getFancyBlueprintName(bp)

	expected := "name"
	if name != expected {
		t.Errorf("name not match %s == %s", expected, name)
	}
}

func TestGetFancyBlueprintNameExists(t *testing.T) {
	for k, v := range BlueprintMap {
		bp := &models_cloudbreak.BlueprintResponse{
			Name:            &(&stringWrapper{"name"}).s,
			AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\": \"" + k + "\"}}",
		}

		name := getFancyBlueprintName(bp)

		if name != v {
			t.Errorf("name not match %s == %s", v, name)
		}
	}
}

func TestGetRealBlueprintNameNotExists(t *testing.T) {
	expected := "name"
	name := getRealBlueprintName(expected)

	if name != expected {
		t.Errorf("name not match %s == %s", expected, name)
	}
}

func TestGetRealBlueprintNameExists(t *testing.T) {
	for k, v := range BlueprintMap {
		name := getRealBlueprintName(k)

		if name != v {
			t.Errorf("name not match %s == %s", v, name)
		}
	}
}
