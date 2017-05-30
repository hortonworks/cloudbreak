package cli

import (
	"time"

	"errors"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/ldap"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"strconv"
	"strings"
)

var LdapHeader []string = []string{"Name", "Server", "Domain", "BindDn", "DirectoryType",
	"UserSearchBase", "UserNameAttribute", "UserObjectClass",
	"GroupMemberAttribute", "GroupNameAttribute", "GroupObjectClass", "GroupSearchBase"}

type Ldap struct {
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

func (l *Ldap) DataAsStringArray() []string {
	return []string{l.Name, l.Server, l.Domain, l.BindDn, l.UserSearchBase, l.UserNameAttribute, l.UserObjectClass, l.GroupSearchBase}
}

func CreateLDAP(c *cli.Context) error {
	defer timeTrack(time.Now(), "create ldap")
	checkRequiredFlags(c)

	name := c.String(FlLdapName.Name)
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
	portSeparatorIndex := strings.LastIndex(server, ":")
	if (!strings.Contains(server, "ldap://") && !strings.Contains(server, "ldaps://")) || portSeparatorIndex == -1 {
		logErrorAndExit(errors.New("invalid ldap server address format, e.g: ldaps://10.0.0.1:389"))
	}
	serverPort, _ := strconv.Atoi(server[portSeparatorIndex+1:])
	protocol := server[0:strings.Index(server, ":")]

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	return createLDAPImpl(cbClient.Cloudbreak.Ldap.PostPublicLdap, name, server, int32(serverPort), protocol, domain, bindDn, bindPassword, directoryType,
		userSearchBase, userNameAttribute, userObjectClass,
		groupSearchBase, groupMemberAttribute, groupNameAttribute, groupObjectClass)
}

func createLDAPImpl(createLDAP func(*ldap.PostPublicLdapParams) (*ldap.PostPublicLdapOK, error),
	name string, server string, port int32, protocol string, domain string, bindDn string, bindPassword string, directoryType string,
	userSearchBase string, userNameAttribute string, userObjectClass string,
	groupSearchBase string, groupMemberAttribute string, groupNameAttribute string, groupObjectClass string) error {

	log.Infof("[createLDAPImpl] create ldap with name: %s", name)
	resp, err := createLDAP(&ldap.PostPublicLdapParams{
		Body: &models_cloudbreak.LdapConfigRequest{
			Name:                 name,
			ServerHost:           server[strings.LastIndex(server, "/")+1 : strings.LastIndex(server, ":")],
			ServerPort:           port,
			Protocol:             &protocol,
			Domain:               &domain,
			BindDn:               bindDn,
			BindPassword:         bindPassword,
			DirectoryType:        &directoryType,
			UserSearchBase:       userSearchBase,
			UserNameAttribute:    &userNameAttribute,
			UserObjectClass:      &userObjectClass,
			GroupMemberAttribute: &groupMemberAttribute,
			GroupNameAttribute:   &groupNameAttribute,
			GroupObjectClass:     &groupObjectClass,
			GroupSearchBase:      &groupSearchBase,
		}})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[createLDAPImpl] ldap created with name: %s, id: %d", name, *resp.Payload.ID)
	return nil
}

func ListLdaps(c *cli.Context) error {
	defer timeTrack(time.Now(), "list ldap configs")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	output := Output{Format: c.String(FlOutput.Name)}
	return ListLdapsImpl(cbClient.Cloudbreak.Ldap.GetPublicsLdap, output.WriteList)
}

func ListLdapsImpl(getLdaps func(*ldap.GetPublicsLdapParams) (*ldap.GetPublicsLdapOK, error), writer func([]string, []Row)) error {
	resp, err := getLdaps(&ldap.GetPublicsLdapParams{})

	if err != nil {
		logErrorAndExit(err)
	}

	var tableRows []Row
	for _, l := range resp.Payload {
		row := &Ldap{
			Name:                 l.Name,
			Server:               fmt.Sprintf("%s://%s:%d", *l.Protocol, l.ServerHost, l.ServerPort),
			Domain:               *l.Domain,
			BindDn:               l.BindDn,
			DirectoryType:        *l.DirectoryType,
			UserSearchBase:       l.UserSearchBase,
			UserNameAttribute:    *l.UserNameAttribute,
			UserObjectClass:      *l.UserObjectClass,
			GroupMemberAttribute: *l.GroupMemberAttribute,
			GroupNameAttribute:   *l.GroupNameAttribute,
			GroupObjectClass:     *l.GroupObjectClass,
			GroupSearchBase:      *l.GroupSearchBase,
		}
		tableRows = append(tableRows, row)
	}

	writer(LdapHeader, tableRows)
	return nil
}

func (c *Cloudbreak) GetLdapByName(name string) *models_cloudbreak.LdapConfigResponse {
	defer timeTrack(time.Now(), "get ldap by name")
	log.Infof("[GetLdapByName] get ldap by name: %s", name)

	resp, err := c.Cloudbreak.Ldap.GetPublicLdap(&ldap.GetPublicLdapParams{Name: name})
	if err != nil {
		logErrorAndExit(err)
	}

	id64 := *resp.Payload.ID
	log.Infof("[GetLdapByName] '%s' ldap id: %d", name, id64)
	return resp.Payload
}

func DeleteLdap(c *cli.Context) error {
	defer timeTrack(time.Now(), "delete an ldap")
	checkRequiredFlags(c)

	ldapName := c.String(FlLdapName.Name)
	log.Infof("[DeleteLdap] delete ldap config by name: %s", ldapName)
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	deleteLdapImpl(ldapName, cbClient.Cloudbreak.Ldap.DeletePublicLdap)
	return nil
}

func deleteLdapImpl(ldapName string, deleteLdap func(*ldap.DeletePublicLdapParams) error) {
	if err := deleteLdap(&ldap.DeletePublicLdapParams{Name: ldapName}); err != nil {
		logErrorAndExit(err)
	}
	log.Infof("[deleteLdapImpl] ldap config deleted: %s", ldapName)
}
