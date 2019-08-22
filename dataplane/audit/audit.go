package audit

import (
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	v4audit "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_audits"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
)

var auditListHeader = []string{"AuditID", "EventType", "TimeStamp", "ResourceId", "ResourceName", "ResourceType", "UserName", "Status", "Duration"}

type auditListOut struct {
	Audit *model.AuditEventV4Response `json:"audit" yaml:"audit"`
}

func (a *auditListOut) DataAsStringArray() []string {
	return []string{strconv.FormatInt(a.Audit.AuditID, 10), a.Audit.Operation.EventType, convertToDateTimeString(a.Audit.Operation.Timestamp), strconv.FormatInt(a.Audit.Operation.ResourceID, 10), a.Audit.Operation.ResourceName, a.Audit.Operation.ResourceType, a.Audit.Operation.UserName, a.Audit.Status, strconv.FormatInt(a.Audit.Duration, 10)}
}

func convertToDateTimeString(t int64) string {
	loc := time.FixedZone("UTC", 0)
	return time.Unix(int64(t/1000), 0).In(loc).Format("2006-01-02T15:04:05Z-07:00")
}

var auditHeader = []string{"Audit"}

type auditOut struct {
	Audit *model.AuditEventV4Response `json:"audit" yaml:"audit"`
}

type auditClient interface {
	GetAuditEventsInWorkspace(params *v4audit.GetAuditEventsInWorkspaceParams) (*v4audit.GetAuditEventsInWorkspaceOK, error)
	GetAuditEventByWorkspace(params *v4audit.GetAuditEventByWorkspaceParams) (*v4audit.GetAuditEventByWorkspaceOK, error)
}

func (a *auditOut) DataAsStringArray() []string {
	if a.Audit.RawFlowEvent != nil && a.Audit.RawFlowEvent.BlueprintDetails != nil {
		a.Audit.RawFlowEvent.BlueprintDetails.BlueprintJSON = "---TRUNCATED---"
	}
	auditYAML, err := yaml.Marshal(a.Audit)
	if err != nil {
		return []string{err.Error()}
	}
	return []string{string(auditYAML)}
}

func ListBlueprintAudits(c *cli.Context) {
	listAudits("blueprints", c)
}

func ListClusterAudits(c *cli.Context) {
	listAudits("stacks", c)
}

func ListCredentialAudits(c *cli.Context) {
	listAudits("credentials", c)
}

func ListDatabaseAudits(c *cli.Context) {
	listAudits("rdsconfigs", c)
}

func ListImagecatalogAudits(c *cli.Context) {
	listAudits("imagecatalogs", c)
}

func ListLdapAudits(c *cli.Context) {
	listAudits("ldapconfigs", c)
}

func ListRecipeAudits(c *cli.Context) {
	listAudits("recipes", c)
}

func listAudits(resourceType string, c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list audits")
	log.Infof("[ListAudits] List all audits for a resource identified by Resource ID")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	resourceID := c.String(fl.FlResourceID.Name)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	listAuditsImpl(cbClient.Cloudbreak.V4WorkspaceIDAudits, workspaceID, resourceType, resourceID, output.WriteList)
}

func listAuditsImpl(client auditClient, workspaceID int64, resourceType string, resourceIDString string, writer func([]string, []utils.Row)) {
	resourceID, err := strconv.ParseInt(resourceIDString, 10, 64)
	if err != nil {
		utils.LogErrorMessageAndExit("Unable to parse as number: " + resourceIDString)
	}
	resp, err := client.GetAuditEventsInWorkspace(v4audit.NewGetAuditEventsInWorkspaceParams().WithWorkspaceID(workspaceID).WithResourceType(&resourceType).WithResourceID(&resourceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	for _, audit := range resp.Payload.Responses {
		tableRows = append(tableRows, &auditListOut{audit})
	}
	writer(auditListHeader, tableRows)
}

func DescribeAudit(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe audit")
	log.Infof("[DescribeAudit] Show audit entry identified by Audit ID")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	auditID := c.String(fl.FlAuditID.Name)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	describeAuditImpl(cbClient.Cloudbreak.V4WorkspaceIDAudits, workspaceID, auditID, output.WriteList)
}

func describeAuditImpl(client auditClient, workspaceID int64, auditIDString string, writer func([]string, []utils.Row)) {
	auditID, err := strconv.ParseInt(auditIDString, 10, 64)
	if err != nil {
		utils.LogErrorMessageAndExit("Unable to parse as number: " + auditIDString)
	}
	resp, err := client.GetAuditEventByWorkspace(v4audit.NewGetAuditEventByWorkspaceParams().WithWorkspaceID(workspaceID).WithAuditID(auditID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	tableRows = append(tableRows, &auditOut{resp.Payload})
	writer(auditHeader, tableRows)
}
