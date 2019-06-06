package env

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"

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
	ProxyConfigs []string                           `json:"ProxyConfigs" yaml:"ProxyConfigs"`
	Network      model.EnvironmentNetworkV1Response `json:"Network" yaml:"Network"`
}

type environmentListJsonDescribe struct {
	*environment
	Network model.EnvironmentNetworkV1Response `json:"Network" yaml:"Network"`
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

	log.Infof("[EditEnvironmentFromTemplate] environment has edited with name: %s, crn: %s", environment.Name, environment.ID)
}

func createEnvironmentImpl(c *cli.Context, EnvironmentV1Request *model.EnvironmentV1Request) {
	log.Infof("[createEnvironmentImpl] create environment with name: %s", *EnvironmentV1Request.Name)

	envClient := oauth.NewEnvironmentClientFromContext(c)
	resp, err := envClient.Environment.V1env.CreateEnvironmentV1(v1env.NewCreateEnvironmentV1Params().WithBody(EnvironmentV1Request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload

	log.Infof("[createEnvironmentImpl] environment created with name: %s, id: %s", *EnvironmentV1Request.Name, environment.ID)
}

func GenerateAwsEnvironmentTemplate(c *cli.Context) error {
	template := createEnvironmentWithNetwork(c)
	template.Network.Aws = &model.EnvironmentNetworkAwsV1Params{
		VpcID: new(string),
	}
	return printTemplate(template)
}

func GenerateAzureEnvironmentTemplate(c *cli.Context) error {
	template := createEnvironmentWithNetwork(c)
	template.Network.Azure = &model.EnvironmentNetworkAzureV1Params{
		NetworkID:         new(string),
		NoFirewallRules:   new(bool),
		NoPublicIP:        new(bool),
		ResourceGroupName: new(string),
	}
	return printTemplate(template)
}

func createEnvironmentWithNetwork(c *cli.Context) model.EnvironmentV1Request {
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
		Network: &model.EnvironmentNetworkV1Request{
			SubnetIds: make([]string, 0),
		},
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
			Credential:    e.CredentialName,
			Regions:       getRegionNames(e.Regions),
			LocationName:  e.Location.Name,
			Longitude:     e.Location.Longitude,
			Latitude:      e.Location.Latitude,
			Crn:           e.ID,
		}

		if output.Format != "table" && output.Format != "yaml" && e.Network != nil {
			tableRows = append(tableRows, &environmentListJsonDescribe{
				environment: row,
				Network:     *e.Network,
			})
		} else {
			tableRows = append(tableRows, row)
		}
	}

	if output.Format != "table" && output.Format != "yaml" {
		output.WriteList(append(EnvironmentHeader, "Network"), tableRows)
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

	resp, err := envClient.Environment.V1env.GetEnvironmentV1(v1env.NewGetEnvironmentV1Params().WithName(envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	env := resp.Payload
	fmt.Printf("%+v\n", env)
	fmt.Printf("%+v\n", env.Regions.DisplayNames)
	if output.Format != "table" && output.Format != "yaml" {
		output.Write(append(EnvironmentHeader, "Network"), convertResponseToJsonOutput(env))
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
	_, err := envClient.Environment.V1env.DeleteEnvironments(v1env.NewDeleteEnvironmentsParams().WithBody(envNames))
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
	log.Infof("[ChangeCredential] credential of environment %s changed to: %s", environment.Name, environment.CredentialName)
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
			Credential:    env.CredentialName,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.Name,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
			Crn:           env.ID,
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
			Credential:    env.CredentialName,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.Name,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
			Crn:           env.ID,
		},
	}
	if env.Network != nil {
		result.Network = *env.Network
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
