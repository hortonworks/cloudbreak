package clusterdefinition

import (
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"

	log "github.com/Sirupsen/logrus"
	v4bp "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_cluster_definitions"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var clusterDefinitionHeader = []string{"Name", "Description", "StackName", "StackVersion", "Hostgroup Count", "Tags"}

type clusterDefinitionOut struct {
	Name           string `json:"Name" yaml:"Name"`
	Description    string `json:"Description" yaml:"Description"`
	StackName      string `json:"StackName" yaml:"StackName"`
	StackVersion   string `json:"StackVersion" yaml:"StackVersion"`
	HostgroupCount string `json:"HostgroupCount" yaml:"HostgroupCount"`
	Tags           string `json:"Tags" yaml:"Tags"`
}

type clusterDefinitionOutJsonDescribe struct {
	*clusterDefinitionOut
	Content string `json:"ClusterDefinitionTextAsBase64" yaml:"ClusterDefinitionTextAsBase64"`
	ID      string `json:"ID" yaml:"ID"`
}

type clusterDefinitionOutTableDescribe struct {
	*clusterDefinitionOut
	ID string `json:"ID" yaml:"ID"`
}

func (b *clusterDefinitionOut) DataAsStringArray() []string {
	return []string{b.Name, b.Description, b.StackName, b.StackVersion, b.HostgroupCount, b.Tags}
}

func (b *clusterDefinitionOutJsonDescribe) DataAsStringArray() []string {
	return append(b.clusterDefinitionOut.DataAsStringArray(), b.ID)
}

func (b *clusterDefinitionOutTableDescribe) DataAsStringArray() []string {
	return append(b.clusterDefinitionOut.DataAsStringArray(), b.ID)
}

func CreateClusterDefinitionFromUrl(c *cli.Context) {

	log.Infof("[CreateClusterDefinitionFromUrl] creating cluster definition from a URL")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	urlLocation := c.String(fl.FlURL.Name)
	createClusterDefinitionImpl(
		cbClient.Cloudbreak.V4WorkspaceIDClusterDefinitions,
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.Bool(fl.FlDlOptional.Name),
		utils.ReadContentFromURL(urlLocation, new(http.Client)),
		c.Int64(fl.FlWorkspaceOptional.Name))
}

func CreateClusterDefinitionFromFile(c *cli.Context) {

	log.Infof("[CreateClusterDefinitionFromFile] creating cluster definition from a file")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(fl.FlFile.Name)
	createClusterDefinitionImpl(
		cbClient.Cloudbreak.V4WorkspaceIDClusterDefinitions,
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.Bool(fl.FlDlOptional.Name),
		utils.ReadFile(fileLocation),
		c.Int64(fl.FlWorkspaceOptional.Name))
}

func createClusterDefinitionImpl(client clusterDefinitionClient, name string, description string, dl bool, clusterDefinition []byte, workspace int64) *model.ClusterDefinitionV4Response {
	defer utils.TimeTrack(time.Now(), "create clusger definition")
	tags := map[string]interface{}{"shared_services_ready": dl}
	bpRequest := &model.ClusterDefinitionV4Request{
		Name:              &name,
		Description:       &description,
		ClusterDefinition: base64.StdEncoding.EncodeToString(clusterDefinition),
		Tags:              tags,
	}
	var clusterDefinitionResp *model.ClusterDefinitionV4Response
	log.Infof("[createClusterDefinitionImpl] sending create cluster definition request")
	resp, err := client.CreateClusterDefinitionInWorkspace(v4bp.NewCreateClusterDefinitionInWorkspaceParams().WithWorkspaceID(workspace).WithBody(bpRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	clusterDefinitionResp = resp.Payload

	log.Infof("[createClusterDefinitionImpl] cluster definition created: %s (id: %d)", *clusterDefinitionResp.Name, clusterDefinitionResp.ID)
	return clusterDefinitionResp
}

func DescribeClusterDefinition(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe cluster definition")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	bp := FetchClusterDefinition(c.Int64(fl.FlWorkspaceOptional.Name), c.String(fl.FlName.Name), cbClient.Cloudbreak.V4WorkspaceIDClusterDefinitions)
	if output.Format != "table" {
		output.Write(append(clusterDefinitionHeader, "Content", "ID"), convertResponseWithContentAndIDToClusterDefinition(bp))
	} else {
		output.Write(append(clusterDefinitionHeader, "ID"), convertResponseWithIDToClusterDefinition(bp))
	}
}

type GetClusterDefinitionInWorkspace interface {
	GetClusterDefinitionInWorkspace(*v4bp.GetClusterDefinitionInWorkspaceParams) (*v4bp.GetClusterDefinitionInWorkspaceOK, error)
}

func FetchClusterDefinition(workspace int64, name string, client GetClusterDefinitionInWorkspace) *model.ClusterDefinitionV4Response {
	resp, err := client.GetClusterDefinitionInWorkspace(v4bp.NewGetClusterDefinitionInWorkspaceParams().WithWorkspaceID(workspace).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func DeleteClusterDefinitions(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete cluster definitions")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	deleteClusterDefinitionsImpl(cbClient.Cloudbreak.V4WorkspaceIDClusterDefinitions, c.Int64(fl.FlWorkspaceOptional.Name), c.StringSlice(fl.FlNames.Name))
}

func deleteClusterDefinitionsImpl(client clusterDefinitionClient, workspace int64, names []string) {
	log.Infof("[deleteClusterDefinitionsImpl] sending delete cluster definition request with names: %s", names)
	_, err := client.DeleteClusterDefinitionsInWorkspace(v4bp.NewDeleteClusterDefinitionsInWorkspaceParams().WithWorkspaceID(workspace).WithBody(names))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteClusterDefinitionsImpl] cluster definitions deleted, names: %s", names)
}

func ListClusterDefinitions(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "get cluster definitions")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspace := fl.FlWorkspaceOptional.Name
	listClusterDefinitionsImpl(c.Int64(workspace), cbClient.Cloudbreak.V4WorkspaceIDClusterDefinitions, output.WriteList)
}

type clusterDefinitionClient interface {
	CreateClusterDefinitionInWorkspace(params *v4bp.CreateClusterDefinitionInWorkspaceParams) (*v4bp.CreateClusterDefinitionInWorkspaceOK, error)
	ListClusterDefinitionsByWorkspace(params *v4bp.ListClusterDefinitionsByWorkspaceParams) (*v4bp.ListClusterDefinitionsByWorkspaceOK, error)
	DeleteClusterDefinitionsInWorkspace(params *v4bp.DeleteClusterDefinitionsInWorkspaceParams) (*v4bp.DeleteClusterDefinitionsInWorkspaceOK, error)
}

func listClusterDefinitionsImpl(workspace int64, client clusterDefinitionClient, writer func([]string, []utils.Row)) {
	log.Infof("[listClusterDefinitionsImpl] sending cluster definition list request")
	resp, err := client.ListClusterDefinitionsByWorkspace(v4bp.NewListClusterDefinitionsByWorkspaceParams().WithWorkspaceID(workspace))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	for _, bp := range resp.Payload.Responses {
		tableRows = append(tableRows, convertResponseToClusterDefinition(bp))
	}

	writer(clusterDefinitionHeader, tableRows)
}

func convertResponseToClusterDefinition(bp *model.ClusterDefinitionV4ViewResponse) *clusterDefinitionOut {
	return &clusterDefinitionOut{
		Name:           *bp.Name,
		Description:    utils.SafeStringConvert(bp.Description),
		StackName:      fmt.Sprintf("%v", bp.StackType),
		StackVersion:   fmt.Sprintf("%v", bp.StackVersion),
		HostgroupCount: fmt.Sprint(bp.HostGroupCount),
		Tags:           bp.Status,
	}
}

func convertResponseWithContentAndIDToClusterDefinition(bp *model.ClusterDefinitionV4Response) *clusterDefinitionOutJsonDescribe {
	jsonRoot := decodeAndParseToJson(bp.ClusterDefinition)
	clusterDefinitionsNode := jsonRoot["Blueprints"].(map[string]interface{})
	return &clusterDefinitionOutJsonDescribe{
		clusterDefinitionOut: &clusterDefinitionOut{
			Name:           *bp.Name,
			Description:    *bp.Description,
			StackName:      fmt.Sprintf("%v", clusterDefinitionsNode["stack_name"]),
			StackVersion:   fmt.Sprintf("%v", clusterDefinitionsNode["stack_version"]),
			HostgroupCount: fmt.Sprint(bp.HostGroupCount),
			Tags:           bp.Status,
		},
		Content: bp.ClusterDefinition,
		ID:      strconv.FormatInt(bp.ID, 10),
	}
}

func convertResponseWithIDToClusterDefinition(bp *model.ClusterDefinitionV4Response) *clusterDefinitionOutTableDescribe {
	jsonRoot := decodeAndParseToJson(bp.ClusterDefinition)
	clusterDefinitionsNode := jsonRoot["Blueprints"].(map[string]interface{})
	return &clusterDefinitionOutTableDescribe{
		clusterDefinitionOut: &clusterDefinitionOut{
			Name:           *bp.Name,
			Description:    *bp.Description,
			StackName:      fmt.Sprintf("%v", clusterDefinitionsNode["stack_name"]),
			StackVersion:   fmt.Sprintf("%v", clusterDefinitionsNode["stack_version"]),
			HostgroupCount: fmt.Sprint(bp.HostGroupCount),
			Tags:           bp.Status,
		},
		ID: strconv.FormatInt(bp.ID, 10),
	}
}

func decodeAndParseToJson(encodedClusterDefinition string) map[string]interface{} {
	log.Debugf("[decodeAndParseToJson] decoding cluster definition from base64")
	b, err := base64.StdEncoding.DecodeString(encodedClusterDefinition)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Debugf("[decodeAndParseToJson] parse cluster definition to JSON")
	var clusterDefinitionJson map[string]interface{}
	if err = json.Unmarshal(b, &clusterDefinitionJson); err != nil {
		utils.LogErrorAndExit(err)
	}
	return clusterDefinitionJson
}
