package main

import (
	"fmt"
	"log"

	"github.com/sequenceiq/hdc-cli/client/blueprints"

	apiclient "github.com/sequenceiq/hdc-cli/client"
)

func main() {

	// create the API client
	client := apiclient.DefaultOAuth2

	// make the request to get all items
	resp, err := client.Blueprints.GetPublics(&blueprints.GetPublicsParams{})
	if err != nil {
		log.Fatal(err)
	}

	for _, v := range resp.Payload {
		fmt.Printf("%s\n", v.Name)
	}
}
