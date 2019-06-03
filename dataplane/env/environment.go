package env

import (
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	v4env "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_environments"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

var EnvironmentHeader = []string{"Name", "Description", "CloudPlatform", "Credential", "Regions", "LocationName", "Longitude", "Latitude"}

type environment struct {
	Name          string   `json:"Name" yaml:"Name"`
	Description   string   `json:"Description" yaml:"Description"`
	CloudPlatform string   `json:"CloudPlatform" yaml:"CloudPlatform"`
	Credential    string   `json:"Credential" yaml:"Credential"`
	Regions       []string `json:"Regions" yaml:"Regions"`
	LocationName  string   `json:"LocationName" yaml:"LocationName"`
	Longitude     float64  `json:"Longitude" yaml:"Longitude"`
	Latitude      float64  `json:"Latitude" yaml:"Latitude"`
}

type environmentOutTableDescribe struct {
	*environment
	ID string `json:"ID" yaml:"ID"`
}

type environmentOutJsonDescribe struct {
	*environment
	LdapConfigs     []string                           `json:"LdapConfigs" yaml:"LdapConfigs"`
	ProxyConfigs    []string                           `json:"ProxyConfigs" yaml:"ProxyConfigs"`
	KerberosConfigs []string                           `json:"KerberosConfigs" yaml:"KerberosConfigs"`
	RdsConfigs      []string                           `json:"RdsConfigs" yaml:"RdsConfigs"`
	ID              string                             `json:"ID" yaml:"ID"`
	Network         model.EnvironmentNetworkV4Response `json:"Network" yaml:"Network"`
}

type environmentListJsonDescribe struct {
	*environment
	Network model.EnvironmentNetworkV4Response `json:"Network" yaml:"Network"`
}

type environmentClient interface {
	CreateEnvironment(params *v4env.CreateEnvironmentParams) (*v4env.CreateEnvironmentOK, error)
	ListEnvironment(params *v4env.ListEnvironmentParams) (*v4env.ListEnvironmentOK, error)
}

func (e *environment) DataAsStringArray() []string {
	return []string{e.Name, e.Description, e.CloudPlatform, e.Credential, strings.Join(e.Regions, ","), e.LocationName, utils.FloatToString(e.Longitude), utils.FloatToString(e.Latitude)}
}

func (e *environmentOutJsonDescribe) DataAsStringArray() []string {
	return append(e.environment.DataAsStringArray(), e.ID)
}

func (e *environmentOutTableDescribe) DataAsStringArray() []string {
	return append(e.environment.DataAsStringArray(), e.ID)
}

func CreateEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "create environment")

	name := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	credentialName := c.String(fl.FlEnvironmentCredential.Name)
	regions := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRegions.Name), ",")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	locationName := c.String(fl.FlEnvironmentLocationName.Name)
	longitude := c.Float64(fl.FlEnvironmentLongitudeOptional.Name)
	latitude := c.Float64(fl.FlEnvironmentLatitudeOptional.Name)

	EnvironmentV4Request := &model.EnvironmentV4Request{
		Name:           &name,
		Description:    &description,
		CredentialName: credentialName,
		Regions:        regions,
		Location: &model.LocationV4Request{
			Name:      &locationName,
			Longitude: longitude,
			Latitude:  latitude,
		},
	}

	createEnvironmentImpl(c, workspaceID, EnvironmentV4Request)
}

func CreateEnvironmentFromTemplate(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "create environment from template")
	fileLocation := c.String(fl.FlEnvironmentTemplateFile.Name)
	log.Infof("[assembleStackTemplate] read environment template JSON from file: %s", fileLocation)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	content := utils.ReadFile(fileLocation)

	var req model.EnvironmentV4Request
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
	createEnvironmentImpl(c, workspaceID, &req)
}

func createEnvironmentImpl(c *cli.Context, workspaceId int64, EnvironmentV4Request *model.EnvironmentV4Request) {
	log.Infof("[createEnvironmentImpl] create environment with name: %s", *EnvironmentV4Request.Name)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	envClient := cbClient.Cloudbreak.V4WorkspaceIDEnvironments
	resp, err := envClient.CreateEnvironment(v4env.NewCreateEnvironmentParams().WithWorkspaceID(workspaceId).WithBody(EnvironmentV4Request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload

	log.Infof("[createEnvironmentImpl] environment created with name: %s, id: %d", *EnvironmentV4Request.Name, environment.ID)
}

func GenerateAwsEnvironmentTemplate(c *cli.Context) error {
	template := createEnvironmentWithNetwork(c)
	template.Network.Aws = &model.EnvironmentNetworkAwsV4Params{
		VpcID: new(string),
	}
	return printTemplate(template)
}

func GenerateAzureEnvironmentTemplate(c *cli.Context) error {
	template := createEnvironmentWithNetwork(c)
	template.Network.Azure = &model.EnvironmentNetworkAzureV4Params{
		ResourceGroupName: new(string),
		NetworkID:         new(string),
		NoFirewallRules:   false,
		NoPublicIP:        false,
	}
	return printTemplate(template)
}

func createEnvironmentWithNetwork(c *cli.Context) model.EnvironmentV4Request {
	template := model.EnvironmentV4Request{
		Name:           new(string),
		Description:    new(string),
		CredentialName: "____",
		Regions:        make([]string, 0),
		Kubernetes:     make([]string, 0),
		Location: &model.LocationV4Request{
			Name:      new(string),
			Longitude: 0,
			Latitude:  0,
		},
		Network: &model.EnvironmentNetworkV4Request{
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

func printTemplate(template model.EnvironmentV4Request) error {
	resp, err := json.MarshalIndent(template, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(resp))
	return nil
}

func RegisterCumulusDatalake(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "register cumulus datalake")

	ldapConfig := c.String(fl.FlLdapNameOptional.Name)
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlRdsNamesOptional.Name), ",")
	kerberosConfig := c.String(fl.FlKerberosNameOptional.Name)
	envName := c.String(fl.FlEnvironmentName.Name)
	rangerPassword := c.String(fl.FlRangerAdminPasswordOptional.Name)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)

	registerRequest := model.RegisterDatalakeV4Request{
		KerberosName:        kerberosConfig,
		LdapName:            ldapConfig,
		DatabaseNames:       rdsConfigs,
		RangerAdminPassword: rangerPassword,
	}

	registerCumulusDatalake(c, workspaceID, envName, &registerRequest)
}

func registerCumulusDatalake(c *cli.Context, workspaceId int64, envName string, registerRequest *model.RegisterDatalakeV4Request) {
	log.Infof("[registerCumulusDatalake] register cumulus datalake for env: %s", envName)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	envClient := cbClient.Cloudbreak.V4WorkspaceIDEnvironments

	resp, err := envClient.RegisterExternalDatalake(v4env.NewRegisterExternalDatalakeParams().
		WithWorkspaceID(workspaceId).
		WithName(envName).
		WithBody(registerRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[registerCumulusDatalake] datalake registered with id: %d", resp.Payload.ID)
}

func ListEnvironments(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list environments")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	return listEnvironmentsImpl(cbClient.Cloudbreak.V4WorkspaceIDEnvironments, output, workspaceID)
}

func listEnvironmentsImpl(envClient environmentClient, output utils.Output, workspaceID int64) error {
	resp, err := envClient.ListEnvironment(v4env.NewListEnvironmentParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, e := range resp.Payload.Responses {
		row := &environment{
			Name:          e.Name,
			Description:   e.Description,
			CloudPlatform: e.CloudPlatform,
			Credential:    e.CredentialName,
			Regions:       getRegionNames(e.Regions),
			LocationName:  e.Location.Name,
			Longitude:     e.Location.Longitude,
			Latitude:      e.Location.Latitude,
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
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	log.Infof("[DescribeEnvironment] describe environment by name: %s", envName)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V4WorkspaceIDEnvironments.GetEnvironment(v4env.NewGetEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	env := resp.Payload
	if output.Format != "table" && output.Format != "yaml" {
		output.Write(append(EnvironmentHeader, "Ldaps", "Proxies", "Databases", "ID", "Network"), convertResponseToJsonOutput(env))
	} else {
		output.Write(append(EnvironmentHeader, "ID"), convertResponseToTableOutput(env))
	}
}

func DeleteEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete an environment")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	log.Infof("[DeleteEnvironment] delete environment by name: %s", envName)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	_, err := cbClient.Cloudbreak.V4WorkspaceIDEnvironments.DeleteEnvironment(v4env.NewDeleteEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func ChangeCredential(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "change credential of environment")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	credential := c.String(fl.FlCredential.Name)

	requestBody := &model.EnvironmentChangeCredentialV4Request{
		CredentialName: credential,
	}
	request := v4env.NewChangeCredentialInEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(requestBody)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	resp, err := cbClient.Cloudbreak.V4WorkspaceIDEnvironments.ChangeCredentialInEnvironment(request)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload
	log.Infof("[ChangeCredential] credential of environment %s changed to: %s", environment.Name, environment.CredentialName)
}

func EditEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "edit environment")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	regions := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRegions.Name), ",")
	locationName := c.String(fl.FlEnvironmentLocationNameOptional.Name)
	longitude := c.Float64(fl.FlEnvironmentLongitudeOptional.Name)
	latitude := c.Float64(fl.FlEnvironmentLatitudeOptional.Name)

	requestBody := &model.EnvironmentEditV4Request{
		Description: &description,
		Regions:     regions,
		Location: &model.LocationV4Request{
			Name:      &locationName,
			Longitude: longitude,
			Latitude:  latitude,
		},
	}
	request := v4env.NewEditEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(requestBody)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	resp, err := cbClient.Cloudbreak.V4WorkspaceIDEnvironments.EditEnvironment(request)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload
	log.Infof("[Edit] Environment %s was edited.", environment.Name)
}

func convertResponseToTableOutput(env *model.DetailedEnvironmentV4Response) *environmentOutTableDescribe {
	return &environmentOutTableDescribe{
		environment: &environment{
			Name:          env.Name,
			Description:   env.Description,
			CloudPlatform: env.CloudPlatform,
			Credential:    env.CredentialName,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.Name,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
		},
		ID: strconv.FormatInt(env.ID, 10),
	}
}

func convertResponseToJsonOutput(env *model.DetailedEnvironmentV4Response) *environmentOutJsonDescribe {
	result := &environmentOutJsonDescribe{
		environment: &environment{
			Name:          env.Name,
			Description:   env.Description,
			CloudPlatform: env.CloudPlatform,
			Credential:    env.CredentialName,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.Name,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
		},
		ID: strconv.FormatInt(env.ID, 10),
	}
	if env.Network != nil {
		result.Network = *env.Network
	}
	return result
}

func getRegionNames(region *model.CompactRegionV4Response) []string {
	var regions []string
	for _, v := range region.Values {
		regions = append(regions, v)
	}
	return regions
}

func getLdapConfigNames(configs []*model.LdapV4Response) []string {
	var names []string
	for _, l := range configs {
		names = append(names, *l.Name)
	}
	return names
}

func getProxyConfigNames(configs []*model.ProxyV4Response) []string {
	var names []string
	for _, c := range configs {
		names = append(names, *c.Name)
	}
	return names
}

func getRdsConfigNames(configs []*model.DatabaseV4Response) []string {
	var names []string
	for _, c := range configs {
		names = append(names, *c.Name)
	}
	return names
}

func getKerberosConfigs(configs []*model.KerberosV4Response) []string {
	var names []string
	for _, c := range configs {
		names = append(names, c.Name)
	}
	return names
}
