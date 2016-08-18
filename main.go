package main

import (
	"log"
	"fmt"

	"github.com/sequenceiq/hdc-cli/client/blueprints"

	apiclient "github.com/sequenceiq/hdc-cli/client"
)

func main() {

	// make the request to get all items
	resp, err := apiclient.DefaultOAuth2.Blueprints.GetPublics(&blueprints.GetPublicsParams{})
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%#v\n", resp.Payload)
}
