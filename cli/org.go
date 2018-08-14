package cli

import (
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3organizations"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
	yaml "gopkg.in/yaml.v2"
)

var orgListHeader = []string{"Name", "Description", "Permissions"}

type orgListOut struct {
	Organization *models_cloudbreak.OrganizationResponse `json:"Organization" yaml:"Organization"`
}

func (o *orgListOut) DataAsStringArray() []string {
	permissionYAML, err := yaml.Marshal(o.Organization.Users)
	var permissionString string
	if err != nil {
		permissionString = err.Error()
	} else {
		permissionString = string(permissionYAML)
	}
	return []string{o.Organization.Name, utils.SafeStringConvert(o.Organization.Description), permissionString}
}

func CreateOrg(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create organization")
	log.Infof("[CreateOrgs] Create an organization into a tenant")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	orgName := c.String(FlName.Name)
	orgDesc := c.String(FlDescriptionOptional.Name)
	req := models_cloudbreak.OrganizationRequest{
		Name:        orgName,
		Description: &orgDesc,
	}

	_, err := cbClient.Cloudbreak.V3organizations.CreateOrganization(v3organizations.NewCreateOrganizationParams().WithBody(&req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[CreateOrg] organization created: %s", orgName)
}

func DeleteOrg(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create organization")
	log.Infof("[CreateOrgs] Create an organization into a tenant")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	orgName := c.String(FlName.Name)
	_, err := cbClient.Cloudbreak.V3organizations.DeleteOrganizationByName(v3organizations.NewDeleteOrganizationByNameParams().WithName(orgName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[CreateOrg] organization created: %s", orgName)
}

func DescribeOrg(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe organization")
	log.Infof("[DescribeOrg] Describes an organizations in a tenant")
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	orgName := c.String(FlName.Name)
	resp, err := cbClient.Cloudbreak.V3organizations.GetOrganizationByName(v3organizations.NewGetOrganizationByNameParams().WithName(orgName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	tableRows = append(tableRows, &orgListOut{resp.Payload})
	output.WriteList(orgListHeader, tableRows)
}

func ListOrgs(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list organizations")
	log.Infof("[ListOrgs] List all organizations in a tenant")
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3organizations.GetOrganizations(v3organizations.NewGetOrganizationsParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, org := range resp.Payload {
		tableRows = append(tableRows, &orgListOut{org})
	}
	output.WriteList(orgListHeader, tableRows)
}

func GetOrgIdByName(c *cli.Context, orgName string) int64 {
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3organizations.GetOrganizationByName(v3organizations.NewGetOrganizationByNameParams().WithName(orgName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	return resp.Payload.ID
}

func GetOrgList(c *cli.Context) []*models_cloudbreak.OrganizationResponse {
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3organizations.GetOrganizations(v3organizations.NewGetOrganizationsParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	return resp.Payload
}
