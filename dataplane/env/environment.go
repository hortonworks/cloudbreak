package env

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/cloud"

	"github.com/hortonworks/cb-cli/dataplane/api-environment/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"

	"github.com/hortonworks/cb-cli/dataplane/api-environment/client/v1env"
)

var EnvironmentHeader = []string{"Name", "Description", "CloudPlatform", "Status", "Credential", "Regions", "LocationName", "Longitude", "Latitude", "Crn"}

type environment struct {
	Name          string   `json:"Name" yaml:"Name"`
	Description   string   `json:"Description" yaml:"Description"`
	CloudPlatform string   `json:"CloudPlatform" yaml:"CloudPlatform"`
	Status        string   `json:"Status" yaml:"Status"`
	Credential    string   `json:"Credential" yaml:"Credential"`
	Regions       []string `json:"Regions" yaml:"Regions"`
	LocationName  string   `json:"LocationName" yaml:"LocationName"`
	Longitude     float64  `json:"Longitude" yaml:"Longitude"`
	Latitude      float64  `json:"Latitude" yaml:"Latitude"`
	Crn           string   `json:"Crn" yaml:"Crn"`
}

type environmentOutTableDescribe struct {
	*environment
}

type environmentOutJsonDescribe struct {
	*environment
	ProxyConfigs   []string                                  `json:"ProxyConfigs" yaml:"ProxyConfigs"`
	Network        model.EnvironmentNetworkV1Response        `json:"Network" yaml:"Network"`
	Telemetry      model.TelemetryResponse                   `json:"Telemetry" yaml:"Telemetry"`
	Authentication model.EnvironmentAuthenticationV1Response `json:"Authentication" yaml:"Authentication"`
}

type environmentListJsonDescribe struct {
	*environment
	Network   model.EnvironmentNetworkV1Response `json:"Network" yaml:"Network"`
	Telemetry model.TelemetryResponse            `json:"Telemetry" yaml:"Telemetry"`
}

type environmentClient interface {
	CreateEnvironmentV1(params *v1env.CreateEnvironmentV1Params) (*v1env.CreateEnvironmentV1OK, error)
	ListEnvironmentV1(params *v1env.ListEnvironmentV1Params) (*v1env.ListEnvironmentV1OK, error)
}

func (e *environment) DataAsStringArray() []string {
	return []string{e.Name, e.Description, e.CloudPlatform, e.Status, e.Credential, strings.Join(e.Regions, ","), e.LocationName, utils.FloatToString(e.Longitude), utils.FloatToString(e.Latitude), e.Crn}
}

func (e *environmentOutJsonDescribe) DataAsStringArray() []string {
	return append(e.environment.DataAsStringArray())
}

func (e *environmentOutTableDescribe) DataAsStringArray() []string {
	return append(e.environment.DataAsStringArray())
}

func CreateEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "create environment")

	name := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	credentialName := c.String(fl.FlEnvironmentCredential.Name)
	regions := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRegions.Name), ",")
	locationName := c.String(fl.FlEnvironmentLocationName.Name)
	longitude := c.Float64(fl.FlEnvironmentLongitudeOptional.Name)
	latitude := c.Float64(fl.FlEnvironmentLatitudeOptional.Name)

	EnvironmentV1Request := &model.EnvironmentV1Request{
		Name:           &name,
		Description:    &description,
		CredentialName: credentialName,
		Regions:        regions,
		Location: &model.LocationV1Request{
			Name:      &locationName,
			Longitude: longitude,
			Latitude:  latitude,
		},
	}

	createEnvironmentImpl(c, EnvironmentV1Request)
}

func CreateEnvironmentFromTemplate(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "create environment from template")
	fileLocation := c.String(fl.FlEnvironmentTemplateFile.Name)
	log.Infof("[assembleStackTemplate] read environment template JSON from file: %s", fileLocation)
	content := utils.ReadFile(fileLocation)

	var req model.EnvironmentV1Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid JSON format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		utils.LogErrorMessageAndExit(msg)
	}

	if name := c.String(fl.FlNameOptional.Name); len(name) != 0 {
		req.Name = &name
	} else if req.Name == nil {
		utils.LogErrorMessageAndExit("Name of the environment must be set either in the template or with the --name command line option.")
	}
	createEnvironmentImpl(c, &req)
}

func EditEnvironmentFromTemplate(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "edit environment from template")
	fileLocation := c.String(fl.FlEnvironmentTemplateFile.Name)
	log.Infof("[EditEnvironmentFromTemplate] read environment edit template JSON from file: %s", fileLocation)
	content := utils.ReadFile(fileLocation)

	var req model.EnvironmentEditV1Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid JSON format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		utils.LogErrorMessageAndExit(msg)
	}
	var name string
	if name = c.String(fl.FlNameOptional.Name); len(name) == 0 {
		utils.LogErrorMessageAndExit("Name of the environment must be set with the --name command line option.")
	}

	envClient := oauth.NewEnvironmentClientFromContext(c)
	resp, err := envClient.Environment.V1env.EditEnvironmentV1(v1env.NewEditEnvironmentV1Params().WithBody(&req).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload

	log.Infof("[EditEnvironmentFromTemplate] environment has edited with name: %s, crn: %s", environment.Name, environment.Crn)
}

func createEnvironmentImpl(c *cli.Context, EnvironmentV1Request *model.EnvironmentV1Request) {
	log.Infof("[createEnvironmentImpl] create environment with name: %s", *EnvironmentV1Request.Name)

	envClient := oauth.NewEnvironmentClientFromContext(c)
	resp, err := envClient.Environment.V1env.CreateEnvironmentV1(v1env.NewCreateEnvironmentV1Params().WithBody(EnvironmentV1Request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload

	log.Infof("[createEnvironmentImpl] environment created with name: %s, id: %s", *EnvironmentV1Request.Name, environment.Crn)
}

func GenerateAwsEnvironmentTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AWS)
	template := createEnvironmentWithNetwork(getNetworkMode(c), c)
	return printTemplate(template)
}

func GenerateAzureEnvironmentTemplate(c *cli.Context) error {
	cloud.SetProviderType(cloud.AZURE)
	template := createEnvironmentWithNetwork(getNetworkMode(c), c)
	return printTemplate(template)
}

func getNetworkMode(c *cli.Context) cloud.NetworkMode {
	switch c.Command.Names()[0] {
	case "create-new-network":
		return cloud.NEW_NETWORK_NEW_SUBNET
	case "use-existing-network":
		return cloud.EXISTING_NETWORK_EXISTING_SUBNET
	default:
		return cloud.NO_NETWORK
	}
}

func createEnvironmentWithNetwork(mode cloud.NetworkMode, c *cli.Context) model.EnvironmentV1Request {
	provider := cloud.GetProvider()
	template := model.EnvironmentV1Request{
		Name:           new(string),
		Description:    new(string),
		CredentialName: "____",
		Regions:        make([]string, 0),
		Location: &model.LocationV1Request{
			Name:      new(string),
			Longitude: 0,
			Latitude:  0,
		},
		Network: provider.GenerateDefaultNetworkWithParams(c.String, mode),
	}
	if credName := c.String(fl.FlEnvironmentCredentialOptional.Name); len(credName) != 0 {
		template.CredentialName = credName
	}
	if locationName := c.String(fl.FlEnvironmentLocationNameOptional.Name); len(locationName) != 0 {
		template.Location.Name = &locationName
	}
	if regions := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRegions.Name), ","); len(regions) != 0 {
		template.Regions = regions
	}
	return template
}

func printTemplate(template model.EnvironmentV1Request) error {
	resp, err := json.MarshalIndent(template, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
	return nil
}

func ListEnvironments(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list environments")

	envClient := oauth.NewEnvironmentClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	return listEnvironmentsImpl(envClient.Environment.V1env, output)
}

func listEnvironmentsImpl(envClient environmentClient, output utils.Output) error {
	resp, err := envClient.ListEnvironmentV1(v1env.NewListEnvironmentV1Params())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, e := range resp.Payload.Responses {
		row := &environment{
			Name:          e.Name,
			Description:   e.Description,
			CloudPlatform: e.CloudPlatform,
			Status:        e.EnvironmentStatus,
			Credential:    *e.Credential.Name,
			Regions:       getRegionNames(e.Regions),
			LocationName:  e.Location.Name,
			Longitude:     e.Location.Longitude,
			Latitude:      e.Location.Latitude,
			Crn:           e.Crn,
		}

		if output.Format != "table" && output.Format != "yaml" {
			envListJSON := environmentListJsonDescribe{
				environment: row,
			}
			if e.Network != nil {
				envListJSON.Network = *e.Network
			}
			if e.Telemetry != nil {
				envListJSON.Telemetry = *e.Telemetry
			}
			tableRows = append(tableRows, &envListJSON)
		} else {
			tableRows = append(tableRows, row)
		}
	}

	if output.Format != "table" && output.Format != "yaml" {
		output.WriteList(append(EnvironmentHeader, "Network", "Telemetry"), tableRows)
	} else {
		output.WriteList(EnvironmentHeader, tableRows)
	}
	return nil
}

func DescribeEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe an environment")

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	envName := c.String(fl.FlName.Name)
	log.Infof("[DescribeEnvironment] describe environment by name: %s", envName)
	envClient := oauth.NewEnvironmentClientFromContext(c)

	resp, err := envClient.Environment.V1env.GetEnvironmentV1ByName(v1env.NewGetEnvironmentV1ByNameParams().WithName(envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	env := resp.Payload
	if output.Format != "table" && output.Format != "yaml" {
		output.Write(append(EnvironmentHeader, "Network", "Telemetry", "Authentication"), convertResponseToJsonOutput(env))
	} else {
		output.Write(append(EnvironmentHeader), convertResponseToTableOutput(env))
	}
}

func DeleteEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete environment")
	val := c.String(fl.FlNames.Name)
	envNames := strings.Split(val[1:len(val)-1], ",")
	envClient := oauth.NewEnvironmentClientFromContext(c)
	log.Infof("[DeleteEnvironment] delete environment(s) by names: %s", envNames)
	_, err := envClient.Environment.V1env.DeleteEnvironmentsByName(v1env.NewDeleteEnvironmentsByNameParams().WithBody(envNames))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func ChangeCredential(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "change credential of environment")
	envName := c.String(fl.FlName.Name)
	credential := c.String(fl.FlCredential.Name)

	requestBody := &model.EnvironmentChangeCredentialV1Request{
		CredentialName: credential,
	}
	request := v1env.NewChangeCredentialInEnvironmentV1Params().WithName(envName).WithBody(requestBody)
	envClient := oauth.NewEnvironmentClientFromContext(c)
	resp, err := envClient.Environment.V1env.ChangeCredentialInEnvironmentV1(request)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload
	log.Infof("[ChangeCredential] credential of environment %s changed to: %s", environment.Name, *environment.Credential.Name)
}

func EditEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "edit environment")
	envName := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	regions := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRegions.Name), ",")
	locationName := c.String(fl.FlEnvironmentLocationNameOptional.Name)
	longitude := c.Float64(fl.FlEnvironmentLongitudeOptional.Name)
	latitude := c.Float64(fl.FlEnvironmentLatitudeOptional.Name)

	requestBody := &model.EnvironmentEditV1Request{
		Description: &description,
		Regions:     regions,
		Location: &model.LocationV1Request{
			Name:      &locationName,
			Longitude: longitude,
			Latitude:  latitude,
		},
	}
	request := v1env.NewEditEnvironmentV1Params().WithName(envName).WithBody(requestBody)
	envClient := oauth.NewEnvironmentClientFromContext(c)
	resp, err := envClient.Environment.V1env.EditEnvironmentV1(request)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload
	log.Infof("[Edit] Environment %s was edited.", environment.Name)
}

func convertResponseToTableOutput(env *model.DetailedEnvironmentV1Response) *environmentOutTableDescribe {
	return &environmentOutTableDescribe{
		environment: &environment{
			Name:          env.Name,
			Description:   env.Description,
			CloudPlatform: env.CloudPlatform,
			Status:        env.EnvironmentStatus,
			Credential:    *env.Credential.Name,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.Name,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
			Crn:           env.Crn,
		},
	}
}

func convertResponseToJsonOutput(env *model.DetailedEnvironmentV1Response) *environmentOutJsonDescribe {
	result := &environmentOutJsonDescribe{
		environment: &environment{
			Name:          env.Name,
			Description:   env.Description,
			CloudPlatform: env.CloudPlatform,
			Status:        env.EnvironmentStatus,
			Credential:    *env.Credential.Name,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.Name,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
			Crn:           env.Crn,
		},
	}
	if env.Network != nil {
		result.Network = *env.Network
	}
	if env.Telemetry != nil {
		result.Telemetry = *env.Telemetry
	}
	if env.Authentication != nil {
		result.Authentication = *env.Authentication
	}
	return result
}

func getRegionNames(region *model.CompactRegionV1Response) []string {
	var regions []string
	for k, v := range region.DisplayNames {
		regions = append(regions, fmt.Sprintf("%s (%s)", v, k))
	}
	return regions
}

func getProxyConfigNames(configs []*model.ProxyResponse) []string {
	var names []string
	for _, c := range configs {
		names = append(names, *c.Name)
	}
	return names
}
