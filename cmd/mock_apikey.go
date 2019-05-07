package cmd

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"

	log "github.com/Sirupsen/logrus"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	"github.com/hortonworks/cb-cli/dataplane/configure"
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
		Flags:  fl.NewFlagBuilder().AddFlags(fl.FlTenantName, fl.FlTenantUser, fl.FlWriteToProfileOptional).AddAuthenticationFlagsWithoutWorkspace().AddOutputFlag().Build(),
		BashComplete: func(c *cli.Context) {
			for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlTenantName, fl.FlTenantUser, fl.FlWriteToProfileOptional).AddAuthenticationFlagsWithoutWorkspace().AddOutputFlag().Build() {
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
	fmt.Println("apikeyid:", myKeys.APIKeyID)
	fmt.Println("privatekey:", myKeys.PrivateKey)
	if c.Bool(fl.FlWriteToProfileOptional.Name) {
		if err = c.Set(fl.FlApiKeyIDOptional.Name, myKeys.APIKeyID); err != nil {
			log.Debug(err)
		}
		if err = c.Set(fl.FlPrivateKeyOptional.Name, myKeys.PrivateKey); err != nil {
			log.Debug(err)
		}
		configure.Configure(c)
	}
}
