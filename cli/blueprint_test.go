package cli

import (
	"testing"

	"github.com/hortonworks/hdc-cli/models"
)

func TestGetFancyBlueprintNameIsEmpty(t *testing.T) {
	bp := &models.BlueprintResponse{
		Name: "name",
		AmbariBlueprint: models.AmbariBlueprint{
			Blueprint: models.Blueprint{Name: &(&sw{""}).s},
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
			Blueprint: models.Blueprint{Name: &(&sw{"blueprint-name"}).s},
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
