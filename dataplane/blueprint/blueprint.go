package blueprint

import (
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"

	log "github.com/Sirupsen/logrus"
	v4bp "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_blueprints"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var blueprintHeader = []string{"Name", "Description", "StackName", "StackVersion", "Hostgroup Count", "Tags"}

type blueprintOut struct {
	Name           string `json:"Name" yaml:"Name"`
	Description    string `json:"Description" yaml:"Description"`
	StackName      string `json:"StackName" yaml:"StackName"`
	StackVersion   string `json:"StackVersion" yaml:"StackVersion"`
	HostgroupCount string `json:"HostgroupCount" yaml:"HostgroupCount"`
	Tags           string `json:"Tags" yaml:"Tags"`
}

type blueprintOutJsonDescribe struct {
	*blueprintOut
	Content string `json:"BlueprintTextAsBase64" yaml:"BlueprintTextAsBase64"`
	ID      string `json:"ID" yaml:"ID"`
}

type blueprintOutTableDescribe struct {
	*blueprintOut
	ID string `json:"ID" yaml:"ID"`
}

func (b *blueprintOut) DataAsStringArray() []string {
	return []string{b.Name, b.Description, b.StackName, b.StackVersion, b.HostgroupCount, b.Tags}
}

func (b *blueprintOutJsonDescribe) DataAsStringArray() []string {
	return append(b.blueprintOut.DataAsStringArray(), b.ID)
}

func (b *blueprintOutTableDescribe) DataAsStringArray() []string {
	return append(b.blueprintOut.DataAsStringArray(), b.ID)
}

func CreateBlueprintFromUrl(c *cli.Context) {

	log.Infof("[CreateBlueprintFromUrl] creating blueprint from a URL")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	urlLocation := c.String(fl.FlURL.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.V4WorkspaceIDBlueprints,
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.Bool(fl.FlDlOptional.Name),
		utils.ReadContentFromURL(urlLocation, new(http.Client)),
		c.Int64(fl.FlWorkspaceOptional.Name))
}

func CreateBlueprintFromFile(c *cli.Context) {

	log.Infof("[CreateBlueprintFromFile] creating blueprint from a file")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(fl.FlFile.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.V4WorkspaceIDBlueprints,
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.Bool(fl.FlDlOptional.Name),
		utils.ReadFile(fileLocation),
		c.Int64(fl.FlWorkspaceOptional.Name))
}

func createBlueprintImpl(client blueprintClient, name string, description string, dl bool, blueprint []byte, workspace int64) *model.BlueprintV4Response {
	defer utils.TimeTrack(time.Now(), "create clusger definition")
	tags := map[string]interface{}{"shared_services_ready": dl}
	bpRequest := &model.BlueprintV4Request{
		Name:        name,
		Description: &description,
		Blueprint:   base64.StdEncoding.EncodeToString(blueprint),
		Tags:        tags,
	}
	var blueprintResp *model.BlueprintV4Response
	log.Infof("[createBlueprintImpl] sending create blueprint request")
	resp, err := client.CreateBlueprintInWorkspace(v4bp.NewCreateBlueprintInWorkspaceParams().WithWorkspaceID(workspace).WithBody(bpRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	blueprintResp = resp.Payload

	log.Infof("[createBlueprintImpl] blueprint created: %s (id: %d)", *blueprintResp.Name, blueprintResp.ID)
	return blueprintResp
}

func DescribeBlueprint(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe blueprint")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	bp := FetchBlueprint(c.Int64(fl.FlWorkspaceOptional.Name), c.String(fl.FlName.Name), cbClient.Cloudbreak.V4WorkspaceIDBlueprints)
	if output.Format != "table" {
		output.Write(append(blueprintHeader, "Content", "ID"), convertResponseWithContentAndIDToBlueprint(bp))
	} else {
		output.Write(append(blueprintHeader, "ID"), convertResponseWithContentAndIDToBlueprint(bp))
	}
}

type GetBlueprintInWorkspace interface {
	GetBlueprintInWorkspace(*v4bp.GetBlueprintInWorkspaceParams) (*v4bp.GetBlueprintInWorkspaceOK, error)
}

func FetchBlueprint(workspace int64, name string, client GetBlueprintInWorkspace) *model.BlueprintV4Response {
	resp, err := client.GetBlueprintInWorkspace(v4bp.NewGetBlueprintInWorkspaceParams().WithWorkspaceID(workspace).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func DeleteBlueprints(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete blueprints")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	deleteBlueprintsImpl(cbClient.Cloudbreak.V4WorkspaceIDBlueprints, c.Int64(fl.FlWorkspaceOptional.Name), c.StringSlice(fl.FlNames.Name))
}

func deleteBlueprintsImpl(client blueprintClient, workspace int64, names []string) {
	log.Infof("[deleteBlueprintsImpl] sending delete blueprint request with names: %s", names)
	_, err := client.DeleteBlueprintsInWorkspace(v4bp.NewDeleteBlueprintsInWorkspaceParams().WithWorkspaceID(workspace).WithBody(names))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteBlueprintsImpl] blueprints deleted, names: %s", names)
}

func ListBlueprints(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "get blueprints")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspace := fl.FlWorkspaceOptional.Name
	listBlueprintsImpl(c.Int64(workspace), cbClient.Cloudbreak.V4WorkspaceIDBlueprints, output.WriteList)
}

type blueprintClient interface {
	CreateBlueprintInWorkspace(params *v4bp.CreateBlueprintInWorkspaceParams) (*v4bp.CreateBlueprintInWorkspaceOK, error)
	ListBlueprintsByWorkspace(params *v4bp.ListBlueprintsByWorkspaceParams) (*v4bp.ListBlueprintsByWorkspaceOK, error)
	DeleteBlueprintsInWorkspace(params *v4bp.DeleteBlueprintsInWorkspaceParams) (*v4bp.DeleteBlueprintsInWorkspaceOK, error)
}

func listBlueprintsImpl(workspace int64, client blueprintClient, writer func([]string, []utils.Row)) {
	log.Infof("[listBlueprintsImpl] sending blueprint list request")
	resp, err := client.ListBlueprintsByWorkspace(v4bp.NewListBlueprintsByWorkspaceParams().WithWorkspaceID(workspace))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	for _, bp := range resp.Payload.Responses {
		tableRows = append(tableRows, convertResponseToBlueprint(bp))
	}

	writer(blueprintHeader, tableRows)
}

func convertResponseToBlueprint(bp *model.BlueprintV4ViewResponse) *blueprintOut {
	return &blueprintOut{
		Name:           *bp.Name,
		Description:    utils.SafeStringConvert(bp.Description),
		StackName:      fmt.Sprintf("%v", bp.StackType),
		StackVersion:   fmt.Sprintf("%v", bp.StackVersion),
		HostgroupCount: fmt.Sprint(bp.HostGroupCount),
		Tags:           bp.Status,
	}
}

func convertResponseWithContentAndIDToBlueprint(bp *model.BlueprintV4Response) *blueprintOutJsonDescribe {
	jsonRoot := decodeAndParseToJson(bp.Blueprint)
	var stackName = "Undefined"
	var stackVersion = "Undefined"

	if blueprints, ok := jsonRoot["Blueprints"]; ok {
		blueprintsNode := blueprints.(map[string]interface{})
		stackName = blueprintsNode["stack_name"].(string)
		stackVersion = blueprintsNode["stack_version"].(string)
	} else if cdhVersion, ok := jsonRoot["cdhVersion"]; ok {
		stackName = "CDH"
		stackVersion = cdhVersion.(string)
	}

	return &blueprintOutJsonDescribe{
		blueprintOut: &blueprintOut{
			Name:           *bp.Name,
			Description:    *bp.Description,
			StackName:      fmt.Sprintf("%v", stackName),
			StackVersion:   fmt.Sprintf("%v", stackVersion),
			HostgroupCount: fmt.Sprint(bp.HostGroupCount),
			Tags:           bp.Status,
		},
		Content: bp.Blueprint,
		ID:      strconv.FormatInt(bp.ID, 10),
	}
}

func decodeAndParseToJson(encodedBlueprint string) map[string]interface{} {
	log.Debugf("[decodeAndParseToJson] decoding blueprint from base64")
	b, err := base64.StdEncoding.DecodeString(encodedBlueprint)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Debugf("[decodeAndParseToJson] parse blueprint to JSON")
	var blueprintJson map[string]interface{}
	if err = json.Unmarshal(b, &blueprintJson); err != nil {
		utils.LogErrorAndExit(err)
	}
	return blueprintJson
}
