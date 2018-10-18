package env

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v3_workspace_id_environments"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/oauth"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/urfave/cli"
	"strconv"
	"strings"
	"time"
)

var EnvironmentHeader = []string{"Name", "Description", "CloudPlatform", "Regions"}

type environment struct {
	Name          string   `json:"Name" yaml:"Name"`
	Description   string   `json:"Description" yaml:"Description"`
	CloudPlatform string   `json:"CloudPlatform" yaml:"CloudPlatform"`
	Regions       []string `json:"Regions" yaml:"Regions"`
}

type environmentOutTableDescribe struct {
	*environment
	ID string `json:"ID" yaml:"ID"`
}

type environmentOutJsonDescribe struct {
	*environment
	LdapConfigs  []string `json:"LdapConfigs" yaml:"LdapConfigs"`
	ProxyConfigs []string `json:"ProxyConfigs" yaml:"ProxyConfigs"`
	RdsConfigs   []string `json:"RdsConfigs" yaml:"RdsConfigs"`
	ID           string   `json:"ID" yaml:"ID"`
}

type environmentClient interface {
	Create(params *v3_workspace_id_environments.CreateParams) (*v3_workspace_id_environments.CreateOK, error)
	List(params *v3_workspace_id_environments.ListParams) (*v3_workspace_id_environments.ListOK, error)
}

func (e *environment) DataAsStringArray() []string {
	return []string{e.Name, e.Description, e.CloudPlatform, strings.Join(e.Regions, ",")}
}

func (e *environmentOutJsonDescribe) DataAsStringArray() []string {
	return append(e.environment.DataAsStringArray(), e.ID)
}

func (e *environmentOutTableDescribe) DataAsStringArray() []string {
	return append(e.environment.DataAsStringArray(), e.ID)
}

func CreateEnvironment(c *cli.Context) {
	name := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	credentialName := c.String(fl.FlEnvironmentCredential.Name)
	regions := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRegions.Name), ",")
	ldapConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentLdapsOptional.Name), ",")
	proxyConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentProxiesOptional.Name), ",")
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRdsesOptional.Name), ",")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createEnvironmentImpl(cbClient.Cloudbreak.V3WorkspaceIDEnvironments, workspaceID, name, description, credentialName, regions, ldapConfigs, proxyConfigs, rdsConfigs)
}

func createEnvironmentImpl(envClient environmentClient, workspaceID int64, name string, description string, credentialName string, regions []string, ldapConfigs []string, proxyConfigs []string, rdsConfigs []string) {
	defer utils.TimeTrack(time.Now(), "create environment")

	environmentRequest := &model.EnvironmentRequest{
		Name:           &name,
		Description:    &description,
		CredentialName: credentialName,
		Regions:        regions,
		LdapConfigs:    ldapConfigs,
		ProxyConfigs:   proxyConfigs,
		RdsConfigs:     rdsConfigs,
	}

	log.Infof("[createEnvironmentImpl] create environment with name: %s", name)
	var environment *model.DetailedEnvironmentResponse
	resp, err := envClient.Create(v3_workspace_id_environments.NewCreateParams().WithWorkspaceID(workspaceID).WithBody(environmentRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment = resp.Payload

	log.Infof("[createEnvironmentImpl] environment created with name: %s, id: %d", name, environment.ID)
}

func ListEnvironments(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list environments")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	return listEnvironmentsImpl(cbClient.Cloudbreak.V3WorkspaceIDEnvironments, output.WriteList, workspaceID)
}

func listEnvironmentsImpl(envClient environmentClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := envClient.List(v3_workspace_id_environments.NewListParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, e := range resp.Payload {
		row := &environment{
			Name:          e.Name,
			Description:   e.Description,
			CloudPlatform: e.CloudPlatform,
			Regions:       e.Regions,
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
	log.Infof("[DescribeEnvironment] describe environment config by name: %s", envName)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.Get(v3_workspace_id_environments.NewGetParams().WithWorkspaceID(workspaceID).WithName(envName))
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

func AttachResources(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "attach resources to an environment")
	attachRequest := createAttachRequest(c)
	sendAttachRequest(c, attachRequest)
}

func createAttachRequest(c *cli.Context) *v3_workspace_id_environments.AttachResourcesParams {
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	ldapConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentLdapsOptional.Name), ",")
	proxyConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentProxiesOptional.Name), ",")
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRdsesOptional.Name), ",")
	log.Infof("[AttachToEnvironment] attach resources to environment: %s. Ldaps: [%s] Proxies: [%s] Rds: [%s]",
		envName, ldapConfigs, proxyConfigs, rdsConfigs)
	attachBody := &model.EnvironmentAttachRequest{
		LdapConfigs:  ldapConfigs,
		ProxyConfigs: proxyConfigs,
		RdsConfigs:   rdsConfigs,
	}
	attachRequest := v3_workspace_id_environments.NewAttachResourcesParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(attachBody)
	return attachRequest
}

func sendAttachRequest(c *cli.Context, attachRequest *v3_workspace_id_environments.AttachResourcesParams) {
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	var environment *model.DetailedEnvironmentResponse
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.AttachResources(attachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment = resp.Payload
	log.Infof("[sendAttachRequest] resources attached to environment with name: %s, id: %d", environment.Name, environment.ID)
}

func DetachResources(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "attach resources to an environment")
	detachRequest := createDetachRequest(c)
	sendDetachRequest(c, detachRequest)
}

func createDetachRequest(c *cli.Context) *v3_workspace_id_environments.DetachResourcesParams {
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	envName := c.String(fl.FlName.Name)
	ldapConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentLdapsOptional.Name), ",")
	proxyConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentProxiesOptional.Name), ",")
	rdsConfigs := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentRdsesOptional.Name), ",")
	log.Infof("[AttachToEnvironment] attach resources to environment: %s. Ldaps: [%s] Proxies: [%s] Rds: [%s]",
		envName, ldapConfigs, proxyConfigs, rdsConfigs)
	detachBody := &model.EnvironmentDetachRequest{
		LdapConfigs:  ldapConfigs,
		ProxyConfigs: proxyConfigs,
		RdsConfigs:   rdsConfigs,
	}
	attachRequest := v3_workspace_id_environments.NewDetachResourcesParams().WithWorkspaceID(workspaceID).WithName(envName).WithBody(detachBody)
	return attachRequest
}

func sendDetachRequest(c *cli.Context, detachRequest *v3_workspace_id_environments.DetachResourcesParams) {
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	var environment *model.DetailedEnvironmentResponse
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDEnvironments.DetachResources(detachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	environment = resp.Payload
	log.Infof("[sendDetachRequest] resources detached to environment with name: %s, id: %d", environment.Name, environment.ID)
}

func convertResponseToTableOutput(env *model.DetailedEnvironmentResponse) *environmentOutTableDescribe {
	return &environmentOutTableDescribe{
		environment: &environment{
			Name:          env.Name,
			Description:   env.Description,
			CloudPlatform: env.CloudPlatform,
			Regions:       env.Regions,
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
			Regions:       env.Regions,
		},
		LdapConfigs:  env.LdapConfigs,
		ProxyConfigs: env.ProxyConfigs,
		RdsConfigs:   env.RdsConfigs,
		ID:           strconv.FormatInt(env.ID, 10),
	}
}
