package cli

import (
	"fmt"
	apiclient "github.com/sequenceiq/hdc-cli/client"
	"github.com/sequenceiq/hdc-cli/client/blueprints"
	"github.com/urfave/cli"
	"log"
)

func Configure(c *cli.Context) error {
	if c.NumFlags() != 3 || len(c.String(FlCBUsername.Name)) == 0 || len(c.String(FlCBPassword.Name)) == 0 || len(c.String(FlCBServer.Name)) == 0 {
		return cli.NewExitError(fmt.Sprintf("You need to specify all the parameters. See '%s configure --help'.", c.App.Name), 1)
	}

	log.Println("Save configureation to file...")
	return nil
}

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
