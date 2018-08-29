package cli

import (
	"strconv"
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

type orgListOutDescribe struct {
	*orgListOut
	ID string `json:"ID" yaml:"ID"`
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

func (o *orgListOutDescribe) DataAsStringArray() []string {
	return append(o.orgListOut.DataAsStringArray(), o.ID)
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
	defer utils.TimeTrack(time.Now(), "delete organization")
	log.Infof("[DeleteOrgs] Delete an organization from a tenant")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	orgName := c.String(FlName.Name)
	_, err := cbClient.Cloudbreak.V3organizations.DeleteOrganizationByName(v3organizations.NewDeleteOrganizationByNameParams().WithName(orgName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[DeleteOrg] organization deleted: %s", orgName)
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
	tableRows = append(tableRows, &orgListOutDescribe{&orgListOut{resp.Payload}, strconv.FormatInt(resp.Payload.ID, 10)})
	output.WriteList(append(orgListHeader, "ID"), tableRows)
}

func ListOrgs(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list organizations")
	log.Infof("[ListOrgs] List all organizations in a tenant")
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	tableRows := []utils.Row{}
	for _, org := range GetOrgList(c) {
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

func RemoveUser(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "remove user from organization")
	log.Infof("[RemoveUser] Remove user from organization")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	userID := c.String(FlUserID.Name)
	orgName := c.String(FlName.Name)
	_, err := cbClient.Cloudbreak.V3organizations.RemoveOrganizationUsers(v3organizations.NewRemoveOrganizationUsersParams().WithName(orgName).WithBody([]string{userID}))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[RemoveUser] user removed from organization: %s", orgName)
}

func AddReadUser(c *cli.Context) {
	addUser(c, []string{"ALL:READ"})
}

func AddReadWriteUser(c *cli.Context) {
	addUser(c, []string{"ALL:READ", "ALL:WRITE"})
}

func addUser(c *cli.Context, permissions []string) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "add user to organization")
	log.Infof("[AddUser] add user to organization")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	userID := c.String(FlUserID.Name)
	orgName := c.String(FlName.Name)

	changeUsersJSON := &models_cloudbreak.ChangeOrganizationUsersJSON{
		UserID:      userID,
		Permissions: permissions,
	}

	_, err := cbClient.Cloudbreak.V3organizations.AddOrganizationUsers(v3organizations.NewAddOrganizationUsersParams().WithName(orgName).WithBody([]*models_cloudbreak.ChangeOrganizationUsersJSON{changeUsersJSON}))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[AddUser] user added to organization: %s", orgName)
}
