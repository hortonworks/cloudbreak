package kubernetes

import (
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"encoding/base64"

	"strings"

	v4kube "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_kubernetes"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
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
	ListKubernetesConfigsByWorkspace(params *v4kube.ListKubernetesConfigsByWorkspaceParams) (*v4kube.ListKubernetesConfigsByWorkspaceOK, error)
	CreateKubernetesConfigInWorkspace(params *v4kube.CreateKubernetesConfigInWorkspaceParams) (*v4kube.CreateKubernetesConfigInWorkspaceOK, error)
	PutKubernetesConfigInWorkspace(params *v4kube.PutKubernetesConfigInWorkspaceParams) (*v4kube.PutKubernetesConfigInWorkspaceOK, error)
}

func CreateKubernetes(c *cli.Context) {
	log.Infof("[CreateKubernetes] creating a kubernetes config configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createKubernetesImpl(
		cbClient.Cloudbreak.V4WorkspaceIDKubernetes,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		base64.StdEncoding.EncodeToString(utils.ReadFile(c.String(fl.FlKubernetesConfigFile.Name))),
		utils.DelimitedStringToArray(c.String(fl.FlEnvironmentsOptional.Name), ","))
}

func createKubernetesImpl(client kubernetesClient, workspaceID int64, name string, description string, configuration string, environments []string) {
	defer utils.TimeTrack(time.Now(), "create kubernetes config")
	config := string(configuration)
	kubernetesRequest := &model.KubernetesV4Request{
		Name:        &name,
		Description: &description,
		Content:     &config,
	}
	var kubernetesResponse *model.KubernetesV4Response
	log.Infof("[createKubernetesImpl] sending create kubernetes config request")
	resp, err := client.CreateKubernetesConfigInWorkspace(v4kube.NewCreateKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(kubernetesRequest))
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
		cbClient.Cloudbreak.V4WorkspaceIDKubernetes,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		base64.StdEncoding.EncodeToString(utils.ReadFile(c.String(fl.FlKubernetesConfigFile.Name))))
}

func editKubernetesImpl(client kubernetesClient, workspaceID int64, name string, description string, configuration string) {
	defer utils.TimeTrack(time.Now(), "edit kubernetes config")
	config := string(configuration)
	kubernetesRequest := &model.KubernetesV4Request{
		Name:        &name,
		Description: &description,
		Content:     &config,
	}
	var kubernetesResponse *model.KubernetesV4Response
	log.Infof("[editKubernetesImpl] sending edit kubernetes config request")
	resp, err := client.PutKubernetesConfigInWorkspace(v4kube.NewPutKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(kubernetesRequest))
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

	if _, err := cbClient.Cloudbreak.V4WorkspaceIDKubernetes.DeleteKubernetesConfigInWorkspace(v4kube.NewDeleteKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(kubernetesName)); err != nil {
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

	resp, err := cbClient.Cloudbreak.V4WorkspaceIDKubernetes.GetKubernetesConfigInWorkspace(v4kube.NewGetKubernetesConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(kubernetesName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	r := resp.Payload

	output.Write(append(kubernetesHeader, "ID"), &kubernetesOutDescribe{
		&kubernetes{
			Name:        *r.Name,
			Description: *r.Description,
		}, strconv.FormatInt(r.ID, 10)})

}

func ListAllKubernetes(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list kubernetes config configurations")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	return listAllKubernetesImpl(cbClient.Cloudbreak.V4WorkspaceIDKubernetes, output.WriteList, workspaceID)
}

func listAllKubernetesImpl(kubernetesClient kubernetesClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := kubernetesClient.ListKubernetesConfigsByWorkspace(v4kube.NewListKubernetesConfigsByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, r := range resp.Payload.Responses {
		row := &kubernetes{
			Name:        *r.Name,
			Description: utils.SafeStringConvert(r.Description),
		}
		tableRows = append(tableRows, row)
	}

	writer(kubernetesHeader, tableRows)
	return nil
}
