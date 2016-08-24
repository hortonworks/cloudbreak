package cli

import (
	"fmt"
	apiclient "github.com/sequenceiq/hdc-cli/client"
	"github.com/sequenceiq/hdc-cli/client/blueprints"
	"github.com/urfave/cli"
	"log"
)

func ListBlueprints(c *cli.Context) error {
	// create the API client
	client := apiclient.NewOAuth2HTTPClient(c.String(FlCBServer.Name), "admin@example.com", "cloudbreak")

	// make the request to get all items
	resp, err := client.Blueprints.GetPublics(&blueprints.GetPublicsParams{})
	if err != nil {
		log.Fatal(err)
	}

	for _, v := range resp.Payload {
		fmt.Printf("%s\n", v.Name)
	}
	return nil

}
