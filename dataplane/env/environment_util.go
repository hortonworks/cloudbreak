package env

import (
	"fmt"

	"github.com/hortonworks/cb-cli/dataplane/api-environment/client/v1env"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

func GetEnvirontmentCrnByName(c *cli.Context, environment string) string {
	envClient := oauth.Environment(*oauth.NewEnvironmentClientFromContext(c)).Environment
	resp, err := envClient.V1env.GetEnvironmentV1ByName(v1env.NewGetEnvironmentV1ByNameParams().WithName(environment))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environmentDetails := resp.Payload
	if len(environmentDetails.Crn) == 0 {
		errmsg := fmt.Sprintf("Failed to get the CRN of environment: %s", environment)
		utils.LogErrorMessageAndExit(errmsg)
	}
	return environmentDetails.Crn
}
