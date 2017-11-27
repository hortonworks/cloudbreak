package cli

import (
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v2connectors"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var regionsHeader = []string{"Name", "Description"}
var availabilityZonesHeader = []string{"Name"}

type regionsOut struct {
	Name        string `json:"Name" yaml:"Name"`
	Description string `json:"Description" yaml:"Description"`
}

func (r *regionsOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description}
}

type availabilityZonesOut struct {
	Name string `json:"Name" yaml:"Name"`
}

func (r *availabilityZonesOut) DataAsStringArray() []string {
	return []string{r.Name}
}

func ListRegions(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list regions")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listRegionsImpl(cbClient.Cloudbreak.V2connectors, output.WriteList, c.String(FlCredential.Name))
}

func ListAvailabilityZones(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list availability zones")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listAvailabilityZonesImpl(cbClient.Cloudbreak.V2connectors, output.WriteList, c.String(FlCredential.Name), c.String(FlRegion.Name))
}

type getConnectorsClient interface {
	GetRegionsByCredentialID(*v2connectors.GetRegionsByCredentialIDParams) (*v2connectors.GetRegionsByCredentialIDOK, error)
}

func listRegionsImpl(client getConnectorsClient, writer func([]string, []utils.Row), credentialName string) {
	log.Infof("[listRegionsImpl] sending regions list request")
	req := &models_cloudbreak.PlatformResourceRequestJSON{
		CredentialName: credentialName,
	}
	regionsResp, err := client.GetRegionsByCredentialID(v2connectors.NewGetRegionsByCredentialIDParams().WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for name, dispname := range regionsResp.Payload.DisplayNames {
		tableRows = append(tableRows, &regionsOut{name, dispname})
	}

	writer(regionsHeader, tableRows)
}

func listAvailabilityZonesImpl(client getConnectorsClient, writer func([]string, []utils.Row), credentialName string, region string) {
	log.Infof("[listAvailabilityZonesImpl] sending availability zones list request")
	req := &models_cloudbreak.PlatformResourceRequestJSON{
		CredentialName: credentialName,
	}
	regionsResp, err := client.GetRegionsByCredentialID(v2connectors.NewGetRegionsByCredentialIDParams().WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, zone := range regionsResp.Payload.AvailabilityZones[region] {
		tableRows = append(tableRows, &availabilityZonesOut{zone})
	}

	writer(availabilityZonesHeader, tableRows)
}
