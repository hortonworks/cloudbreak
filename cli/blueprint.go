package cli

import (
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/blueprints"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"fmt"
	"net/http"
	"encoding/base64"
	"encoding/json"
	"github.com/hortonworks/hdc-cli/cli/utils"
)

var BlueprintHeader []string = []string{"Blueprint Name", "Description", "HDP Version", "Hostgroup Count", "Tags"}

type Blueprint struct {
	Name           string `json:"BlueprintName" yaml:"BlueprintName"`
	Description    string `json:"Description" yaml:"Description"`
	HDPVersion     string `json:"HDPVersion" yaml:"HDPVersion"`
	HostgroupCount string `json:"HostgroupCount" yaml:"HostgroupCount"`
	Tags           string `json:"Tags" yaml:"Tags"`
}

func (b *Blueprint) DataAsStringArray() []string {
	return []string{b.Name, b.Description, b.HDPVersion, b.HostgroupCount, b.Tags}
}

type blueprintsClient interface {
	PostPrivateBlueprint(params *blueprints.PostPrivateBlueprintParams) (*blueprints.PostPrivateBlueprintOK, error)
	PostPublicBlueprint(params *blueprints.PostPublicBlueprintParams) (*blueprints.PostPublicBlueprintOK, error)
	GetPrivatesBlueprint(params *blueprints.GetPrivatesBlueprintParams) (*blueprints.GetPrivatesBlueprintOK, error)
	DeletePrivateBlueprint(params *blueprints.DeletePrivateBlueprintParams) error
}

func ListBlueprints(c *cli.Context) {
	checkRequiredFlags(c)
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	listBlueprintsImpl(cbClient.Cloudbreak.Blueprints, output.WriteList)
}

func listBlueprintsImpl(client blueprintsClient, writer func([]string, []utils.Row)) {
	defer utils.TimeTrack(time.Now(), "get public blueprints")
	log.Infof("[ListBlueprints] sending blueprint list request")
	resp, err := client.GetPrivatesBlueprint(blueprints.NewGetPrivatesBlueprintParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	for _, blueprint := range resp.Payload {
		jsonRoot := decodeAndParseToJson(blueprint.AmbariBlueprint)
		blueprintsNode := jsonRoot["Blueprints"].(map[string]interface{})
		row := &Blueprint{
			Name:           *blueprint.Name,
			Description:    *blueprint.Description,
			HDPVersion:     fmt.Sprintf("%v", blueprintsNode["stack_version"]),
			HostgroupCount: fmt.Sprint(blueprint.HostGroupCount),
			Tags:           blueprint.Status,
		}
		tableRows = append(tableRows, row)
	}

	writer(BlueprintHeader, tableRows)
}

func decodeAndParseToJson(encodedBlueprint string) map[string]interface{} {
	log.Debugf("[ListBlueprints] decoding blueprint from base64")
	b, err := base64.StdEncoding.DecodeString(encodedBlueprint)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Debugf("[ListBlueprints] parse blueprint to JSON")
	var blueprintJson interface{}
	json.Unmarshal(b, &blueprintJson)
	return blueprintJson.(map[string]interface{})
}

func CreateBlueprintFromUrl(c *cli.Context) {
	checkRequiredFlags(c)
	log.Infof("[CreateBlueprint] creating blueprint from a URL")
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
	log.Infof("[CreateBlueprint] creating blueprint from a file")
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	fileLocation := c.String(FlBlueprintFileLocation.Name)
	createBlueprintImpl(
		cbClient.Cloudbreak.Blueprints,
		c.String(FlName.Name),
		c.String(FlDescription.Name),
		c.Bool(FlPublic.Name),
		utils.ReadFile(fileLocation))
}

func createBlueprintImpl(client blueprintsClient, name string, description string, public bool, ambariBlueprint []byte) {
	defer utils.TimeTrack(time.Now(), "create blueprint")
	bpRequest := &models_cloudbreak.BlueprintRequest{
		Name:            &name,
		Description:     &description,
		AmbariBlueprint: base64.StdEncoding.EncodeToString(ambariBlueprint),
		Inputs:          make([]*models_cloudbreak.BlueprintParameter, 0),
	}

	if public {
		log.Infof("[CreateBlueprint] sending create public blueprint request")
		resp, err := client.PostPublicBlueprint(blueprints.NewPostPublicBlueprintParams().WithBody(bpRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		log.Infof("[CreateBlueprint] private blueprint created: %s (id: %d)", *resp.Payload.Name, resp.Payload.ID)
	} else {
		log.Infof("[CreateBlueprint] sending create private blueprint request")
		resp, err := client.PostPrivateBlueprint(blueprints.NewPostPrivateBlueprintParams().WithBody(bpRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		log.Infof("[CreateBlueprint] private blueprint created: %s (id: %d)", *resp.Payload.Name, resp.Payload.ID)
	}
}

func DeleteBlueprint(c *cli.Context) {
	checkRequiredFlags(c)
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	deleteBlueprintsImpl(cbClient.Cloudbreak.Blueprints, c.String(FlName.Name))
}

func deleteBlueprintsImpl(client blueprintsClient, name string) {
	defer utils.TimeTrack(time.Now(), "delete blueprint")
	log.Infof("[DeleteBlueprint] sending delete blueprint request with name: %s", name)
	err := client.DeletePrivateBlueprint(blueprints.NewDeletePrivateBlueprintParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteBlueprint] blueprint deleted, name: %s", name)
}
