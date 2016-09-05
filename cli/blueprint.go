package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/blueprints"
	"github.com/urfave/cli"
	"strconv"
	"strings"
)

var BlueprintHeader []string = []string{"Cluster Type", "HDP Version"}

type Blueprint struct {
	ClusterType string `json:"ClusterType" yaml:"ClusterType"`
	HDPVersion  string `json:"HDPVersion" yaml:"HDPVersion"`
}

func (b *Blueprint) DataAsStringArray() []string {
	return []string{b.ClusterType, b.HDPVersion}
}

func ListBlueprints(c *cli.Context) error {
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))

	// make the request to get all items
	respBlueprints, err := oAuth2Client.Cloudbreak.Blueprints.GetPublics(&blueprints.GetPublicsParams{})
	if err != nil {
		log.Error(err)
		newExitReturnError()
	}

	var tableRows []Row
	for _, blueprint := range respBlueprints.Payload {
		// this is a workaround, needs to be hidden, by not storing them as public
		if !strings.HasPrefix(blueprint.Name, "b") {
			row := &Blueprint{ClusterType: blueprint.Name, HDPVersion: blueprint.AmbariBlueprint.Blueprint.StackVersion}
			tableRows = append(tableRows, row)
		}
	}
	output := Output{Format: c.String(FlCBOutput.Name)}
	output.WriteList(BlueprintHeader, tableRows)

	return nil
}

func (c *Cloudbreak) GetBlueprintId(name string) int64 {
	log.Infof("[GetBlueprintId] get blueprint id by name: %s", name)
	resp, err := c.Cloudbreak.Blueprints.GetPrivate(&blueprints.GetPrivateParams{Name: name})

	if err != nil {
		log.Errorf("[GetBlueprintId] %s", err.Error())
		newExitReturnError()
	}

	id, _ := strconv.Atoi(*resp.Payload.ID)
	id64 := int64(id)
	log.Infof("[GetBlueprintId] '%s' blueprint id: %d", name, id64)
	return id64
}
