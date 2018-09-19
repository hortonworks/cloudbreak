package connectors

import (
	"github.com/hortonworks/cb-cli/cloudbreak/oauth"
	"sort"
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v1connectors"
	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v2connectors"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/cloud"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/urfave/cli"
)

var regionsHeader = []string{"Name", "Description"}
var diskHeader = []string{"Name", "Description"}
var availabilityZonesHeader = []string{"Name"}
var instanceHeader = []string{"Name", "Cpu", "Memory", "AvailabilityZone"}

type regionsOut struct {
	Name        string `json:"Name" yaml:"Name"`
	Description string `json:"Description" yaml:"Description"`
}

type diskOut struct {
	Name        string `json:"Name" yaml:"Name"`
	Description string `json:"Description" yaml:"Description"`
}

type availabilityZonesOut struct {
	Name string `json:"Name" yaml:"Name"`
}

type instanceOut struct {
	Name             string `json:"Name" yaml:"Name"`
	Cpu              string `json:"Cpu" yaml:"Cpu"`
	Memory           string `json:"Memory" yaml:"Memory"`
	AvailabilityZone string `json:"AvailabilityZone" yaml:"AvailabilityZone"`
}

type hasName interface {
	GetName() string
}

func (r *regionsOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description}
}

func (r *regionsOut) GetName() string {
	return r.Name
}

func (r *diskOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description}
}

func (r *diskOut) GetName() string {
	return r.Name
}

func (r *availabilityZonesOut) DataAsStringArray() []string {
	return []string{r.Name}
}

func (r *availabilityZonesOut) GetName() string {
	return r.Name
}

func (r *instanceOut) DataAsStringArray() []string {
	return []string{r.Name, r.Cpu, r.Memory, r.AvailabilityZone}
}

func (r *instanceOut) GetName() string {
	return r.Name
}

func ListRegions(c *cli.Context) {
	fl.CheckRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list regions")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listRegionsImpl(cbClient.Cloudbreak.V2connectors, output.WriteList, c.String(fl.FlCredential.Name))
}

func ListAvailabilityZones(c *cli.Context) {
	fl.CheckRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list availability zones")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listAvailabilityZonesImpl(cbClient.Cloudbreak.V2connectors, output.WriteList, c.String(fl.FlCredential.Name), c.String(fl.FlRegion.Name))
}

func ListAwsVolumeTypes(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	listVolumeTypes(c)
}

func ListAzureVolumeTypes(c *cli.Context) {
	cloud.SetProviderType(cloud.AZURE)
	listVolumeTypes(c)
}

func ListGcpVolumeTypes(c *cli.Context) {
	cloud.SetProviderType(cloud.GCP)
	listVolumeTypes(c)
}

func listVolumeTypes(c *cli.Context) {
	fl.CheckRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list volume types")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listVolumeTypesImpl(cbClient.Cloudbreak.V1connectors, output.WriteList)
}

func ListInstanceTypes(c *cli.Context) {
	fl.CheckRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list instance types")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listInstanceTypesImpl(cbClient.Cloudbreak.V2connectors, output.WriteList, c.String(fl.FlCredential.Name), c.String(fl.FlRegion.Name), c.String(fl.FlAvailabilityZoneOptional.Name))
}

type getConnectorsClient interface {
	GetRegionsByCredentialID(*v2connectors.GetRegionsByCredentialIDParams) (*v2connectors.GetRegionsByCredentialIDOK, error)
}

type getDiskTypesClient interface {
	GetDisktypes(*v1connectors.GetDisktypesParams) (*v1connectors.GetDisktypesOK, error)
}

type getInstanceTypesClient interface {
	GetVMTypesByCredentialID(*v2connectors.GetVMTypesByCredentialIDParams) (*v2connectors.GetVMTypesByCredentialIDOK, error)
}

func listRegionsImpl(client getConnectorsClient, writer func([]string, []utils.Row), credentialName string) {
	log.Infof("[listRegionsImpl] sending regions list request")
	req := &model.PlatformResourceRequestJSON{
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
	sortByName(tableRows)
	writer(regionsHeader, tableRows)
}

func listAvailabilityZonesImpl(client getConnectorsClient, writer func([]string, []utils.Row), credentialName string, region string) {
	log.Infof("[listAvailabilityZonesImpl] sending availability zones list request")
	req := &model.PlatformResourceRequestJSON{
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
	sortByName(tableRows)
	writer(availabilityZonesHeader, tableRows)
}

func listVolumeTypesImpl(client getDiskTypesClient, writer func([]string, []utils.Row)) {
	log.Infof("[listVolumeTypesImpl] sending volume type list request")
	provider := cloud.GetProvider().GetName()
	volumeResp, err := client.GetDisktypes(v1connectors.NewGetDisktypesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	tableRows := []utils.Row{}
	for t, desc := range volumeResp.Payload.DisplayNames[*provider] {
		tableRows = append(tableRows, &diskOut{t, desc})
	}
	sortByName(tableRows)
	writer(diskHeader, tableRows)
}

func listInstanceTypesImpl(client getInstanceTypesClient, writer func([]string, []utils.Row), credentialName string, region string, avzone string) {
	log.Infof("[listInstanceTypesImpl] sending instance type list request")
	req := &model.PlatformResourceRequestJSON{
		CredentialName: credentialName,
		Region:         region,
	}
	instanceResp, err := client.GetVMTypesByCredentialID(v2connectors.NewGetVMTypesByCredentialIDParams().WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	machines := instanceResp.Payload.VMTypes[avzone]
	if len(machines.VirtualMachines) == 0 {
		machines = instanceResp.Payload.VMTypes[region]
	}
	if len(machines.VirtualMachines) == 0 && len(avzone) == 0 {
		for k := range instanceResp.Payload.VMTypes {
			if strings.HasPrefix(k, region) {
				avzone = k
				machines = instanceResp.Payload.VMTypes[k]
				break
			}
		}
	}

	tableRows := []utils.Row{}
	for _, instance := range machines.VirtualMachines {
		tableRows = append(tableRows, &instanceOut{instance.Value, utils.SafeStringTypeAssert(instance.VMTypeMetaJSON.Properties["Cpu"]), utils.SafeStringTypeAssert(instance.VMTypeMetaJSON.Properties["Memory"]), avzone})
	}
	sortByName(tableRows)
	writer(instanceHeader, tableRows)
}

func sortByName(rows []utils.Row) {
	sort.Slice(rows, func(i, j int) bool {
		vi, oki := rows[i].(hasName)
		vj, okj := rows[j].(hasName)
		if !oki || !okj {
			return i < j
		}
		return vi.GetName() < vj.GetName()
	})
}
