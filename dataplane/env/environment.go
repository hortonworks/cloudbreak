package env

import (
	"strconv"
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/dataplane/api/client/v3_workspace_id_environments"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
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
	LdapConfigs     []string `json:"LdapConfigs" yaml:"LdapConfigs"`
	ProxyConfigs    []string `json:"ProxyConfigs" yaml:"ProxyConfigs"`
	KerberosConfigs []string `json:"KerberosConfigs" yaml:"KerberosConfigs"`
	RdsConfigs      []string `json:"RdsConfigs" yaml:"RdsConfigs"`
	ID              string   `json:"ID" yaml:"ID"`
}

type environmentClient interface {
	CreateEnvironment(params *v3_workspace_id_environments.CreateEnvironmentParams) (*v3_workspace_id_environments.CreateEnvironmentOK, error)
	ListEnvironment(params *v3_workspace_id_environments.ListEnvironmentParams) (*v3_workspace_id_environments.ListEnvironmentOK, error)
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
	ldapConfigs := utils.DelimitedStringToArray(c.String(fl.FlLdapNamesOptional.Name), ",")
	proxyConfigs := utils.DelimitedStringToArray(c.String(fl.FlProxyNamesOptional.Name), ",")
	kerberosConfigs := utils.DelimitedStringToArray(c.String(fl.FlKerberosNamesOptional.Name), ",")
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlRdsNamesOptional.Name), ",")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	locationName := c.String(fl.FlEnvironmentLocationName.Name)
	longitude := c.Float64(fl.FlEnvironmentLongitudeOptional.Name)
	latitude := c.Float64(fl.FlEnvironmentLatitudeOptional.Name)

	environmentRequest := &model.EnvironmentRequest{
		Name:            &name,
		Description:     &description,
		CredentialName:  credentialName,
		Regions:         regions,
		LdapConfigs:     ldapConfigs,
		ProxyConfigs:    proxyConfigs,
		KerberosConfigs: kerberosConfigs,
		RdsConfigs:      rdsConfigs,
		Location: &model.LocationRequest{
			LocationName: &locationName,
			Longitude:    longitude,
			Latitude:     latitude,
		},
	}

	createEnvironmentImpl(c, workspaceID, environmentRequest)
}

func createEnvironmentImpl(c *cli.Context, workspaceId int64, environmentRequest *model.EnvironmentRequest) {
	log.Infof("[createEnvironmentImpl] create environment with name: %s", *environmentRequest.Name)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	envClient := cbClient.Cloudbreak.V3WorkspaceIDEnvironments
	resp, err := envClient.CreateEnvironment(v3_workspace_id_environments.NewCreateEnvironmentParams().WithWorkspaceID(workspaceId).WithBody(environmentRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload

	log.Infof("[createEnvironmentImpl] environment created with name: %s, id: %d", *environmentRequest.Name, environment.ID)
}

func RegisterCumulusDatalake(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "register cumulus datalake")

	ldapConfig := c.String(fl.FlLdapNameOptional.Name)
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlRdsNamesOptional.Name), ",")
	kerberosConfig := c.String(fl.FlKerberosNameOptional.Name)
	envName := c.String(fl.FlEnvironmentName.Name)
	rangerPassword := c.String(fl.FlRangerAdminPasswordOptional.Name)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)

	registerRequest := model.RegisterDatalakeRequest{
		KerberosName:        kerberosConfig,
		LdapName:            ldapConfig,
		RdsNames:            rdsConfigs,
		RangerAdminPassword: rangerPassword,
	}

	registerCumulusDatalake(c, workspaceID, envName, &registerRequest)
}

func registerCumulusDatalake(c *cli.Context, workspaceId int64, envName string, registerRequest *model.RegisterDatalakeRequest) {
	log.Infof("[registerCumulusDatalake] register cumulus datalake for env: %s", envName)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	envClient := cbClient.Cloudbreak.V3WorkspaceIDEnvironments

	resp, err := envClient.RegisterExternalDatalake(v3_workspace_id_environments.NewRegisterExternalDatalakeParams().
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
	return listEnvironmentsImpl(cbClient.Cloudbreak.V3WorkspaceIDEnvironments, output.WriteList, workspaceID)
}

func listEnvironmentsImpl(envClient environmentClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := envClient.ListEnvironment(v3_workspace_id_environments.NewListEnvironmentParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, e := range resp.Payload {
		row := &environment{
			Name:          e.Name,
			Description:   e.Description,
			CloudPlatform: e.CloudPlatform,
			Credential:    e.CredentialName,
			Regions:       getRegionNames(e.Regions),
			LocationName:  e.Location.LocationName,
			Longitude:     e.Location.Longitude,
			Latitude:      e.Location.Latitude,
		}
		tableRows = append(tableRows, row)
	}

	writer(EnvironmentHeader, tableRows)
	return nil
}

func DescribeEnvironment(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe an environment")

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	log.Infof("[DescribeEnvironment] describe environment by name: %s", envName)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.GetEnvironment(v3_workspace_id_environments.NewGetEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	env := resp.Payload
	if output.Format != "table" {
		output.Write(append(EnvironmentHeader, "Ldaps", "Proxies", "Rds", "ID"), convertResponseToJsonOutput(env))
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
	_, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.DeleteEnvironment(v3_workspace_id_environments.NewDeleteEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func AttachResources(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "attach resources to an environment")
	attachRequest := createAttachRequest(c)
	sendAttachRequest(c, attachRequest)
}

func createAttachRequest(c *cli.Context) *v3_workspace_id_environments.AttachResourcesToEnvironmentParams {
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	ldapConfigs := utils.DelimitedStringToArray(c.String(fl.FlLdapNamesOptional.Name), ",")
	proxyConfigs := utils.DelimitedStringToArray(c.String(fl.FlProxyNamesOptional.Name), ",")
	kerberosConfigs := utils.DelimitedStringToArray(c.String(fl.FlKerberosNamesOptional.Name), ",")
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlRdsNamesOptional.Name), ",")
	log.Infof("[AttachResources] attach resources to environment: %s. Ldaps: [%s] Proxies: [%s] Kerberos: [%s] Rds: [%s]",
		envName, ldapConfigs, proxyConfigs, kerberosConfigs, rdsConfigs)
	attachBody := &model.EnvironmentAttachRequest{
		LdapConfigs:     ldapConfigs,
		ProxyConfigs:    proxyConfigs,
		KerberosConfigs: kerberosConfigs,
		RdsConfigs:      rdsConfigs,
	}
	attachRequest := v3_workspace_id_environments.NewAttachResourcesToEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(attachBody)
	return attachRequest
}

func sendAttachRequest(c *cli.Context, attachRequest *v3_workspace_id_environments.AttachResourcesToEnvironmentParams) {
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	var environment *model.DetailedEnvironmentResponse
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.AttachResourcesToEnvironment(attachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment = resp.Payload
	log.Infof("[AttachResources] resources attached to environment with name: %s, id: %d", environment.Name, environment.ID)
}

func DetachResources(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "detach resources from an environment")
	detachRequest := createDetachRequest(c)
	sendDetachRequest(c, detachRequest)
}

func createDetachRequest(c *cli.Context) *v3_workspace_id_environments.DetachResourcesFromEnvironmentParams {
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	ldapConfigs := utils.DelimitedStringToArray(c.String(fl.FlLdapNamesOptional.Name), ",")
	proxyConfigs := utils.DelimitedStringToArray(c.String(fl.FlProxyNamesOptional.Name), ",")
	kerberosConfigs := utils.DelimitedStringToArray(c.String(fl.FlKerberosNamesOptional.Name), ",")
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlRdsNamesOptional.Name), ",")
	log.Infof("[DetachResources] detach resources from environment: %s. Ldaps: [%s] Proxies: [%s] Kerberos: [%s] Rds: [%s]",
		envName, ldapConfigs, proxyConfigs, kerberosConfigs, rdsConfigs)
	detachBody := &model.EnvironmentDetachRequest{
		LdapConfigs:     ldapConfigs,
		ProxyConfigs:    proxyConfigs,
		KerberosConfigs: kerberosConfigs,
		RdsConfigs:      rdsConfigs,
	}
	attachRequest := v3_workspace_id_environments.NewDetachResourcesFromEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(detachBody)
	return attachRequest
}

func sendDetachRequest(c *cli.Context, detachRequest *v3_workspace_id_environments.DetachResourcesFromEnvironmentParams) {
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.DetachResourcesFromEnvironment(detachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload
	log.Infof("[DetachResources] resources detached to environment with name: %s, id: %d", environment.Name, environment.ID)
}

func ChangeCredential(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "change credential of environment")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	credential := c.String(fl.FlCredential.Name)

	requestBody := &model.EnvironmentChangeCredentialRequest{
		CredentialName: credential,
	}
	request := v3_workspace_id_environments.NewChangeCredentialInEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(requestBody)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.ChangeCredentialInEnvironment(request)
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

	requestBody := &model.EnvironmentEditRequest{
		Description: &description,
		Regions:     regions,
		Location: &model.LocationRequest{
			LocationName: &locationName,
			Longitude:    longitude,
			Latitude:     latitude,
		},
	}
	request := v3_workspace_id_environments.NewEditEnvironmentParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(requestBody)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.EditEnvironment(request)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment := resp.Payload
	log.Infof("[Edit] Environment %s was edited.", environment.Name)
}

func convertResponseToTableOutput(env *model.DetailedEnvironmentResponse) *environmentOutTableDescribe {
	return &environmentOutTableDescribe{
		environment: &environment{
			Name:          env.Name,
			Description:   env.Description,
			CloudPlatform: env.CloudPlatform,
			Credential:    env.CredentialName,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.LocationName,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
		},
		ID: strconv.FormatInt(env.ID, 10),
	}
}

func convertResponseToJsonOutput(env *model.DetailedEnvironmentResponse) *environmentOutJsonDescribe {
	return &environmentOutJsonDescribe{
		environment: &environment{
			Name:          env.Name,
			Description:   env.Description,
			CloudPlatform: env.CloudPlatform,
			Credential:    env.CredentialName,
			Regions:       getRegionNames(env.Regions),
			LocationName:  env.Location.LocationName,
			Longitude:     env.Location.Longitude,
			Latitude:      env.Location.Latitude,
		},
		LdapConfigs:     getLdapConfigNames(env.LdapConfigs),
		ProxyConfigs:    getProxyConfigNames(env.ProxyConfigs),
		KerberosConfigs: getKerberosConfigs(env.KerberosConfigs),
		RdsConfigs:      getRdsConfigNames(env.RdsConfigs),
		ID:              strconv.FormatInt(env.ID, 10),
	}
}

func getRegionNames(region *model.CompactRegionResponse) []string {
	var regions []string
	for _, v := range region.Values {
		regions = append(regions, v)
	}
	return regions
}

func getLdapConfigNames(configs []*model.LdapConfigResponse) []string {
	var names []string
	for _, l := range configs {
		names = append(names, *l.Name)
	}
	return names
}

func getProxyConfigNames(configs []*model.ProxyConfigResponse) []string {
	var names []string
	for _, c := range configs {
		names = append(names, *c.Name)
	}
	return names
}

func getRdsConfigNames(configs []*model.RDSConfigResponse) []string {
	var names []string
	for _, c := range configs {
		names = append(names, *c.Name)
	}
	return names
}

func getKerberosConfigs(configs []*model.KerberosResponse) []string {
	var names []string
	for _, c := range configs {
		names = append(names, c.Name)
	}
	return names
}
