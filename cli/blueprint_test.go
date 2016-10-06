package cli

import (
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client/blueprints"
	"github.com/hortonworks/hdc-cli/models"
)

func TestListBlueprintsImplPrefixed(t *testing.T) {
	prints := make([]*models.BlueprintResponse, 0)
	prints = append(prints, &models.BlueprintResponse{Name: "blueprint-name"})
	var rows []Row

	listBlueprintsImpl(func() []*models.BlueprintResponse { return prints }, func(h []string, r []Row) { rows = r })

	if len(rows) != 0 {
		t.Errorf("rows not empty %s", rows)
	}
}

func TestListBlueprintsImplNotPrefixed(t *testing.T) {
	prints := make([]*models.BlueprintResponse, 0)
	for i := 0; i < 3; i++ {
		prints = append(prints, &models.BlueprintResponse{
			Name: "name" + strconv.Itoa(i),
			AmbariBlueprint: models.AmbariBlueprint{
				Blueprint: models.Blueprint{
					Name:         &(&StringWrapper{"blueprint-name"}).s,
					StackVersion: "version" + strconv.Itoa(i),
				},
			},
		})
	}
	var rows []Row

	listBlueprintsImpl(func() []*models.BlueprintResponse { return prints }, func(h []string, r []Row) { rows = r })

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

	blueprint := models.BlueprintResponse{
		ID: &(&StringWrapper{"123"}).s,
	}
	resolver := func(params *blueprints.PostPublicParams) (*blueprints.PostPublicOK, error) {
		return nil, nil
	}

	createBlueprintImpl(skeleton, &blueprint, blueprintId, resolver)

	expected := int64(123)
	actual := <-blueprintId
	if actual != actual {
		t.Errorf("id not maatch %d == %d", expected, actual)
	}
}

func TestCreateBlueprintImplNonDefaultBlueprint(t *testing.T) {
	confs := make([]models.Configurations, 0)
	confs = append(confs, models.Configurations{})
	skeleton := ClusterSkeleton{
		Configurations: confs,
	}
	blueprintId := make(chan int64, 1)

	blueprint := models.BlueprintResponse{
		ID: &(&StringWrapper{"123"}).s,
	}
	expected := int64(321)
	resolver := func(params *blueprints.PostPublicParams) (*blueprints.PostPublicOK, error) {
		return &blueprints.PostPublicOK{Payload: &models.ID{ID: expected}}, nil
	}

	createBlueprintImpl(skeleton, &blueprint, blueprintId, resolver)

	actual := <-blueprintId
	if actual != actual {
		t.Errorf("id not maatch %d == %d", expected, actual)
	}
}

func TestGetBlueprintId(t *testing.T) {
	bp := models.BlueprintResponse{ID: &(&StringWrapper{"123"}).s}

	id := getBlueprintId(&bp)

	expected := int64(123)
	if id != expected {
		t.Errorf("id not match %d == %d", expected, id)
	}
}

func TestGetFancyBlueprintNameIsEmpty(t *testing.T) {
	bp := &models.BlueprintResponse{
		Name: "name",
		AmbariBlueprint: models.AmbariBlueprint{
			Blueprint: models.Blueprint{Name: &(&StringWrapper{""}).s},
		},
	}

	name := getFancyBlueprintName(bp)

	expected := "name"
	if name != expected {
		t.Errorf("name not match %s == %s", expected, name)
	}
}

func TestGetFancyBlueprintNameNotExists(t *testing.T) {
	bp := &models.BlueprintResponse{
		Name: "name",
		AmbariBlueprint: models.AmbariBlueprint{
			Blueprint: models.Blueprint{Name: &(&StringWrapper{"blueprint-name"}).s},
		},
	}

	name := getFancyBlueprintName(bp)

	expected := "name"
	if name != expected {
		t.Errorf("name not match %s == %s", expected, name)
	}
}

func TestGetFancyBlueprintNameExists(t *testing.T) {
	for k, v := range BlueprintMap {
		bp := &models.BlueprintResponse{
			Name: "name",
			AmbariBlueprint: models.AmbariBlueprint{
				Blueprint: models.Blueprint{Name: &k},
			},
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
