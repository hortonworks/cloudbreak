package kerberos

import (
	"encoding/base64"
	log "github.com/Sirupsen/logrus"
	v4krb "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_kerberos"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
	"strconv"
	"strings"
	"time"
)

var Header = []string{"Name", "Description", "Type", "Environments", "ID"}

type kerberos struct {
	Name         string `json:"Name" yaml:"Name"`
	Description  string `json:"Description" yaml:"Description"`
	Type         string `json:"Type" yaml:"Type"`
	Environments []string
}

type kerberosOutDescribe struct {
	*kerberos
	ID string `json:"ID" yaml:"ID"`
}

type kerberosClient interface {
	ListKerberosConfigByWorkspace(params *v4krb.ListKerberosConfigByWorkspaceParams) (*v4krb.ListKerberosConfigByWorkspaceOK, error)
	GetKerberosConfigInWorkspace(params *v4krb.GetKerberosConfigInWorkspaceParams) (*v4krb.GetKerberosConfigInWorkspaceOK, error)
	CreateKerberosConfigInWorkspace(params *v4krb.CreateKerberosConfigInWorkspaceParams) (*v4krb.CreateKerberosConfigInWorkspaceOK, error)
	AttachKerberosConfigToEnvironments(params *v4krb.AttachKerberosConfigToEnvironmentsParams) (*v4krb.AttachKerberosConfigToEnvironmentsOK, error)
	DetachKerberosConfigFromEnvironments(params *v4krb.DetachKerberosConfigFromEnvironmentsParams) (*v4krb.DetachKerberosConfigFromEnvironmentsOK, error)
	DeleteKerberosConfigInWorkspace(params *v4krb.DeleteKerberosConfigInWorkspaceParams) (*v4krb.DeleteKerberosConfigInWorkspaceOK, error)
}

func (k *kerberos) DataAsStringArray() []string {
	return []string{k.Name, k.Description, k.Type, strings.Join(k.Environments, ",")}
}

func (k *kerberosOutDescribe) DataAsStringArray() []string {
	return append(k.kerberos.DataAsStringArray(), k.ID)
}

func GetVerifyKdcTrustFlag(c *cli.Context) bool {
	return !c.Bool(fl.FlKerberosDisableVerifyKdcTrust.Name)
}

func CreateAdKerberos(c *cli.Context) error {
	admin := c.String(fl.FlKerberosAdmin.Name)
	verifyKdcTrust := GetVerifyKdcTrustFlag(c)
	domain := c.String(fl.FlKerberosDomain.Name)
	nameServers := c.String(fl.FlKerberosNameServers.Name)
	password := c.String(fl.FlKerberosPassword.Name)
	tcpAllowed := c.Bool(fl.FlKerberosTcpAllowed.Name)
	principal := c.String(fl.FlKerberosPrincipal.Name)
	url := c.String(fl.FlKerberosUrl.Name)
	adminUrl := c.String(fl.FlKerberosAdminUrl.Name)
	realm := c.String(fl.FlKerberosRealm.Name)
	ldapUrl := c.String(fl.FlKerberosLdapUrl.Name)
	containerDn := c.String(fl.FlKerberosContainerDn.Name)

	adRequest := model.ActiveDirectoryKerberosDescriptor{
		VerifyKdcTrust: verifyKdcTrust,
		Domain:         domain,
		NameServers:    nameServers,
		Password:       &password,
		TCPAllowed:     &tcpAllowed,
		Principal:      &principal,
		URL:            &url,
		AdminURL:       &adminUrl,
		Realm:          &realm,
		LdapURL:        &ldapUrl,
		ContainerDn:    &containerDn,
		Admin:          admin,
	}

	kerberosRequest := CreateKerberosRequest(c)
	kerberosRequest.ActiveDirectory = &adRequest
	return SendCreateKerberosRequest(c, &kerberosRequest)
}

func CreateCustomKerberos(c *cli.Context) error {
	admin := c.String(fl.FlKerberosAdmin.Name)
	verifyKdcTrust := GetVerifyKdcTrustFlag(c)
	domain := c.String(fl.FlKerberosDomain.Name)
	nameServers := c.String(fl.FlKerberosNameServers.Name)
	password := c.String(fl.FlKerberosPassword.Name)
	tcpAllowed := c.Bool(fl.FlKerberosTcpAllowed.Name)
	principal := c.String(fl.FlKerberosPrincipal.Name)
	krb5Conf := base64.StdEncoding.EncodeToString([]byte(c.String(fl.FlKerberosKrb5Conf.Name)))
	descriptor := base64.StdEncoding.EncodeToString([]byte(c.String(fl.FlKerberosDescriptor.Name)))

	customRequest := model.AmbariKerberosDescriptor{
		VerifyKdcTrust: verifyKdcTrust,
		Domain:         domain,
		NameServers:    nameServers,
		Password:       &password,
		TCPAllowed:     &tcpAllowed,
		Principal:      &principal,
		Krb5Conf:       &krb5Conf,
		Descriptor:     &descriptor,
		Admin:          admin,
	}

	kerberosRequest := CreateKerberosRequest(c)
	kerberosRequest.AmbariDescriptor = &customRequest
	return SendCreateKerberosRequest(c, &kerberosRequest)
}

func CreateFreeIpaKerberos(c *cli.Context) error {
	admin := c.String(fl.FlKerberosAdmin.Name)
	verifyKdcTrust := GetVerifyKdcTrustFlag(c)
	domain := c.String(fl.FlKerberosDomain.Name)
	nameServers := c.String(fl.FlKerberosNameServers.Name)
	password := c.String(fl.FlKerberosPassword.Name)
	tcpAllowed := c.Bool(fl.FlKerberosTcpAllowed.Name)
	principal := c.String(fl.FlKerberosPrincipal.Name)
	url := c.String(fl.FlKerberosUrl.Name)
	adminUrl := c.String(fl.FlKerberosAdminUrl.Name)
	realm := c.String(fl.FlKerberosRealm.Name)

	freeIpaRequest := model.FreeIPAKerberosDescriptor{
		VerifyKdcTrust: verifyKdcTrust,
		Domain:         domain,
		NameServers:    nameServers,
		Password:       &password,
		TCPAllowed:     &tcpAllowed,
		Principal:      &principal,
		URL:            &url,
		AdminURL:       &adminUrl,
		Realm:          &realm,
		Admin:          admin,
	}

	kerberosRequest := CreateKerberosRequest(c)
	kerberosRequest.FreeIpa = &freeIpaRequest
	return SendCreateKerberosRequest(c, &kerberosRequest)
}

func CreateKerberosRequest(c *cli.Context) model.KerberosV4Request {
	kerberosName := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentsOptional.Name), ",")

	kerberosRequest := &model.KerberosV4Request{
		Name:         &kerberosName,
		Description:  &description,
		Environments: environments,
	}

	return *kerberosRequest
}

func SendCreateKerberosRequest(c *cli.Context, request *model.KerberosV4Request) error {
	defer utils.TimeTrack(time.Now(), "create kerberos")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	return SendCreateKerberosRequestImpl(cbClient.Cloudbreak.V4WorkspaceIDKerberos, workspaceID, request, output.Write)
}

func SendCreateKerberosRequestImpl(kerberosClient kerberosClient, workspaceID int64, request *model.KerberosV4Request, writer func([]string, utils.Row)) error {
	resp, err := kerberosClient.CreateKerberosConfigInWorkspace(v4krb.NewCreateKerberosConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kerberosResponse := resp.Payload
	writeResponse(writer, kerberosResponse)
	log.Infof("[SendCreateKerberosRequestImpl] kerberos created with name: %s, id: %d", kerberosResponse.Name, kerberosResponse.ID)
	return nil
}

func ListKerberos(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list kerberos configs")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	return ListKerberosImpl(cbClient.Cloudbreak.V4WorkspaceIDKerberos, workspaceID, output.WriteList)
}

func ListKerberosImpl(kerberosClient kerberosClient, workspaceID int64, writer func([]string, []utils.Row)) error {
	listRequest := v4krb.NewListKerberosConfigByWorkspaceParams().WithWorkspaceID(workspaceID)
	resp, err := kerberosClient.ListKerberosConfigByWorkspace(listRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	for _, k := range resp.Payload.Responses {
		row := &kerberosOutDescribe{
			&kerberos{
				Name:         *k.Name,
				Description:  utils.SafeStringConvert(k.Description),
				Environments: k.Environments,
				Type:         utils.SafeStringConvert(&k.Type),
			},
			strconv.FormatInt(k.ID, 10)}
		tableRows = append(tableRows, row)
	}
	writer(Header, tableRows)
	return nil
}

func GetKerberos(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "describe a kerberos")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kerberosName := c.String(fl.FlName.Name)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	return GetKerberosImpl(cbClient.Cloudbreak.V4WorkspaceIDKerberos, workspaceID, kerberosName, output.Write)
}

func GetKerberosImpl(kerberosClient kerberosClient, workspaceID int64, kerberosName string, writer func([]string, utils.Row)) error {
	log.Infof("[GetKerberos] describe kerberos config by name: %s", kerberosName)
	resp, err := kerberosClient.GetKerberosConfigInWorkspace(v4krb.NewGetKerberosConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(kerberosName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kerberosResponse := resp.Payload
	writeResponse(writer, kerberosResponse)
	log.Infof("[GetKerberosImpl] kerberos config '%s' is now attached to the following environments: %s", kerberosResponse.Name, kerberosResponse.Environments)
	return nil
}

func AttachKerberos(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "attach kerberos to environments")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kerberosName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	return AttachKerberosImpl(cbClient.Cloudbreak.V4WorkspaceIDKerberos, workspaceID, kerberosName, environments, output.Write)
}

func AttachKerberosImpl(kerberosClient kerberosClient, workspaceID int64, kerberosName string, environments []string, writer func([]string, utils.Row)) error {
	log.Infof("[AttachKerberosImpl] attach kerberos config '%s' to environments: %s", kerberosName, environments)
	attachRequest := v4krb.NewAttachKerberosConfigToEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(kerberosName).WithBody(&model.EnvironmentNames{EnvironmentNames: environments})
	response, err := kerberosClient.AttachKerberosConfigToEnvironments(attachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kerberosResponse := response.Payload
	writeResponse(writer, kerberosResponse)
	log.Infof("[AttachKerberosImpl] kerberos config '%s' is now attached to the following environments: %s", kerberosResponse.Name, kerberosResponse.Environments)
	return nil
}

func DetachKerberos(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "detach kerberos from environments")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kerberosName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	return DetachKerberosImpl(cbClient.Cloudbreak.V4WorkspaceIDKerberos, workspaceID, kerberosName, environments, output.Write)
}

func DetachKerberosImpl(kerberosClient kerberosClient, workspaceID int64, kerberosName string, environments []string, writer func([]string, utils.Row)) error {
	log.Infof("[DetachKerberosImpl] detach kerberos config '%s' from environments: %s", kerberosName, environments)
	detachRequest := v4krb.NewDetachKerberosConfigFromEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(kerberosName).WithBody(&model.EnvironmentNames{EnvironmentNames: environments})
	response, err := kerberosClient.DetachKerberosConfigFromEnvironments(detachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kerberosResponse := response.Payload
	writeResponse(writer, kerberosResponse)
	log.Infof("[DetachKerberosImpl] kerberos config '%s' is now attached to the following environments: %s", kerberosResponse.Name, kerberosResponse.Environments)
	return nil
}

func DeleteKerberos(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete a kerberos")
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	kerberosName := c.String(fl.FlName.Name)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	return DeleteKerberosImpl(cbClient.Cloudbreak.V4WorkspaceIDKerberos, workspaceID, kerberosName, output.Write)
}

func DeleteKerberosImpl(kerberosClient kerberosClient, workspaceID int64, kerberosName string, writer func([]string, utils.Row)) error {
	log.Infof("[DeleteKerberosImpl] delete kerberos config by name: %s", kerberosName)
	response, err := kerberosClient.DeleteKerberosConfigInWorkspace(v4krb.NewDeleteKerberosConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(kerberosName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	kerberosResponse := response.Payload
	writeResponse(writer, kerberosResponse)
	log.Infof("[DeleteKerberosImpl] delete kerberos config deleted: %s", kerberosResponse.Name)
	return nil
}

func writeResponse(writer func([]string, utils.Row), kerberosResponse *model.KerberosV4Response) {
	writer(append(Header), &kerberosOutDescribe{
		&kerberos{
			Name:         kerberosResponse.Name,
			Description:  utils.SafeStringConvert(kerberosResponse.Description),
			Environments: kerberosResponse.Environments,
			Type:         utils.SafeStringConvert(kerberosResponse.Type),
		},
		strconv.FormatInt(kerberosResponse.ID, 10)})
}
