package clustertemplate

import (
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"

	v4bp "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_blueprints"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

var clusterTemplateHeader = []string{"Name", "Description", "StackName", "StackVersion", "Hostgroup Count", "Tags"}

type clusterTemplateOut struct {
	Name           string `json:"Name" yaml:"Name"`
	Description    string `json:"Description" yaml:"Description"`
	StackName      string `json:"StackName" yaml:"StackName"`
	StackVersion   string `json:"StackVersion" yaml:"StackVersion"`
	HostgroupCount string `json:"HostgroupCount" yaml:"HostgroupCount"`
	Tags           string `json:"Tags" yaml:"Tags"`
	Created        int64  `json:"Created" yaml:"Created"`
}

type clusterTemplateOutJsonDescribe struct {
	*clusterTemplateOut
	Content string `json:"ClusterTemplateTextAsBase64" yaml:"ClusterTemplateTextAsBase64"`
	Crn     string `json:"Crn" yaml:"Crn"`
}

type clusterTemplateOutTableDescribe struct {
	*clusterTemplateOut
	Crn string `json:"Crn" yaml:"Crn"`
}

func (b *clusterTemplateOut) DataAsStringArray() []string {
	return []string{b.Name, b.Description, b.StackName, b.StackVersion, b.HostgroupCount, b.Tags}
}

func (b *clusterTemplateOutJsonDescribe) DataAsStringArray() []string {
	return append(b.clusterTemplateOut.DataAsStringArray(), b.Crn)
}

func (b *clusterTemplateOutTableDescribe) DataAsStringArray() []string {
	return append(b.clusterTemplateOut.DataAsStringArray(), b.Crn)
}

func CreateClusterTemplateFromUrl(c *cli.Context) {

	log.Infof("[CreateClusterTemplateFromUrl] creating cluster template from a URL")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	urlLocation := c.String(fl.FlURL.Name)
	createClusterTemplateImpl(
		cbClient.Cloudbreak.V4WorkspaceIDBlueprints,
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.Bool(fl.FlDlOptional.Name),
		utils.ReadContentFromURL(urlLocation, new(http.Client)),
		c.Int64(fl.FlWorkspaceOptional.Name))
}

func CreateClusterTemplateFromFile(c *cli.Context) {

	log.Infof("[CreateClusterTemplateFromFile] creating cluster template from a file")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(fl.FlFile.Name)
	createClusterTemplateImpl(
		cbClient.Cloudbreak.V4WorkspaceIDBlueprints,
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.Bool(fl.FlDlOptional.Name),
		utils.ReadFile(fileLocation),
		c.Int64(fl.FlWorkspaceOptional.Name))
}

func createClusterTemplateImpl(client clusterTemplateClient, name string, description string, dl bool, clusterTemplate []byte, workspace int64) *model.BlueprintV4Response {
	defer utils.TimeTrack(time.Now(), "create cluster template")
	tags := map[string]interface{}{"shared_services_ready": dl}
	bpRequest := &model.BlueprintV4Request{
		Name:        name,
		Description: &description,
		Blueprint:   base64.StdEncoding.EncodeToString(clusterTemplate),
		Tags:        tags,
	}
	var clusterTemplateResp *model.BlueprintV4Response
	log.Infof("[createClusterTemplateImpl] sending create cluster template request")
	resp, err := client.CreateBlueprintInWorkspace(v4bp.NewCreateBlueprintInWorkspaceParams().WithWorkspaceID(workspace).WithBody(bpRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	clusterTemplateResp = resp.Payload

	log.Infof("[createClusterTemplateImpl] cluster template created: %s (crn: %d)", *clusterTemplateResp.Name, clusterTemplateResp.Crn)
	return clusterTemplateResp
}

func DescribeClusterTemplate(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe cluster template")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	bp := FetchClusterTemplate(c.Int64(fl.FlWorkspaceOptional.Name), c.String(fl.FlName.Name), cbClient.Cloudbreak.V4WorkspaceIDBlueprints)
	if output.Format != "table" {
		output.Write(append(clusterTemplateHeader, "Content", "CRN"), convertResponseWithContentAndCRNToClusterTemplate(bp))
	} else {
		output.Write(append(clusterTemplateHeader, "CNR"), convertResponseWithContentAndCRNToClusterTemplate(bp))
	}
}

type GetClusterTemplateInWorkspace interface {
	GetBlueprintInWorkspace(*v4bp.GetBlueprintInWorkspaceParams) (*v4bp.GetBlueprintInWorkspaceOK, error)
}

func FetchClusterTemplate(workspace int64, name string, client GetClusterTemplateInWorkspace) *model.BlueprintV4Response {
	resp, err := client.GetBlueprintInWorkspace(v4bp.NewGetBlueprintInWorkspaceParams().WithWorkspaceID(workspace).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func DeleteClusterTemplates(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete cluster templates")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	deleteClusterTemplatesImpl(cbClient.Cloudbreak.V4WorkspaceIDBlueprints, c.Int64(fl.FlWorkspaceOptional.Name), c.StringSlice(fl.FlNames.Name))
}

func deleteClusterTemplatesImpl(client clusterTemplateClient, workspace int64, names []string) {
	log.Infof("[deleteClusterTemplatesImpl] sending delete cluster template request with names: %s", names)
	_, err := client.DeleteBlueprintsInWorkspace(v4bp.NewDeleteBlueprintsInWorkspaceParams().WithWorkspaceID(workspace).WithBody(names))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteClusterTemplatesImpl] cluster templates deleted, names: %s", names)
}

func ListClusterTemplates(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "get cluster templates")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspace := fl.FlWorkspaceOptional.Name
	listClusterTemplatesImpl(c.Int64(workspace), cbClient.Cloudbreak.V4WorkspaceIDBlueprints, output.WriteList)
}

type clusterTemplateClient interface {
	CreateBlueprintInWorkspace(params *v4bp.CreateBlueprintInWorkspaceParams) (*v4bp.CreateBlueprintInWorkspaceOK, error)
	ListBlueprintsByWorkspace(params *v4bp.ListBlueprintsByWorkspaceParams) (*v4bp.ListBlueprintsByWorkspaceOK, error)
	DeleteBlueprintsInWorkspace(params *v4bp.DeleteBlueprintsInWorkspaceParams) (*v4bp.DeleteBlueprintsInWorkspaceOK, error)
}

func listClusterTemplatesImpl(workspace int64, client clusterTemplateClient, writer func([]string, []utils.Row)) {
	log.Infof("[listClusterTemplatesImpl] sending cluster template list request")
	resp, err := client.ListBlueprintsByWorkspace(v4bp.NewListBlueprintsByWorkspaceParams().WithWorkspaceID(workspace))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	for _, bp := range resp.Payload.Responses {
		tableRows = append(tableRows, convertResponseToClusterTemplate(bp))
	}

	writer(clusterTemplateHeader, tableRows)
}

func convertResponseToClusterTemplate(bp *model.BlueprintV4ViewResponse) *clusterTemplateOut {
	return &clusterTemplateOut{
		Name:           *bp.Name,
		Description:    utils.SafeStringConvert(bp.Description),
		StackName:      fmt.Sprintf("%v", bp.StackType),
		StackVersion:   fmt.Sprintf("%v", bp.StackVersion),
		HostgroupCount: fmt.Sprint(bp.HostGroupCount),
		Tags:           bp.Status,
		Created:        bp.Created,
	}
}

func convertResponseWithContentAndCRNToClusterTemplate(bp *model.BlueprintV4Response) *clusterTemplateOutJsonDescribe {
	jsonRoot := decodeAndParseToJson(bp.Blueprint)
	var stackName = "Undefined"
	var stackVersion = "Undefined"

	if clusterTemplates, ok := jsonRoot["Blueprints"]; ok {
		clusterTemplatesNode := clusterTemplates.(map[string]interface{})
		stackName = clusterTemplatesNode["stack_name"].(string)
		stackVersion = clusterTemplatesNode["stack_version"].(string)
	} else if cdhVersion, ok := jsonRoot["cdhVersion"]; ok {
		stackName = "CDH"
		stackVersion = cdhVersion.(string)
	}

	return &clusterTemplateOutJsonDescribe{
		clusterTemplateOut: &clusterTemplateOut{
			Name:           *bp.Name,
			Description:    *bp.Description,
			StackName:      fmt.Sprintf("%v", stackName),
			StackVersion:   fmt.Sprintf("%v", stackVersion),
			HostgroupCount: fmt.Sprint(bp.HostGroupCount),
			Tags:           bp.Status,
			Created:        bp.Created,
		},
		Content: bp.Blueprint,
		Crn:     *bp.Crn,
	}
}

func decodeAndParseToJson(encodedClusterTemplate string) map[string]interface{} {
	log.Debugf("[decodeAndParseToJson] decoding cluster template from base64")
	b, err := base64.StdEncoding.DecodeString(encodedClusterTemplate)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Debugf("[decodeAndParseToJson] parse cluster template to JSON")
	var clusterTemplateJson map[string]interface{}
	if err = json.Unmarshal(b, &clusterTemplateJson); err != nil {
		utils.LogErrorAndExit(err)
	}
	return clusterTemplateJson
}
