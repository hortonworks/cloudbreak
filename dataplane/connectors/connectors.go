package connectors

import (
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"sort"
	"strings"
	"time"

	v4con "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_connectors"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
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
	defer utils.TimeTrack(time.Now(), "list regions")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listRegionsImpl(cbClient.Cloudbreak.V4WorkspaceIDConnectors, output.WriteList, c.String(fl.FlCredential.Name), c.Int64(fl.FlWorkspaceOptional.Name))
}

func ListAvailabilityZones(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list availability zones")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listAvailabilityZonesImpl(cbClient.Cloudbreak.V4WorkspaceIDConnectors, output.WriteList, c.String(fl.FlCredential.Name), c.String(fl.FlRegion.Name), c.Int64(fl.FlWorkspaceOptional.Name))
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
	defer utils.TimeTrack(time.Now(), "list volume types")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listVolumeTypesImpl(cbClient.Cloudbreak.V4WorkspaceIDConnectors, output.WriteList, c.Int64(fl.FlWorkspaceOptional.Name))
}

func ListInstanceTypes(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list instance types")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listInstanceTypesImpl(cbClient.Cloudbreak.V4WorkspaceIDConnectors, output.WriteList, c.String(fl.FlCredential.Name), c.String(fl.FlRegion.Name), c.String(fl.FlAvailabilityZoneOptional.Name), c.Int64(fl.FlWorkspaceOptional.Name))
}

type getConnectorsClient interface {
	GetRegionsByCredentialAndWorkspace(*v4con.GetRegionsByCredentialAndWorkspaceParams) (*v4con.GetRegionsByCredentialAndWorkspaceOK, error)
}

type getDiskTypesClient interface {
	GetDisktypesForWorkspace(*v4con.GetDisktypesForWorkspaceParams) (*v4con.GetDisktypesForWorkspaceOK, error)
}

type getInstanceTypesClient interface {
	GetVMTypesByCredentialAndWorkspace(*v4con.GetVMTypesByCredentialAndWorkspaceParams) (*v4con.GetVMTypesByCredentialAndWorkspaceOK, error)
}

func listRegionsImpl(client getConnectorsClient, writer func([]string, []utils.Row), credentialName string, workspaceId int64) {
	log.Infof("[listRegionsImpl] sending regions list request")
	regionsResp, err := client.GetRegionsByCredentialAndWorkspace(v4con.NewGetRegionsByCredentialAndWorkspaceParams().WithWorkspaceID(workspaceId).WithCredentialName(&credentialName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for name, dispname := range regionsResp.Payload.DisplayNames {
		tableRows = append(tableRows, &regionsOut{name, dispname})
	}
	sortByName(tableRows)
	writer(regionsHeader, tableRows)
}

func listAvailabilityZonesImpl(client getConnectorsClient, writer func([]string, []utils.Row), credentialName string, region string, workspaceId int64) {
	log.Infof("[listAvailabilityZonesImpl] sending availability zones list request")
	regionsResp, err := client.GetRegionsByCredentialAndWorkspace(v4con.NewGetRegionsByCredentialAndWorkspaceParams().WithWorkspaceID(workspaceId).WithCredentialName(&credentialName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, zone := range regionsResp.Payload.AvailabilityZones[region] {
		tableRows = append(tableRows, &availabilityZonesOut{zone})
	}
	sortByName(tableRows)
	writer(availabilityZonesHeader, tableRows)
}

func listVolumeTypesImpl(client getDiskTypesClient, writer func([]string, []utils.Row), workspaceId int64) {
	log.Infof("[listVolumeTypesImpl] sending volume type list request")
	provider := cloud.GetProvider().GetName()
	volumeResp, err := client.GetDisktypesForWorkspace(v4con.NewGetDisktypesForWorkspaceParams().WithWorkspaceID(workspaceId))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	for t, desc := range volumeResp.Payload.DisplayNames[*provider] {
		tableRows = append(tableRows, &diskOut{t, desc})
	}
	sortByName(tableRows)
	writer(diskHeader, tableRows)
}

func listInstanceTypesImpl(client getInstanceTypesClient, writer func([]string, []utils.Row), credentialName string, region string, avzone string, workspaceId int64) {
	log.Infof("[listInstanceTypesImpl] sending instance type list request")
	instanceResp, err := client.GetVMTypesByCredentialAndWorkspace(v4con.NewGetVMTypesByCredentialAndWorkspaceParams().WithWorkspaceID(workspaceId).WithCredentialName(&credentialName).WithRegion(&region))
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

	var tableRows []utils.Row
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
