package cmd

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"

	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

func init() {
	AppCommands = append(AppCommands, cli.Command{
		Name:   "generate-mock-apikeys",
		Usage:  "Generate mock ums apikey, based on tenant-name and tenant-user",
		Before: cf.CheckConfigAndCommandFlagsDP,
		Action: generateMockApikeys,
		Flags:  fl.NewFlagBuilder().AddFlags(fl.FlServerOptional, fl.FlTenantName, fl.FlTenantUser).Build(),
		BashComplete: func(c *cli.Context) {
			for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlServerOptional, fl.FlTenantName, fl.FlTenantUser).Build() {
				fl.PrintFlagCompletion(f)
			}
		},
		Hidden: true,
	})
}

type keys struct {
	APIKeyID   string `json:"accessKeyId"`
	PrivateKey string `json:"privateKey"`
}

func generateMockApikeys(c *cli.Context) {
	server := c.String(fl.FlServerOptional.Name)
	tenant := c.String(fl.FlTenantName.Name)
	user := c.String(fl.FlTenantUser.Name)
	url := fmt.Sprintf("http://%s/auth/mockkey/%s/%s", server, tenant, user)
	resp, err := http.Get(url)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var myKeys keys
	err = json.Unmarshal(body, &myKeys)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Println("APIKeyID:", myKeys.APIKeyID)
	fmt.Println("PrivateKey:", myKeys.PrivateKey)
}
