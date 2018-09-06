package cli

import (
	"strconv"
	"time"

	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_organization_id_blueprints"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
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
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateBlueprintFromUrl] creating blueprint from a URL")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	urlLocation := c.String(FlURL.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.V3OrganizationIDBlueprints,
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		c.Bool(FlDlOptional.Name),
		utils.ReadContentFromURL(urlLocation, new(http.Client)),
		c.Int64(FlOrganizationOptional.Name))
}

func CreateBlueprintFromFile(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateBlueprintFromFile] creating blueprint from a file")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(FlFile.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.V3OrganizationIDBlueprints,
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		c.Bool(FlDlOptional.Name),
		utils.ReadFile(fileLocation),
		c.Int64(FlOrganizationOptional.Name))
}

func createBlueprintImpl(client blueprintClient, name string, description string, dl bool, ambariBlueprint []byte, organization int64) *models_cloudbreak.BlueprintResponse {
	defer utils.TimeTrack(time.Now(), "create blueprint")
	tags := map[string]interface{}{"shared_services_ready": dl}
	bpRequest := &models_cloudbreak.BlueprintRequest{
		Name:            &name,
		Description:     &description,
		AmbariBlueprint: base64.StdEncoding.EncodeToString(ambariBlueprint),
		Inputs:          make([]*models_cloudbreak.BlueprintParameter, 0),
		Tags:            tags,
	}
	var blueprint *models_cloudbreak.BlueprintResponse
	log.Infof("[createBlueprintImpl] sending create blueprint request")
	resp, err := client.CreateBlueprintInOrganization(v3_organization_id_blueprints.NewCreateBlueprintInOrganizationParams().WithOrganizationID(organization).WithBody(bpRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	blueprint = resp.Payload

	log.Infof("[createBlueprintImpl] blueprint created: %s (id: %d)", *blueprint.Name, blueprint.ID)
	return blueprint
}

func DescribeBlueprint(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe blueprint")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	bp := fetchBlueprint(c.Int64(FlOrganizationOptional.Name), c.String(FlName.Name), cbClient.Cloudbreak.V3OrganizationIDBlueprints)
	if output.Format != "table" {
		output.Write(append(blueprintHeader, "Content", "ID"), convertResponseWithContentAndIDToBlueprint(bp))
	} else {
		output.Write(append(blueprintHeader, "ID"), convertResponseWithIDToBlueprint(bp))
	}
}

type getBlueprintInOrganization interface {
	GetBlueprintInOrganization(*v3_organization_id_blueprints.GetBlueprintInOrganizationParams) (*v3_organization_id_blueprints.GetBlueprintInOrganizationOK, error)
}

func fetchBlueprint(organization int64, name string, client getBlueprintInOrganization) *models_cloudbreak.BlueprintResponse {
	resp, err := client.GetBlueprintInOrganization(v3_organization_id_blueprints.NewGetBlueprintInOrganizationParams().WithOrganizationID(organization).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func DeleteBlueprint(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete blueprint")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	deleteBlueprintsImpl(cbClient.Cloudbreak.V3OrganizationIDBlueprints, c.Int64(FlOrganizationOptional.Name), c.String(FlName.Name))
}

func deleteBlueprintsImpl(client blueprintClient, organization int64, name string) {
	log.Infof("[deleteBlueprintsImpl] sending delete blueprint request with name: %s", name)
	_, err := client.DeleteBlueprintInOrganization(v3_organization_id_blueprints.NewDeleteBlueprintInOrganizationParams().WithOrganizationID(organization).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteBlueprintsImpl] blueprint deleted, name: %s", name)
}

func ListBlueprints(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "get blueprints")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	organization := FlOrganizationOptional.Name
	listBlueprintsImpl(c.Int64(organization), cbClient.Cloudbreak.V3OrganizationIDBlueprints, output.WriteList)
}

type blueprintClient interface {
	CreateBlueprintInOrganization(params *v3_organization_id_blueprints.CreateBlueprintInOrganizationParams) (*v3_organization_id_blueprints.CreateBlueprintInOrganizationOK, error)
	ListBlueprintsByOrganization(params *v3_organization_id_blueprints.ListBlueprintsByOrganizationParams) (*v3_organization_id_blueprints.ListBlueprintsByOrganizationOK, error)
	DeleteBlueprintInOrganization(params *v3_organization_id_blueprints.DeleteBlueprintInOrganizationParams) (*v3_organization_id_blueprints.DeleteBlueprintInOrganizationOK, error)
}

func listBlueprintsImpl(organization int64, client blueprintClient, writer func([]string, []utils.Row)) {
	log.Infof("[listBlueprintsImpl] sending blueprint list request")
	resp, err := client.ListBlueprintsByOrganization(v3_organization_id_blueprints.NewListBlueprintsByOrganizationParams().WithOrganizationID(organization))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	tableRows := []utils.Row{}
	for _, bp := range resp.Payload {
		tableRows = append(tableRows, convertResponseToBlueprint(bp))
	}

	writer(blueprintHeader, tableRows)
}

func convertResponseToBlueprint(bp *models_cloudbreak.BlueprintResponse) *blueprintOut {
	jsonRoot := decodeAndParseToJson(bp.AmbariBlueprint)
	blueprintsNode := jsonRoot["Blueprints"].(map[string]interface{})
	return &blueprintOut{
		Name:           *bp.Name,
		Description:    *bp.Description,
		StackName:      fmt.Sprintf("%v", blueprintsNode["stack_name"]),
		StackVersion:   fmt.Sprintf("%v", blueprintsNode["stack_version"]),
		HostgroupCount: fmt.Sprint(bp.HostGroupCount),
		Tags:           bp.Status,
	}
}

func convertResponseWithContentAndIDToBlueprint(bp *models_cloudbreak.BlueprintResponse) *blueprintOutJsonDescribe {
	jsonRoot := decodeAndParseToJson(bp.AmbariBlueprint)
	blueprintsNode := jsonRoot["Blueprints"].(map[string]interface{})
	return &blueprintOutJsonDescribe{
		blueprintOut: &blueprintOut{
			Name:           *bp.Name,
			Description:    *bp.Description,
			StackName:      fmt.Sprintf("%v", blueprintsNode["stack_name"]),
			StackVersion:   fmt.Sprintf("%v", blueprintsNode["stack_version"]),
			HostgroupCount: fmt.Sprint(bp.HostGroupCount),
			Tags:           bp.Status,
		},
		Content: bp.AmbariBlueprint,
		ID:      strconv.FormatInt(bp.ID, 10),
	}
}

func convertResponseWithIDToBlueprint(bp *models_cloudbreak.BlueprintResponse) *blueprintOutTableDescribe {
	jsonRoot := decodeAndParseToJson(bp.AmbariBlueprint)
	blueprintsNode := jsonRoot["Blueprints"].(map[string]interface{})
	return &blueprintOutTableDescribe{
		blueprintOut: &blueprintOut{
			Name:           *bp.Name,
			Description:    *bp.Description,
			StackName:      fmt.Sprintf("%v", blueprintsNode["stack_name"]),
			StackVersion:   fmt.Sprintf("%v", blueprintsNode["stack_version"]),
			HostgroupCount: fmt.Sprint(bp.HostGroupCount),
			Tags:           bp.Status,
		},
		ID: strconv.FormatInt(bp.ID, 10),
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
