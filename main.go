package main

import (
	"fmt"
	"log"

	"github.com/sequenceiq/hdc-cli/client/blueprints"

	httptransport "github.com/go-swagger/go-swagger/httpkit/client"
	apiclient "github.com/sequenceiq/hdc-cli/client"
)

func main() {

	// create the API client
	client := apiclient.DefaultOAuth2

	token := apiclient.GetToken("http://192.168.99.100:8089/oauth/authorize", "admin@example.com", "cloudbreak", "cloudbreak_shell")
	bearerTokenAuth := httptransport.BearerToken(token)
	// make the request to get all items
	resp, err := client.Blueprints.GetPublics(&blueprints.GetPublicsParams{}, &bearerTokenAuth)
	if err != nil {
		log.Fatal(err)
	}

	for _, v := range resp.Payload {
		fmt.Printf("%s\n", v.Name)
	}
}
