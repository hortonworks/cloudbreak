package kerberos

import (
	"time"

	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1kerberos"
	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/model"
	"github.com/hortonworks/cb-cli/dataplane/env"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

var Header = []string{"Name", "Description", "Type", "Environments", "Crn"}

type kerberos struct {
	Name           string `json:"name" yaml:"name"`
	Description    string `json:"description" yaml:"description"`
	Type           string `json:"type" yaml:"type"`
	EnvironmentCrn string
}

type kerberosOutDescribe struct {
	*kerberos
	Crn string `json:"crn" yaml:"crn"`
}

type freeIpaKerberosClient interface {
	CreateKerberosConfigForEnvironment(params *v1kerberos.CreateKerberosConfigForEnvironmentParams) (*v1kerberos.CreateKerberosConfigForEnvironmentOK, error)
	GetKerberosConfigForEnvironment(params *v1kerberos.GetKerberosConfigForEnvironmentParams) (*v1kerberos.GetKerberosConfigForEnvironmentOK, error)
	DeleteKerberosConfigForEnvironment(params *v1kerberos.DeleteKerberosConfigForEnvironmentParams) error
}

func (k *kerberos) DataAsStringArray() []string {
	return []string{k.Name, k.Description, k.Type, k.EnvironmentCrn}
}

func (k *kerberosOutDescribe) DataAsStringArray() []string {
	return append(k.kerberos.DataAsStringArray(), k.Crn)
}

func GetVerifyKdcTrustFlag(c *cli.Context) bool {
	return !c.Bool(fl.FlKerberosDisableVerifyKdcTrust.Name)
}

func CreateAdKerberos(c *cli.Context) error {
	verifyKdcTrust := GetVerifyKdcTrustFlag(c)
	domain := c.String(fl.FlKerberosDomain.Name)
	nameServers := c.String(fl.FlKerberosNameServers.Name)
	password := c.String(fl.FlKerberosPassword.Name)
	tcpAllowed := c.Bool(fl.FlKerberosTcpAllowed.Name)
	principal := c.String(fl.FlKerberosPrincipal.Name)
	url := c.String(fl.FlKerberosUrl.Name)
	adminURL := c.String(fl.FlKerberosAdminUrl.Name)
	realm := c.String(fl.FlKerberosRealm.Name)
	ldapURL := c.String(fl.FlKerberosLdapUrl.Name)
	containerDn := c.String(fl.FlKerberosContainerDn.Name)

	adRequest := model.ActiveDirectoryKerberosV1Descriptor{
		VerifyKdcTrust: verifyKdcTrust,
		Domain:         domain,
		NameServers:    nameServers,
		Password:       &password,
		TCPAllowed:     &tcpAllowed,
		Principal:      &principal,
		URL:            &url,
		AdminURL:       &adminURL,
		Realm:          &realm,
		LdapURL:        &ldapURL,
		ContainerDn:    &containerDn,
	}

	kerberosRequest := CreateKerberosRequest(c)
	kerberosRequest.ActiveDirectory = &adRequest
	return SendCreateKerberosRequest(c, &kerberosRequest)
}

func CreateMitKerberos(c *cli.Context) error {
	verifyKdcTrust := GetVerifyKdcTrustFlag(c)
	domain := c.String(fl.FlKerberosDomain.Name)
	nameServers := c.String(fl.FlKerberosNameServers.Name)
	password := c.String(fl.FlKerberosPassword.Name)
	tcpAllowed := c.Bool(fl.FlKerberosTcpAllowed.Name)
	principal := c.String(fl.FlKerberosPrincipal.Name)
	url := c.String(fl.FlKerberosUrl.Name)
	adminURL := c.String(fl.FlKerberosAdminUrl.Name)
	realm := c.String(fl.FlKerberosRealm.Name)

	mitRequest := model.MITKerberosV1Descriptor{
		VerifyKdcTrust: verifyKdcTrust,
		Domain:         domain,
		NameServers:    nameServers,
		Password:       &password,
		TCPAllowed:     &tcpAllowed,
		Principal:      &principal,
		URL:            &url,
		AdminURL:       &adminURL,
		Realm:          &realm,
	}

	kerberosRequest := CreateKerberosRequest(c)
	kerberosRequest.Mit = &mitRequest
	return SendCreateKerberosRequest(c, &kerberosRequest)
}

func CreateFreeIpaKerberos(c *cli.Context) error {
	verifyKdcTrust := GetVerifyKdcTrustFlag(c)
	domain := c.String(fl.FlKerberosDomain.Name)
	nameServers := c.String(fl.FlKerberosNameServers.Name)
	password := c.String(fl.FlKerberosPassword.Name)
	tcpAllowed := c.Bool(fl.FlKerberosTcpAllowed.Name)
	principal := c.String(fl.FlKerberosPrincipal.Name)
	url := c.String(fl.FlKerberosUrl.Name)
	adminURL := c.String(fl.FlKerberosAdminUrl.Name)
	realm := c.String(fl.FlKerberosRealm.Name)

	freeIpaRequest := model.FreeIPAKerberosV1Descriptor{
		VerifyKdcTrust: verifyKdcTrust,
		Domain:         domain,
		NameServers:    nameServers,
		Password:       &password,
		TCPAllowed:     &tcpAllowed,
		Principal:      &principal,
		URL:            &url,
		AdminURL:       &adminURL,
		Realm:          &realm,
	}

	kerberosRequest := CreateKerberosRequest(c)
	kerberosRequest.FreeIpa = &freeIpaRequest
	return SendCreateKerberosRequest(c, &kerberosRequest)
}

func CreateKerberosRequest(c *cli.Context) model.CreateKerberosConfigV1Request {
	kerberosName := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	environmentName := c.String(fl.FlEnvironmentName.Name)
	environment := env.GetEnvirontmentCrnByName(c, environmentName)

	kerberosRequest := &model.CreateKerberosConfigV1Request{
		Name:           &kerberosName,
		Description:    &description,
		EnvironmentCrn: &environment,
	}

	return *kerberosRequest
}

func SendCreateKerberosRequest(c *cli.Context, request *model.CreateKerberosConfigV1Request) error {
	defer utils.TimeTrack(time.Now(), "create kerberos")
	freeIpaClient := oauth.FreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	return SendCreateKerberosRequestImpl(freeIpaClient.V1kerberos, request, output.Write)
}

func SendCreateKerberosRequestImpl(kerberosClient freeIpaKerberosClient, request *model.CreateKerberosConfigV1Request, writer func([]string, utils.Row)) error {
	resp, err := kerberosClient.CreateKerberosConfigForEnvironment(v1kerberos.NewCreateKerberosConfigForEnvironmentParams().WithBody(request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kerberosResponse := resp.Payload
	writeResponse(writer, kerberosResponse)
	log.Infof("[SendCreateKerberosRequestImpl] kerberos created with name: %s, id: %s", kerberosResponse.Name, kerberosResponse.Crn)
	return nil
}

func GetKerberos(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "describe a kerberos")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	environmentName := c.String(fl.FlEnvironmentName.Name)
	environment := env.GetEnvirontmentCrnByName(c, environmentName)
	freeIpaClient := oauth.FreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	return GetKerberosImpl(freeIpaClient.V1kerberos, environment, output.Write)
}

func GetKerberosImpl(kerberosClient freeIpaKerberosClient, environmentName string, writer func([]string, utils.Row)) error {
	log.Infof("[GetKerberos] describe kerberos config from environment: %s", environmentName)
	resp, err := kerberosClient.GetKerberosConfigForEnvironment(v1kerberos.NewGetKerberosConfigForEnvironmentParams().WithEnvironmentCrn(&environmentName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kerberosResponse := resp.Payload
	writeResponse(writer, kerberosResponse)
	log.Infof("[GetKerberosImpl] kerberos config '%s' is fetched.", kerberosResponse.Name)
	return nil
}

func DeleteKerberos(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete a kerberos")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	environmentName := c.String(fl.FlEnvironmentName.Name)
	environment := env.GetEnvirontmentCrnByName(c, environmentName)
	freeIpaClient := oauth.FreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	return DeleteKerberosImpl(freeIpaClient.V1kerberos, workspaceID, environment)
}

func DeleteKerberosImpl(kerberosClient freeIpaKerberosClient, workspaceID int64, environmentName string) error {
	log.Infof("[DeleteKerberosImpl] delete kerberos config by name: %s", environmentName)
	err := kerberosClient.DeleteKerberosConfigForEnvironment(v1kerberos.NewDeleteKerberosConfigForEnvironmentParams().WithEnvironmentCrn(&environmentName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteKerberosImpl] kerberos config deleted from environment: %s", environmentName)
	return nil
}

func writeResponse(writer func([]string, utils.Row), kerberosResponse *model.DescribeKerberosConfigV1Response) {
	writer(append(Header), &kerberosOutDescribe{
		&kerberos{
			Name:           kerberosResponse.Name,
			Description:    utils.SafeStringConvert(kerberosResponse.Description),
			EnvironmentCrn: *kerberosResponse.EnvironmentCrn,
			Type:           utils.SafeStringConvert(kerberosResponse.Type),
		},
		kerberosResponse.Crn})
}
