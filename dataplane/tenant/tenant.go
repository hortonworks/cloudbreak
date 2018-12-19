package tenant

import (
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/go-openapi/swag"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/tenants"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/model"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var tenantDetailsHeader = []string{"ID", "Name", "DisplayName", "State"}

type tenantsListOut struct {
	Tenant *model.Tenant
}

func (t *tenantsListOut) DataAsStringArray() []string {
	return []string{
		t.Tenant.ID.String(),
		swag.StringValue(t.Tenant.Name),
		swag.StringValue(t.Tenant.DisplayName),
		swag.StringValue(t.Tenant.State)}
}

type tenantClient interface {
	GetAllTenants(params *tenants.GetAllTenantsParams) (*tenants.GetAllTenantsOK, error)
	RegisterTenant(params *tenants.RegisterTenantParams) (*tenants.RegisterTenantOK, error)
	DisableTenant(params *tenants.DisableTenantParams) (*tenants.DisableTenantOK, error)
	ResendMail(params *tenants.ResendMailParams) (*tenants.ResendMailOK, error)
	RetrieveTenant(params *tenants.RetrieveTenantParams) (*tenants.RetrieveTenantOK, error)
}

func ListTenants(c *cli.Context) {
	log.Infof("[ListTenant] List information for avaliable tenants")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	tenantsResponse := listTenantsImpl(dpClient.Dataplane.Tenants)
	tableRows := []utils.Row{}
	for _, tenant := range tenantsResponse {
		tableRows = append(tableRows, &tenantsListOut{tenant})
	}
	output.WriteList(tenantDetailsHeader, tableRows)
}
func listTenantsImpl(client tenantClient) []*model.Tenant {
	defer utils.TimeTrack(time.Now(), "List Tenants")
	log.Infof("[listTenantsImpl] sending list tenants request")
	resp, err := client.GetAllTenants(tenants.NewGetAllTenantsParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func RegisterTenant(c *cli.Context) {
	log.Infof("[RegisterTenant] Register a new tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	tenantResponse := registerTenantImpl(
		dpClient.Dataplane.Tenants,
		c.String(fl.FlCaasTenantName.Name),
		c.String(fl.FlCaasTenantLabel.Name),
		c.String(fl.FlCaasTenantEmail.Name),
	)
	output.Write(tenantDetailsHeader, &tenantsListOut{tenantResponse})
}

func registerTenantImpl(client tenantClient, name string, label string, email string) *model.Tenant {
	defer utils.TimeTrack(time.Now(), "Register Tenant")

	tenantRequest := &model.TenantRegisterRequest{
		Name:  &name,
		Label: &label,
		Su: &model.SuperUser{
			Email:    &email,
			Name:     &name,
			Username: &email,
		},
	}
	log.Infof("[registerTenantImpl] sending register tenant request")
	resp, err := client.RegisterTenant(tenants.NewRegisterTenantParams().WithBody(tenantRequest).WithMethod("register"))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func ResendEMail(c *cli.Context) {
	log.Infof("[ResendEMail] Resend Mail to Tenant's super user")
	//Search if the tenant exist
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	tenantName := c.String(fl.FlCaasTenantName.Name)
	tenant := retrieveTenantImpl(
		dpClient.Dataplane.Tenants,
		tenantName)
	// check tenant status
	if swag.StringValue(tenant.State) == "CREATED" {
		log.Infof("Sending activation mail to %s ", swag.StringValue(tenant.Name))
		log.Infof("[ResendEMail] sending  ResendMail request")
		mailType := "activation"
		_, err := dpClient.Dataplane.Tenants.ResendMail(tenants.NewResendMailParams().WithName(tenantName).WithType(mailType))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
	} else {
		log.Infof("Activation mail will not be send. Tenant state is %s ", swag.StringValue(tenant.State))
	}

}

func SendPasswordResetEMail(c *cli.Context) {
	log.Infof("[SendPasswordResetEMail] Sending password reset mail to Tenant's super user")
	//Search if the tenant exist
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	tenantName := c.String(fl.FlCaasTenantName.Name)
	tenant := retrieveTenantImpl(
		dpClient.Dataplane.Tenants,
		tenantName)
	// check tenant status
	if swag.StringValue(tenant.State) == "ACTIVE" {
		log.Infof("Sending password reset mail to %s ", swag.StringValue(tenant.Name))
		log.Infof("[ResendEMail] sending password reset email")
		mailType := "reset"
		_, err := dpClient.Dataplane.Tenants.ResendMail(tenants.NewResendMailParams().WithName(tenantName).WithType(mailType))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
	} else {
		log.Infof("Password reset mail will not be send. Tenant state is %s ", swag.StringValue(tenant.State))
	}

}

//DisableTenant : Disable a tenant
func DisableTenant(c *cli.Context) {
	log.Infof("[RegisterTenant] Disable a tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	tenantName := c.String(fl.FlCaasTenantName.Name)
	tenant := retrieveTenantImpl(
		dpClient.Dataplane.Tenants,
		tenantName)
	log.Infof("Resending mail to %s ", swag.StringValue(tenant.Name))
	log.Infof("[ResendEMail] sending resend-email request")
	resp, err := dpClient.Dataplane.Tenants.DisableTenant(tenants.NewDisableTenantParams().WithTenantName(tenantName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	tableRows := []utils.Row{}
	for _, tenant := range resp.Payload {
		tableRows = append(tableRows, &tenantsListOut{tenant})
	}
	output.WriteList(tenantDetailsHeader, tableRows)
}

func retrieveTenantImpl(client tenantClient, name string) *model.Tenant {
	log.Infof("[RetrieveTenantImpl] sending  retrieve tenant details request")
	resp, err := client.RetrieveTenant(tenants.NewRetrieveTenantParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}
