package audit

import (
	"regexp"
	"strings"
	"testing"

	v4audit "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_audits"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/dp-cli-common/utils"
)

type mockAuditClient struct {
}

func (*mockAuditClient) GetAuditEventsInWorkspace(params *v4audit.GetAuditEventsInWorkspaceParams) (*v4audit.GetAuditEventsInWorkspaceOK, error) {
	resp := []*model.AuditEventV4Response{
		{
			AuditID:  1,
			Duration: 2,
			Operation: &model.OperationDetails{
				CloudbreakVersion: "2.8.0-rc.3-1-ge4975d5",
				EventType:         "REST",
				ResourceID:        1378,
				ResourceType:      "credentials",
				Timestamp:         1544020242298,
				UserID:            "7ee3cc26-759b-4c27-8f56-4d2917216b39",
				UserName:          "admin@example.com",
			},
			Status: "OK - 200",
		},
	}
	return &v4audit.GetAuditEventsInWorkspaceOK{Payload: &model.AuditEventV4Responses{Responses: resp}}, nil
}

func (*mockAuditClient) GetAuditEventByWorkspace(params *v4audit.GetAuditEventByWorkspaceParams) (*v4audit.GetAuditEventByWorkspaceOK, error) {
	resp := &model.AuditEventV4Response{
		AuditID:  1,
		Duration: 2,
		Operation: &model.OperationDetails{
			CloudbreakVersion: "2.8.0-rc.3-1-ge4975d5",
			EventType:         "REST",
			ResourceID:        1378,
			ResourceType:      "credentials",
			Timestamp:         1535460683694,
			UserID:            "7ee3cc26-759b-4c27-8f56-4d2917216b39",
			UserName:          "admin@example.com",
		},
		RawFlowEvent: &model.StructuredFlowEvent{
			BlueprintDetails: &model.BlueprintDetails{
				BlueprintJSON: "long blueprint text",
			},
		},
		Status: "OK - 200",
	}
	return &v4audit.GetAuditEventByWorkspaceOK{Payload: resp}, nil
}

func TestListAuditsImpl(t *testing.T) {
	var rows []utils.Row
	listAuditsImpl(new(mockAuditClient), 1, "credentials", "1378", func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}

	for _, r := range rows {
		expected := "1 REST 2018-12-05T14:30:42Z+00:00 1378  credentials admin@example.com OK - 200 2"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

func TestListAuditsImplWithWrongResourceID(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			if r != "Unable to parse as number: ABCD" {
				t.Errorf("error not match 'Unable to parse as number' == %s", r)
			}
		}
	}()
	listAuditsImpl(new(mockAuditClient), 1, "credentials", "ABCD", func(h []string, r []utils.Row) {})

	t.Error("Exit not happened")
}

func TestDescribeAuditImpl(t *testing.T) {
	var rows []utils.Row
	describeAuditImpl(new(mockAuditClient), 1, "1234", func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	expected := regexp.MustCompile("blueprintjson: '---TRUNCATED---'")
	for _, r := range rows {
		if expected.FindString(strings.Join(r.DataAsStringArray(), "")) == "" {
			t.Errorf("TRUNCATED text not found in %s", r.DataAsStringArray())
		}
	}
}

func TestDescribeAuditImplWithWrongResourceID(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			if r != "Unable to parse as number: ABCD" {
				t.Errorf("error not match 'Unable to parse as number' == %s", r)
			}
		}
	}()
	describeAuditImpl(new(mockAuditClient), 1, "ABCD", func(h []string, r []utils.Row) {})

	t.Error("Exit not happened")
}
