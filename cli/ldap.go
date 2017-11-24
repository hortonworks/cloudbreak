package cli

import (
	"time"

	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1ldap"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"strconv"
	"strings"
)

var LdapHeader = []string{"Name", "Server", "Domain", "BindDn", "DirectoryType",
	"UserSearchBase", "UserNameAttribute", "UserObjectClass",
	"GroupMemberAttribute", "GroupNameAttribute", "GroupObjectClass", "GroupSearchBase"}

type ldap struct {
	Name                 string `json:"Name" yaml:"Name"`
	Server               string `json:"Server" yaml:"Server"`
	Domain               string `json:"Domain,omitempty" yaml:"Domain,omitempty"`
	BindDn               string `json:"BindDn" yaml:"BindDn"`
	DirectoryType        string `json:"DirectoryType" yaml:"DirectoryType"`
	UserSearchBase       string `json:"UserSearchBase" yaml:"UserSearchBase"`
	UserNameAttribute    string `json:"UserNameAttribute,omitempty" yaml:"UserNameAttribute,omitempty"`
	UserObjectClass      string `json:"UserObjectClass,omitempty" yaml:"UserObjectClass,omitempty"`
	GroupMemberAttribute string `json:"GroupMemberAttribute,omitempty" yaml:"GroupMemberAttribute,omitempty"`
	GroupNameAttribute   string `json:"GroupNameAttribute,omitempty" yaml:"GroupNameAttribute,omitempty"`
	GroupObjectClass     string `json:"GroupObjectClass,omitempty" yaml:"GroupObjectClass,omitempty"`
	GroupSearchBase      string `json:"GroupSearchBase,omitempty" yaml:"GroupSearchBase,omitempty"`
}

func (l *ldap) DataAsStringArray() []string {
	return []string{l.Name, l.Server, l.Domain, l.BindDn, l.DirectoryType, l.UserSearchBase, l.UserNameAttribute,
		l.UserObjectClass, l.GroupMemberAttribute, l.GroupNameAttribute, l.GroupObjectClass, l.GroupSearchBase}
}

type ldapClient interface {
	PostPrivateLdap(params *v1ldap.PostPrivateLdapParams) (*v1ldap.PostPrivateLdapOK, error)
	PostPublicLdap(params *v1ldap.PostPublicLdapParams) (*v1ldap.PostPublicLdapOK, error)
	GetPublicsLdap(params *v1ldap.GetPublicsLdapParams) (*v1ldap.GetPublicsLdapOK, error)
}

func CreateLDAP(c *cli.Context) error {
	checkRequiredFlags(c)

	name := c.String(FlName.Name)
	domain := c.String(FlLdapDomain.Name)
	bindDn := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)

	directoryType := c.String(FlLdapDirectoryType.Name)
	userSearchBase := c.String(FlLdapUserSearchBase.Name)
	userNameAttribute := c.String(FlLdapUserNameAttribute.Name)
	userObjectClass := c.String(FlLdapUserObjectClass.Name)

	groupSearchBase := c.String(FlLdapGroupSearchBase.Name)
	groupMemberAttribute := c.String(FlLdapGroupMemberAttribute.Name)
	groupNameAttribute := c.String(FlLdapGroupNameAttribute.Name)
	groupObjectClass := c.String(FlLdapGroupObjectClass.Name)

	server := c.String(FlLdapServer.Name)
	public := c.Bool(FlPublicOptional.Name)

	portSeparatorIndex := strings.LastIndex(server, ":")
	if (!strings.Contains(server, "ldap://") && !strings.Contains(server, "ldaps://")) || portSeparatorIndex == -1 {
		utils.LogErrorMessageAndExit("Invalid ldap server address format, e.g: ldaps://10.0.0.1:389")
	}
	serverPort, _ := strconv.Atoi(server[portSeparatorIndex+1:])
	protocol := server[0:strings.Index(server, ":")]

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	return createLDAPImpl(cbClient.Cloudbreak.V1ldap, int32(serverPort), name, server, protocol, domain, bindDn, bindPassword, directoryType,
		userSearchBase, userNameAttribute, userObjectClass, groupSearchBase, groupMemberAttribute, groupNameAttribute, groupObjectClass, public)
}

func createLDAPImpl(ldapClient ldapClient, port int32, name, server, protocol, domain, bindDn, bindPassword, directoryType,
	userSearchBase, userNameAttribute, userObjectClass, groupSearchBase, groupMemberAttribute, groupNameAttribute,
	groupObjectClass string, public bool) error {
	defer utils.TimeTrack(time.Now(), "create ldap")

	host := server[strings.LastIndex(server, "/")+1 : strings.LastIndex(server, ":")]
	ldapConfigRequest := &models_cloudbreak.LdapConfigRequest{
		Name:                 &name,
		ServerHost:           &host,
		ServerPort:           &port,
		Protocol:             protocol,
		Domain:               domain,
		BindDn:               &bindDn,
		BindPassword:         &bindPassword,
		DirectoryType:        directoryType,
		UserSearchBase:       &userSearchBase,
		UserNameAttribute:    userNameAttribute,
		UserObjectClass:      userObjectClass,
		GroupMemberAttribute: groupMemberAttribute,
		GroupNameAttribute:   groupNameAttribute,
		GroupObjectClass:     groupObjectClass,
		GroupSearchBase:      groupSearchBase,
	}

	log.Infof("[createLDAPImpl] create ldap with name: %s", name)
	var ldap *models_cloudbreak.LdapConfigResponse
	if public {
		resp, err := ldapClient.PostPublicLdap(v1ldap.NewPostPublicLdapParams().WithBody(ldapConfigRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		ldap = resp.Payload
	} else {
		resp, err := ldapClient.PostPrivateLdap(v1ldap.NewPostPrivateLdapParams().WithBody(ldapConfigRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		ldap = resp.Payload
	}

	log.Infof("[createLDAPImpl] ldap created with name: %s, id: %d", name, ldap.ID)
	return nil
}

func ListLdaps(c *cli.Context) error {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list ldap configs")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	return listLdapsImpl(cbClient.Cloudbreak.V1ldap, output.WriteList)
}

func listLdapsImpl(ldapClient ldapClient, writer func([]string, []utils.Row)) error {
	resp, err := ldapClient.GetPublicsLdap(v1ldap.NewGetPublicsLdapParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, l := range resp.Payload {
		server := fmt.Sprintf("%s://%s:%d", l.Protocol, utils.SafeStringConvert(l.ServerHost), utils.SafeInt32Convert(l.ServerPort))
		row := &ldap{
			Name:                 *l.Name,
			Server:               server,
			Domain:               l.Domain,
			BindDn:               utils.SafeStringConvert(l.BindDn),
			DirectoryType:        l.DirectoryType,
			UserSearchBase:       utils.SafeStringConvert(l.UserSearchBase),
			UserNameAttribute:    l.UserNameAttribute,
			UserObjectClass:      l.UserObjectClass,
			GroupMemberAttribute: l.GroupMemberAttribute,
			GroupNameAttribute:   l.GroupNameAttribute,
			GroupObjectClass:     l.GroupObjectClass,
			GroupSearchBase:      l.GroupSearchBase,
		}
		tableRows = append(tableRows, row)
	}

	writer(LdapHeader, tableRows)
	return nil
}

func DeleteLdap(c *cli.Context) error {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "delete an ldap")

	ldapName := c.String(FlName.Name)
	log.Infof("[DeleteLdap] delete ldap config by name: %s", ldapName)
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	if err := cbClient.Cloudbreak.V1ldap.DeletePublicLdap(v1ldap.NewDeletePublicLdapParams().WithName(ldapName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteLdap] ldap config deleted: %s", ldapName)
	return nil
}
