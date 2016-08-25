package cli

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/blueprints"
	"github.com/urfave/cli"
	"strconv"
)

func ListBlueprints(c *cli.Context) error {
	client := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name)).Cloudbreak

	// make the request to get all items
	resp, err := client.Blueprints.GetPublics(&blueprints.GetPublicsParams{})
	if err != nil {
		log.Error(err)
	}

	for _, v := range resp.Payload {
		fmt.Printf("%s\n", v.Name)
	}
	return nil
}

func GetBlueprintId(name string, client *Cloudbreak) int64 {
	log.Infof("[GetBlueprintId] get blueprint id by name: %s", name)
	resp, err := client.Cloudbreak.Blueprints.GetPrivate(&blueprints.GetPrivateParams{Name: name})

	if err != nil {
		log.Errorf("[CreateCredential] %s", err.Error())
		newExitError()
	}

	id, _ := strconv.Atoi(*resp.Payload.ID)
	id64 := int64(id)
	log.Infof("[GetBlueprintId] '%s' blueprint id: %d", name, id64)
	return id64
}
