package cli

import (
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1audits"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
	yaml "gopkg.in/yaml.v2"
)

var auditListHeader = []string{"AuditID", "EventType", "TimeStamp", "ResourceId", "ResourceName", "ResourceType", "UserName", "Status", "Duration"}

type auditListOut struct {
	Audit *models_cloudbreak.AuditEvent `json:"Audit" yaml:"Audit"`
}

func (a *auditListOut) DataAsStringArray() []string {
	return []string{strconv.FormatInt(a.Audit.AuditID, 10), a.Audit.Operation.EventType, a.Audit.Operation.ZonedDateTime.String(), strconv.FormatInt(a.Audit.Operation.ResourceID, 10), a.Audit.Operation.ResourceName, a.Audit.Operation.ResourceType, a.Audit.Operation.UserName, a.Audit.Status, strconv.FormatInt(a.Audit.Duration, 10)}
}

var auditHeader = []string{"Audit"}

type auditOut struct {
	Audit *models_cloudbreak.AuditEvent `json:"Audit" yaml:"Audit"`
}

func (a *auditOut) DataAsStringArray() []string {
	if a.Audit.RawFlowEvent.Blueprint != nil {
		a.Audit.RawFlowEvent.Blueprint.BlueprintJSON = "---TRUNCATED---"
	}
	auditYAML, err := yaml.Marshal(a.Audit)
	if err != nil {
		return []string{err.Error()}
	}
	return []string{string(auditYAML)}
}

func ListAudits(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list audits")
	log.Infof("[ListAudits] List all audits for a resource identified by Resource ID")
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	resourceID, err := strconv.ParseInt(c.String(FlResourceID.Name), 10, 64)
	if err != nil {
		utils.LogErrorMessageAndExit("Unable to parse as number: " + c.String(FlResourceID.Name))
	}
	resourceType := c.String(FlResourceType.Name)

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V1audits.GetAuditEvents(v1audits.NewGetAuditEventsParams().WithResourceType(resourceType).WithResourceID(resourceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, audit := range resp.Payload {
		tableRows = append(tableRows, &auditListOut{audit})
	}
	output.WriteList(auditListHeader, tableRows)

	// tableRows = append(tableRows, &auditListOut{strconv.FormatInt(audit.AuditID, 10),
	// 	audit.Operation.EventType, audit.Operation.ZonedDateTime.String(),
	// 	strconv.FormatInt(audit.Operation.ResourceID, 10), audit.Operation.ResourceName,
	// 	audit.Operation.ResourceType, audit.Operation.UserName, audit.Status, strconv.FormatInt(audit.Duration, 10)})
}

func DescribeAudit(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe audit")
	log.Infof("[DescribeAudit] Show audit entry identified by Audit ID")
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	auditID, err := strconv.ParseInt(c.String(FlAuditID.Name), 10, 64)
	if err != nil {
		utils.LogErrorMessageAndExit("Unable to parse as number: " + c.String(FlAuditID.Name))
	}

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V1audits.GetAuditEvent(v1audits.NewGetAuditEventParams().WithAuditID(auditID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	tableRows = append(tableRows, &auditOut{resp.Payload})

	output.Write(auditHeader, &auditOut{resp.Payload})
}
