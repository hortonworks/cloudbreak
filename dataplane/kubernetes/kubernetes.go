package kubernetes

import (
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"strconv"
	"time"

	"encoding/base64"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/dataplane/api/client/v3_workspace_id_kubernetesconfigs"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
	"strings"
)

var kubernetesHeader = []string{"Name", "Description", "Environments"}

type kubernetes struct {
	Name         string `json:"Name" yaml:"Name"`
	Description  string `json:"Description" yaml:"Description"`
	Environments []string
}

type kubernetesOutDescribe struct {
	*kubernetes
	ID string `json:"ID" yaml:"ID"`
}

func (k *kubernetes) DataAsStringArray() []string {
	return []string{k.Name, k.Description, strings.Join(k.Environments, ",")}
}

func (k *kubernetesOutDescribe) DataAsStringArray() []string {
	return append(k.kubernetes.DataAsStringArray(), k.ID)
}

type kubernetesClient interface {
	ListKubernetesConfigsByWorkspace(params *v3_workspace_id_kubernetesconfigs.ListKubernetesConfigsByWorkspaceParams) (*v3_workspace_id_kubernetesconfigs.ListKubernetesConfigsByWorkspaceOK, error)
	CreateKubernetesConfigInWorkspace(params *v3_workspace_id_kubernetesconfigs.CreateKubernetesConfigInWorkspaceParams) (*v3_workspace_id_kubernetesconfigs.CreateKubernetesConfigInWorkspaceOK, error)
	PutKubernetesConfigInWorkspace(params *v3_workspace_id_kubernetesconfigs.PutKubernetesConfigInWorkspaceParams) (*v3_workspace_id_kubernetesconfigs.PutKubernetesConfigInWorkspaceOK, error)
}

func CreateKubernetes(c *cli.Context) {
	log.Infof("[CreateKubernetes] creating a kubernetes config configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createKubernetesImpl(
		cbClient.Cloudbreak.V3WorkspaceIDKubernetesconfigs,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		base64.StdEncoding.EncodeToString(utils.ReadFile(c.String(fl.FlKubernetesConfigFile.Name))),
		utils.DelimitedStringToArray(c.String(fl.FlEnvironmentsOptional.Name), ","))
}

func createKubernetesImpl(client kubernetesClient, workspaceID int64, name string, description string, configuration string, environments []string) {
	defer utils.TimeTrack(time.Now(), "create kubernetes config")
	config := string(configuration)
	kubernetesRequest := &model.KubernetesConfigRequest{
		Name:         &name,
		Description:  &description,
		Config:       &config,
		Environments: environments,
	}
	var kubernetesResponse *model.KubernetesConfigResponse
	log.Infof("[createKubernetesImpl] sending create kubernetes config request")
	resp, err := client.CreateKubernetesConfigInWorkspace(v3_workspace_id_kubernetesconfigs.NewCreateKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(kubernetesRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kubernetesResponse = resp.Payload

	log.Infof("[createKubernetesImpl] kubernetes created: %s (id: %d)", *kubernetesResponse.Name, kubernetesResponse.ID)
}

func EditKubernetes(c *cli.Context) {
	log.Infof("[EditKubernetes] edit a kubernetes config configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	editKubernetesImpl(
		cbClient.Cloudbreak.V3WorkspaceIDKubernetesconfigs,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		base64.StdEncoding.EncodeToString(utils.ReadFile(c.String(fl.FlKubernetesConfigFile.Name))))
}

func editKubernetesImpl(client kubernetesClient, workspaceID int64, name string, description string, configuration string) {
	defer utils.TimeTrack(time.Now(), "edit kubernetes config")
	config := string(configuration)
	kubernetesRequest := &model.KubernetesConfigRequest{
		Name:        &name,
		Description: &description,
		Config:      &config,
	}
	var kubernetesResponse *model.KubernetesConfigResponse
	log.Infof("[editKubernetesImpl] sending edit kubernetes config request")
	resp, err := client.PutKubernetesConfigInWorkspace(v3_workspace_id_kubernetesconfigs.NewPutKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(kubernetesRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kubernetesResponse = resp.Payload

	log.Infof("[editKubernetesImpl] kubernetes created: %s (id: %d)", *kubernetesResponse.Name, kubernetesResponse.ID)
}

func DeleteKubernetes(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete a kubernetes config configuration")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kubernetesName := c.String(fl.FlName.Name)
	log.Infof("[DeleteKubernetes] delete kubernetes config configuration by name: %s", kubernetesName)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	if _, err := cbClient.Cloudbreak.V3WorkspaceIDKubernetesconfigs.DeleteKubernetesConfigInWorkspace(v3_workspace_id_kubernetesconfigs.NewDeleteKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(kubernetesName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteKubernetes] kubernetes config configuration deleted: %s", kubernetesName)
	return nil
}

func DescribeKubernetes(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe a kubernetes config configuration")
	log.Infof("[DescribeKubernetes] Describes a kubernetes config configuration")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kubernetesName := c.String(fl.FlName.Name)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3WorkspaceIDKubernetesconfigs.GetKubernetesConfigInWorkspace(v3_workspace_id_kubernetesconfigs.NewGetKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(kubernetesName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	r := resp.Payload

	output.Write(append(kubernetesHeader, "ID"), &kubernetesOutDescribe{
		&kubernetes{
			Name:         *r.Name,
			Description:  *r.Description,
			Environments: r.Environments,
		}, strconv.FormatInt(r.ID, 10)})

}

func AttachKubernetesToEnvs(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "attach kubernetes to environments")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kubernetesName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	log.Infof("[AttachKubernetesToEnvs] attach kubernetes config '%s' to environments: %s", kubernetesName, environments)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	attachRequest := v3_workspace_id_kubernetesconfigs.NewAttachKubernetesResourceToEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(kubernetesName).WithBody(environments)
	response, err := cbClient.Cloudbreak.V3WorkspaceIDKubernetesconfigs.AttachKubernetesResourceToEnvironments(attachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kubernetes := response.Payload
	log.Infof("[AttachKubernetesToEnvs] kubernetes config '%s' is now attached to the following environments: %s", *kubernetes.Name, kubernetes.Environments)
}

func DetachKubernetesFromEnvs(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "detach kubernetes from environments")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kubernetesName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	log.Infof("[DetachKubernetesFromEnvs] detach kubernetes config '%s' from environments: %s", kubernetesName, environments)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	detachRequest := v3_workspace_id_kubernetesconfigs.NewDetachKubernetesResourceFromEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(kubernetesName).WithBody(environments)
	response, err := cbClient.Cloudbreak.V3WorkspaceIDKubernetesconfigs.DetachKubernetesResourceFromEnvironments(detachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kubernetes := response.Payload
	log.Infof("[DetachKubernetesFromEnvs] kubernetes config '%s' is now attached to the following environments: %s", *kubernetes.Name, kubernetes.Environments)
}

func ListAllKubernetes(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list kubernetes config configurations")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	return listAllKubernetesImpl(cbClient.Cloudbreak.V3WorkspaceIDKubernetesconfigs, output.WriteList, workspaceID)
}

func listAllKubernetesImpl(kubernetesClient kubernetesClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := kubernetesClient.ListKubernetesConfigsByWorkspace(v3_workspace_id_kubernetesconfigs.NewListKubernetesConfigsByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, r := range resp.Payload {
		row := &kubernetes{
			Name:         *r.Name,
			Description:  utils.SafeStringConvert(r.Description),
			Environments: r.Environments,
		}
		tableRows = append(tableRows, row)
	}

	writer(kubernetesHeader, tableRows)
	return nil
}
