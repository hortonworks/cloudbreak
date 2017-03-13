package cli

import (
	"testing"

	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func TestCleanupBlueprintsImplNotGenerated(t *testing.T) {
	items := make([]*models_cloudbreak.BlueprintResponse, 0)
	items = append(items, &models_cloudbreak.BlueprintResponse{Name: "name"})
	items = append(items, &models_cloudbreak.BlueprintResponse{Name: "name2"})
	getItems := func() []*models_cloudbreak.BlueprintResponse {
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
	items := make([]*models_cloudbreak.BlueprintResponse, 0)
	item := models_cloudbreak.BlueprintResponse{
		Name: "bname",
		AmbariBlueprint: models_cloudbreak.AmbariBlueprint{
			Blueprint: models_cloudbreak.Blueprint{
				Name: &(&stringWrapper{"name"}).s,
			},
		},
	}
	items = append(items, &item)
	getItems := func() []*models_cloudbreak.BlueprintResponse {
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
	items := make([]*models_cloudbreak.BlueprintResponse, 0)
	item := models_cloudbreak.BlueprintResponse{
		Name: "bname",
		AmbariBlueprint: models_cloudbreak.AmbariBlueprint{
			Blueprint: models_cloudbreak.Blueprint{
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
	getItems := func() []*models_cloudbreak.BlueprintResponse {
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
	items := make([]*models_cloudbreak.TemplateResponse, 0)
	items = append(items, &models_cloudbreak.TemplateResponse{Name: "name"})
	getItems := func() []*models_cloudbreak.TemplateResponse {
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
	items := make([]*models_cloudbreak.TemplateResponse, 0)
	items = append(items, &models_cloudbreak.TemplateResponse{Name: "asd_mtempl_qwe"})
	items = append(items, &models_cloudbreak.TemplateResponse{Name: "qwe_wtempl_asd"})
	getItems := func() []*models_cloudbreak.TemplateResponse {
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
	items := make([]*models_cloudbreak.CredentialResponse, 0)
	items = append(items, &models_cloudbreak.CredentialResponse{Name: "name"})
	items = append(items, &models_cloudbreak.CredentialResponse{Name: "longname"})
	getItems := func() []*models_cloudbreak.CredentialResponse {
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
	items := make([]*models_cloudbreak.CredentialResponse, 0)
	items = append(items, &models_cloudbreak.CredentialResponse{Name: "credname"})
	getItems := func() []*models_cloudbreak.CredentialResponse {
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
	items := make([]*models_cloudbreak.NetworkResponse, 0)
	items = append(items, &models_cloudbreak.NetworkResponse{Name: "net"})
	items = append(items, &models_cloudbreak.NetworkResponse{Name: "netn"})
	items = append(items, &models_cloudbreak.NetworkResponse{Name: "longnetname"})
	getItems := func() []*models_cloudbreak.NetworkResponse {
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
	items := make([]*models_cloudbreak.NetworkResponse, 0)
	items = append(items, &models_cloudbreak.NetworkResponse{Name: "network"})
	getItems := func() []*models_cloudbreak.NetworkResponse {
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
	items := make([]*models_cloudbreak.SecurityGroupResponse, 0)
	items = append(items, &models_cloudbreak.SecurityGroupResponse{Name: "secg"})
	items = append(items, &models_cloudbreak.SecurityGroupResponse{Name: "secgn"})
	items = append(items, &models_cloudbreak.SecurityGroupResponse{Name: "longsecgname"})
	getItems := func() []*models_cloudbreak.SecurityGroupResponse {
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
	items := make([]*models_cloudbreak.SecurityGroupResponse, 0)
	items = append(items, &models_cloudbreak.SecurityGroupResponse{Name: "hdc-sg-master-"})
	getItems := func() []*models_cloudbreak.SecurityGroupResponse {
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
