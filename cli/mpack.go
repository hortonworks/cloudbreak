package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1mpacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"strings"
	"time"
)

type mpackClient interface {
	PostPublicManagementPack(params *v1mpacks.PostPublicManagementPackParams) (*v1mpacks.PostPublicManagementPackOK, error)
	PostPrivateManagementPack(params *v1mpacks.PostPrivateManagementPackParams) (*v1mpacks.PostPrivateManagementPackOK, error)
	GetPublicManagementPacks(params *v1mpacks.GetPublicManagementPacksParams) (*v1mpacks.GetPublicManagementPacksOK, error)
}

var mpackHeader = []string{"Name", "Description", "URL", "Purge", "PurgeList", "Force"}

type mpack struct {
	Name        string `json:"Name" yaml:"Name"`
	Description string `json:"Description" yaml:"Description"`
	URL         string `json:"URL" yaml:"URL"`
	Purge       string `json:"Purge" yaml:"Purge"`
	PurgeList   string `json:"PurgeList" yaml:"PurgeList"`
	Force       string `json:"Force" yaml:"Force"`
}

func (m *mpack) DataAsStringArray() []string {
	return []string{m.Name, m.Description, m.URL, m.Purge, m.PurgeList, m.Force}
}

func CreateMpack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createMpackImpl(
		cbClient.Cloudbreak.V1mpacks,
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		c.String(FLMpackURL.Name),
		c.Bool(FLMpackPurge.Name),
		c.String(FLMpackPurgeList.Name),
		c.Bool(FLMpackForce.Name),
		c.Bool(FlPublicOptional.Name))
}

func createMpackImpl(client mpackClient, name, description, url string, purge bool, purgeList string, force, public bool) {
	defer utils.TimeTrack(time.Now(), "create management pack")
	req := &models_cloudbreak.MpackRequest{
		Name:        &name,
		Description: &description,
		MpackURL:    &url,
		Purge:       &purge,
		PurgeList:   strings.Split(purgeList, ","),
		Force:       &force,
	}

	var mpackResponse *models_cloudbreak.MpackResponse
	if public {
		log.Infof("[createMpackImpl] sending create public management pack request with name: %s", name)
		resp, err := client.PostPublicManagementPack(v1mpacks.NewPostPublicManagementPackParams().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		mpackResponse = resp.Payload
	} else {
		log.Infof("[createMpackImpl] sending create private management pack request with name: %s", name)
		resp, err := client.PostPrivateManagementPack(v1mpacks.NewPostPrivateManagementPackParams().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		mpackResponse = resp.Payload
	}
	log.Infof("[createMpackImpl] management pack created: %s (id: %d)", *mpackResponse.Name, mpackResponse.ID)
}

func DeleteMpack(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete a management pack")

	mpackName := c.String(FlName.Name)
	log.Infof("[DeleteMpack] delete management pack by name: %s", mpackName)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	if err := cbClient.Cloudbreak.V1mpacks.DeletePublicManagementPack(v1mpacks.NewDeletePublicManagementPackParams().WithName(mpackName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteMpack] management pack deleted: %s", mpackName)
	return nil
}

func ListMpacks(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list management pack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	return listMpacksImpl(cbClient.Cloudbreak.V1mpacks, output.WriteList)
}

func listMpacksImpl(mpackClient mpackClient, writer func([]string, []utils.Row)) error {
	resp, err := mpackClient.GetPublicManagementPacks(v1mpacks.NewGetPublicManagementPacksParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, m := range resp.Payload {
		purgeString := "false"
		forceString := "false"
		desc := ""
		if m.Purge != nil && *m.Purge {
			purgeString = "true"
		}
		if m.Force != nil && *m.Force {
			forceString = "true"
		}
		if m.Description != nil && len(*m.Description) > 0 {
			desc = *m.Description
		}
		row := &mpack{
			Name:        *m.Name,
			Description: desc,
			URL:         *m.MpackURL,
			Purge:       purgeString,
			PurgeList:   strings.Join(m.PurgeList, ","),
			Force:       forceString,
		}
		tableRows = append(tableRows, row)
	}

	writer(mpackHeader, tableRows)
	return nil
}
