package main

import (
	"log"
	"fmt"

	"github.com/sequenceiq/hdc-cli/client/greeting"

	apiclient "github.com/sequenceiq/hdc-cli/client"
)

func main() {

	name := "TestName"
	gr := greeting.GreetingParams{Name: &name}
	// make the request to get all items
	resp, err := apiclient.Default.Greeting.Greeting(&gr)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%#v\n", *resp.Payload.Content)
}
