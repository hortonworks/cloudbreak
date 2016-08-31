package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/blueprints"
	"github.com/urfave/cli"
	"strconv"
)

func ListBlueprints(c *cli.Context) error {
	client := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name)).Cloudbreak

	// make the request to get all items
	respBlueprints, err := client.Blueprints.GetPublics(&blueprints.GetPublicsParams{})
	if err != nil {
		log.Error(err)
		newExitReturnError()
	}

	var tableRows []TableRow
	for _, blueprint := range respBlueprints.Payload {
		row := &GenericRow{Data: []string{blueprint.Name}}
		tableRows = append(tableRows, row)
	}
	WriteTable([]string{"ClusterType"}, tableRows)

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
