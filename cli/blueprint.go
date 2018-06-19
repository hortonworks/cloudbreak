package cli

import (
	"time"

	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1blueprints"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var blueprintHeader = []string{"Name", "Description", "HDP Version", "Hostgroup Count", "Tags"}

type blueprintOut struct {
	Name           string `json:"Name" yaml:"Name"`
	Description    string `json:"Description" yaml:"Description"`
	HDPVersion     string `json:"HDPVersion" yaml:"HDPVersion"`
	HostgroupCount string `json:"HostgroupCount" yaml:"HostgroupCount"`
	Tags           string `json:"Tags" yaml:"Tags"`
}

func (b *blueprintOut) DataAsStringArray() []string {
	return []string{b.Name, b.Description, b.HDPVersion, b.HostgroupCount, b.Tags}
}

func CreateBlueprintFromUrl(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateBlueprintFromUrl] creating blueprint from a URL")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	urlLocation := c.String(FlURL.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.V1blueprints,
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		c.Bool(FlPublicOptional.Name),
		c.Bool(FlDlOptional.Name),
		utils.ReadContentFromURL(urlLocation, new(http.Client)))
}

func CreateBlueprintFromFile(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateBlueprintFromFile] creating blueprint from a file")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(FlFile.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.V1blueprints,
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		c.Bool(FlPublicOptional.Name),
		c.Bool(FlDlOptional.Name),
		utils.ReadFile(fileLocation))
}

func createBlueprintImpl(client blueprintClient, name string, description string, public bool, dl bool, ambariBlueprint []byte) *models_cloudbreak.BlueprintResponse {
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
	if public {
		log.Infof("[createBlueprintImpl] sending create public blueprint request")
		resp, err := client.PostPublicBlueprint(v1blueprints.NewPostPublicBlueprintParams().WithBody(bpRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		blueprint = resp.Payload
	} else {
		log.Infof("[createBlueprintImpl] sending create private blueprint request")
		resp, err := client.PostPrivateBlueprint(v1blueprints.NewPostPrivateBlueprintParams().WithBody(bpRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		blueprint = resp.Payload
	}
	log.Infof("[createBlueprintImpl] blueprint created: %s (id: %d)", *blueprint.Name, blueprint.ID)
	return blueprint
}

func DescribeBlueprint(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe blueprint")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	bp := fetchBlueprint(c.String(FlName.Name), cbClient.Cloudbreak.V1blueprints)
	output.Write(blueprintHeader, convertResponseToBlueprint(bp))
}

type getPublicBlueprint interface {
	GetPublicBlueprint(*v1blueprints.GetPublicBlueprintParams) (*v1blueprints.GetPublicBlueprintOK, error)
}

func fetchBlueprint(name string, client getPublicBlueprint) *models_cloudbreak.BlueprintResponse {
	resp, err := client.GetPublicBlueprint(v1blueprints.NewGetPublicBlueprintParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func DeleteBlueprint(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete blueprint")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	deleteBlueprintsImpl(cbClient.Cloudbreak.V1blueprints, c.String(FlName.Name))
}

func deleteBlueprintsImpl(client blueprintClient, name string) {
	log.Infof("[deleteBlueprintsImpl] sending delete blueprint request with name: %s", name)
	err := client.DeletePrivateBlueprint(v1blueprints.NewDeletePrivateBlueprintParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteBlueprintsImpl] blueprint deleted, name: %s", name)
}

func ListBlueprints(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "get public blueprints")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listBlueprintsImpl(cbClient.Cloudbreak.V1blueprints, output.WriteList)
}

type blueprintClient interface {
	PostPrivateBlueprint(params *v1blueprints.PostPrivateBlueprintParams) (*v1blueprints.PostPrivateBlueprintOK, error)
	PostPublicBlueprint(params *v1blueprints.PostPublicBlueprintParams) (*v1blueprints.PostPublicBlueprintOK, error)
	GetPrivatesBlueprint(params *v1blueprints.GetPrivatesBlueprintParams) (*v1blueprints.GetPrivatesBlueprintOK, error)
	DeletePrivateBlueprint(params *v1blueprints.DeletePrivateBlueprintParams) error
}

func listBlueprintsImpl(client blueprintClient, writer func([]string, []utils.Row)) {
	log.Infof("[listBlueprintsImpl] sending blueprint list request")
	resp, err := client.GetPrivatesBlueprint(v1blueprints.NewGetPrivatesBlueprintParams())
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
		HDPVersion:     fmt.Sprintf("%v", blueprintsNode["stack_version"]),
		HostgroupCount: fmt.Sprint(bp.HostGroupCount),
		Tags:           bp.Status,
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
