package cli

import (
	"time"

	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/blueprints"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var blueprintHeader []string = []string{"Name", "Description", "HDP Version", "Hostgroup Count", "Tags"}

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
	checkRequiredFlags(c)

	log.Infof("[CreateBlueprintFromUrl] creating blueprint from a URL")
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	urlLocation := c.String(FlBlueprintURL.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.Blueprints,
		c.String(FlName.Name),
		c.String(FlDescription.Name),
		c.Bool(FlPublic.Name),
		utils.ReadContentFromURL(urlLocation, new(http.Client)))
}

func CreateBlueprintFromFile(c *cli.Context) {
	checkRequiredFlags(c)

	log.Infof("[CreateBlueprintFromFile] creating blueprint from a file")
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	fileLocation := c.String(FlBlueprintFileLocation.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.Blueprints,
		c.String(FlName.Name),
		c.String(FlDescription.Name),
		c.Bool(FlPublic.Name),
		utils.ReadFile(fileLocation))
}

func createBlueprintImpl(client blueprintsClient, name string, description string, public bool, ambariBlueprint []byte) *models_cloudbreak.BlueprintResponse {
	defer utils.TimeTrack(time.Now(), "create blueprint")
	bpRequest := &models_cloudbreak.BlueprintRequest{
		Name:            &name,
		Description:     &description,
		AmbariBlueprint: base64.StdEncoding.EncodeToString(ambariBlueprint),
		Inputs:          make([]*models_cloudbreak.BlueprintParameter, 0),
	}
	var blueprint *models_cloudbreak.BlueprintResponse
	if public {
		log.Infof("[createBlueprintImpl] sending create public blueprint request")
		resp, err := client.PostPublicBlueprint(blueprints.NewPostPublicBlueprintParams().WithBody(bpRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		blueprint = resp.Payload
	} else {
		log.Infof("[createBlueprintImpl] sending create private blueprint request")
		resp, err := client.PostPrivateBlueprint(blueprints.NewPostPrivateBlueprintParams().WithBody(bpRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		blueprint = resp.Payload
	}
	log.Infof("[createBlueprintImpl] blueprint created: %s (id: %d)", *blueprint.Name, blueprint.ID)
	return blueprint
}

func DescribeBlueprint(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "describe blueprint")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	resp, err := cbClient.Cloudbreak.Blueprints.GetPublicBlueprint(blueprints.NewGetPublicBlueprintParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	bp := resp.Payload
	output.Write(blueprintHeader, convertResponseToBlueprint(bp))
}

func DeleteBlueprint(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "delete blueprint")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	deleteBlueprintsImpl(cbClient.Cloudbreak.Blueprints, c.String(FlName.Name))
}

func deleteBlueprintsImpl(client blueprintsClient, name string) {
	log.Infof("[deleteBlueprintsImpl] sending delete blueprint request with name: %s", name)
	err := client.DeletePrivateBlueprint(blueprints.NewDeletePrivateBlueprintParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteBlueprintsImpl] blueprint deleted, name: %s", name)
}

func ListBlueprints(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "get public blueprints")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	listBlueprintsImpl(cbClient.Cloudbreak.Blueprints, output.WriteList)
}

type blueprintsClient interface {
	PostPrivateBlueprint(params *blueprints.PostPrivateBlueprintParams) (*blueprints.PostPrivateBlueprintOK, error)
	PostPublicBlueprint(params *blueprints.PostPublicBlueprintParams) (*blueprints.PostPublicBlueprintOK, error)
	GetPrivatesBlueprint(params *blueprints.GetPrivatesBlueprintParams) (*blueprints.GetPrivatesBlueprintOK, error)
	DeletePrivateBlueprint(params *blueprints.DeletePrivateBlueprintParams) error
}

func listBlueprintsImpl(client blueprintsClient, writer func([]string, []utils.Row)) {
	log.Infof("[listBlueprintsImpl] sending blueprint list request")
	resp, err := client.GetPrivatesBlueprint(blueprints.NewGetPrivatesBlueprintParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
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
	var blueprintJson interface{}
	json.Unmarshal(b, &blueprintJson)
	return blueprintJson.(map[string]interface{})
}
