package strategy

import (
	"github.com/go-openapi/swag"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/strategies"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/strategytypes"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/model"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var validStrategyTypes = []string{"local", "saml"}

var strategyTypesHeader = []string{"ID", "DisplayName", "Name"}

type strategyTypeOut struct {
	StrategyType *model.StrategyType
}

func (s *strategyTypeOut) DataAsStringArray() []string {
	return []string{
		s.StrategyType.ID.String(),
		swag.StringValue(s.StrategyType.DisplayName),
		swag.StringValue(s.StrategyType.Name)}
}

type strategyClient interface {
	CreateStrategy(params *strategies.CreateStrategyParams) (*strategies.CreateStrategyOK, error)
	ListStrategies(params *strategies.ListStrategiesParams) (*strategies.ListStrategiesOK, error)
	UpdateStrategy(params *strategies.UpdateStrategyParams) (*strategies.UpdateStrategyOK, error)
}

type strategyTypesClient interface {
	GetStrategyTypes(params *strategytypes.GetStrategyTypesParams) (*strategytypes.GetStrategyTypesOK, error)
}

func ListStrategyTypes(c *cli.Context) {
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	tableRows := []utils.Row{}
	for _, strategy := range getStrategyTypeListImpl(dpClient.Dataplane.Strategytypes) {
		tableRows = append(tableRows, &strategyTypeOut{strategy})
	}
	output.WriteList(strategyTypesHeader, tableRows)
}

func getStrategyTypeListImpl(client strategyTypesClient) []*model.StrategyType {
	resp, err := client.GetStrategyTypes(strategytypes.NewGetStrategyTypesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func getStrategyID(client strategyTypesClient, strategyNeeded string) string {
	var strategyID string
	for _, strategy := range getStrategyTypeListImpl(client) {
		strategyType := swag.StringValue(strategy.Name)
		if strategyType == strategyNeeded {
			strategyID = strategy.ID.String()
		}
	}
	return strategyID
}
