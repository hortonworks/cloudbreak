package cli

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	apiclient "github.com/sequenceiq/hdc-cli/client"
	"github.com/sequenceiq/hdc-cli/client/stacks"
	"github.com/urfave/cli"
)

func ListClusters(c *cli.Context) error {
	client := apiclient.NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))

	resp, err := client.Stacks.GetStacksUser(&stacks.GetStacksUserParams{})
	if err != nil {
		log.Error(err)
	}

	for _, v := range resp.Payload {
		fmt.Printf("%s\n", v.Name)
	}
	return nil

}
