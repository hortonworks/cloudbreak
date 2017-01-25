package cli

import (
	"testing"

	"github.com/hortonworks/hdc-cli/models"
)

func TestCleanupBlueprintsImplNotGenerated(t *testing.T) {
	items := make([]*models.BlueprintResponse, 0)
	items = append(items, &models.BlueprintResponse{Name: "name"})
	items = append(items, &models.BlueprintResponse{Name: "name2"})
	getItems := func() []*models.BlueprintResponse {
		return items
	}

	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupBlueprintsImpl(getItems, doDelete)

	if len(deleteCalled) != 0 {
		t.Errorf("delete blueprint called for %s", deleteCalled)
	}
}

func TestCleanupBlueprintsImplNotGeneratedButStartsWithB(t *testing.T) {
	items := make([]*models.BlueprintResponse, 0)
	item := models.BlueprintResponse{
		Name: "bname",
		AmbariBlueprint: models.AmbariBlueprint{
			Blueprint: models.Blueprint{
				Name: &(&stringWrapper{"name"}).s,
			},
		},
	}
	items = append(items, &item)
	getItems := func() []*models.BlueprintResponse {
		return items
	}

	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupBlueprintsImpl(getItems, doDelete)

	if len(deleteCalled) != 0 {
		t.Errorf("delete blueprint called for %s", deleteCalled)
	}
}

func TestCleanupBlueprintsImplGenerated(t *testing.T) {
	items := make([]*models.BlueprintResponse, 0)
	item := models.BlueprintResponse{
		Name: "bname",
		AmbariBlueprint: models.AmbariBlueprint{
			Blueprint: models.Blueprint{
				Name: func() *string {
					for k, _ := range BlueprintMap {
						return &k
					}
					return nil
				}(),
			},
		},
	}
	items = append(items, &item)
	getItems := func() []*models.BlueprintResponse {
		return items
	}

	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupBlueprintsImpl(getItems, doDelete)

	if len(deleteCalled) != len(items) {
		t.Errorf("delete blueprint not match %d == %d", len(items), len(deleteCalled))
	}
}

func TestCleanupTemplatesImplNotGenerated(t *testing.T) {
	items := make([]*models.TemplateResponse, 0)
	items = append(items, &models.TemplateResponse{Name: "name"})
	getItems := func() []*models.TemplateResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupTemplatesImpl(getItems, doDelete)

	if len(deleteCalled) != 0 {
		t.Errorf("delete template called for %s", deleteCalled)
	}
}

func TestCleanupTemplatesImplGenerated(t *testing.T) {
	items := make([]*models.TemplateResponse, 0)
	items = append(items, &models.TemplateResponse{Name: "asd_mtempl_qwe"})
	items = append(items, &models.TemplateResponse{Name: "qwe_wtempl_asd"})
	getItems := func() []*models.TemplateResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupTemplatesImpl(getItems, doDelete)

	if len(deleteCalled) != len(items) {
		t.Errorf("delete template not match %d == %d", len(items), len(deleteCalled))
	}
}

func TestCleanupCredentialsImplNotGenerated(t *testing.T) {
	items := make([]*models.CredentialResponse, 0)
	items = append(items, &models.CredentialResponse{Name: "name"})
	items = append(items, &models.CredentialResponse{Name: "longname"})
	getItems := func() []*models.CredentialResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupCredentialsImpl(getItems, doDelete)

	if len(deleteCalled) != 0 {
		t.Errorf("delete credentials called for %s", deleteCalled)
	}
}

func TestCleanupCredentialsImplGenerated(t *testing.T) {
	items := make([]*models.CredentialResponse, 0)
	items = append(items, &models.CredentialResponse{Name: "credname"})
	getItems := func() []*models.CredentialResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupCredentialsImpl(getItems, doDelete)

	if len(deleteCalled) != len(items) {
		t.Errorf("delete credentials not match %d == %d", len(items), len(deleteCalled))
	}
}

func TestCleanupNetworksImplNotGenerated(t *testing.T) {
	items := make([]*models.NetworkResponse, 0)
	items = append(items, &models.NetworkResponse{Name: "net"})
	items = append(items, &models.NetworkResponse{Name: "netn"})
	items = append(items, &models.NetworkResponse{Name: "longnetname"})
	getItems := func() []*models.NetworkResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupNetworksImpl(getItems, doDelete)

	if len(deleteCalled) != 0 {
		t.Errorf("delete network called for %s", deleteCalled)
	}
}

func TestCleanupNetworksImplGenerated(t *testing.T) {
	items := make([]*models.NetworkResponse, 0)
	items = append(items, &models.NetworkResponse{Name: "network"})
	getItems := func() []*models.NetworkResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupNetworksImpl(getItems, doDelete)

	if len(deleteCalled) != len(items) {
		t.Errorf("delete network not match %d == %d", len(items), len(deleteCalled))
	}
}

func TestCleanupSecurityGroupImplNotGenerated(t *testing.T) {
	items := make([]*models.SecurityGroupResponse, 0)
	items = append(items, &models.SecurityGroupResponse{Name: "secg"})
	items = append(items, &models.SecurityGroupResponse{Name: "secgn"})
	items = append(items, &models.SecurityGroupResponse{Name: "longsecgname"})
	getItems := func() []*models.SecurityGroupResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupSecurityGroupsImpl(getItems, doDelete)

	if len(deleteCalled) != 0 {
		t.Errorf("delete network called for %s", deleteCalled)
	}
}

func TestCleanupSecurityGroupImplGenerated(t *testing.T) {
	items := make([]*models.SecurityGroupResponse, 0)
	items = append(items, &models.SecurityGroupResponse{Name: "hdc-sg-master-"})
	getItems := func() []*models.SecurityGroupResponse {
		return items
	}
	var deleteCalled []string
	doDelete := func(name string) error {
		deleteCalled = append(deleteCalled, name)
		return nil
	}

	cleanupSecurityGroupsImpl(getItems, doDelete)

	if len(deleteCalled) != len(items) {
		t.Errorf("delete network not match %d == %d", len(items), len(deleteCalled))
	}
}
