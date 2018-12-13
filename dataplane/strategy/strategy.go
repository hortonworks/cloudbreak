package strategy

import (
	"strconv"
	"time"

	"github.com/go-openapi/strfmt"

	log "github.com/Sirupsen/logrus"
	"github.com/go-openapi/swag"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/strategies"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/strategytypes"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/model"
	"github.com/hortonworks/cb-cli/dataplane/user"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var strategyTypesHeader = []string{"ID", "DisplayName", "Name"}
var strategyHeader = []string{"ID", "Name", "Enabled"}

type strategyTypeOut struct {
	StrategyType *model.StrategyType
}

func (s *strategyTypeOut) DataAsStringArray() []string {
	return []string{
		s.StrategyType.ID.String(),
		swag.StringValue(s.StrategyType.DisplayName),
		swag.StringValue(s.StrategyType.Name)}
}

type strategyOut struct {
	Strategy *model.Strategy
}

func (s *strategyOut) DataAsStringArray() []string {
	return []string{
		s.Strategy.ID.String(),
		swag.StringValue(s.Strategy.Name),
		strconv.FormatBool(swag.BoolValue(s.Strategy.Enabled))}
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

func getStrategyTypeID(client strategyTypesClient, strategyNeeded string) string {
	var strategyID string
	for _, strategy := range getStrategyTypeListImpl(client) {
		strategyType := swag.StringValue(strategy.Name)
		if strategyType == strategyNeeded {
			return strategy.ID.String()
		}
	}
	return strategyID
}

func getStrategyID(client strategyClient, name string) string {
	var strategyID string
	for _, strategy := range listLoginStrategiesImpl(client) {
		strategyName := swag.StringValue(strategy.Name)
		if strategyName == name {
			return strategy.ID.String()
		}
	}
	return strategyID
}

func CreateStrategySAML(c *cli.Context) {
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	config := getStrategyConfigForSAML(c.String(fl.FlCaasStrateyProvider.Name))
	strategyName := getStrategyName(c.String(fl.FlCaasStrategyName.Name))
	var userinfo *model.UserInfo
	userinfo = user.UserInfoImpl(dpClient.Dataplane.Oidc)
	strategyTypeID := strfmt.UUID(getStrategyTypeID(dpClient.Dataplane.Strategytypes, "saml"))
	enabled := true
	strategyRequest := model.Strategy{
		Config:         config,
		TenantID:       userinfo.TenantID,
		StrategyTypeID: &strategyTypeID,
		Name:           &strategyName,
		Enabled:        &enabled,
	}
	resp, err := dpClient.Dataplane.Strategies.CreateStrategy(strategies.NewCreateStrategyParams().WithStrategy(&strategyRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	output.Write(strategyHeader, &strategyOut{resp.Payload})
}

func UpdateStrategySAML(c *cli.Context) {
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	config := getStrategyConfigForSAML(c.String(fl.FlCaasStrateyProvider.Name))
	strategyName := getStrategyName(c.String(fl.FlCaasStrategyName.Name))
	strategyTypeID := strfmt.UUID(getStrategyTypeID(dpClient.Dataplane.Strategytypes, "saml"))
	strategyID := strfmt.UUID(getStrategyID(dpClient.Dataplane.Strategies, strategyName))
	enabled := true
	var userinfo *model.UserInfo
	userinfo = user.UserInfoImpl(dpClient.Dataplane.Oidc)

	strategyRequest := model.Strategy{
		ID:             strategyID,
		Config:         config,
		TenantID:       userinfo.TenantID,
		StrategyTypeID: &strategyTypeID,
		Name:           &strategyName,
		Enabled:        &enabled,
	}
	resp, err := dpClient.Dataplane.Strategies.UpdateStrategy(strategies.NewUpdateStrategyParams().WithStrategy(&strategyRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[UpdateStrategySAML] startegy updated: %s : %d", strategyName, resp.Payload)

}

func ListLoginStrategies(c *cli.Context) {
	log.Infof("[ListUsers] List all strategies")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	strategyResponse := listLoginStrategiesImpl(dpClient.Dataplane.Strategies)
	tableRows := []utils.Row{}
	for _, strategy := range strategyResponse {
		tableRows = append(tableRows, &strategyOut{strategy})
	}
	output.WriteList(strategyHeader, tableRows)
}

func listLoginStrategiesImpl(client strategyClient) []*model.Strategy {
	defer utils.TimeTrack(time.Now(), "List Login Strategies")
	log.Infof("[listUsersImpl] sending list login strategies request")
	resp, err := client.ListStrategies(strategies.NewListStrategiesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func getStrategyConfigForSAML(name string) interface{} {
	log.Infof(name)
	config := map[string]interface{}{
		"saml.identityProviderMetadataPath": name}
	return config
}

func getStrategyName(name string) string {
	// okta0 is the streategy name for saml and it is constant.
	// local0 is the strategy name for local user and it is constant.
	strategyName := "okta0"
	if name == "local" {
		strategyName = "local0"
	}
	return strategyName
}
