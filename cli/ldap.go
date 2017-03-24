package cli

import (
	"time"

	"errors"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/ldap"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"strconv"
	"strings"
)

func CreateLDAP(c *cli.Context) error {
	defer timeTrack(time.Now(), "create LDAP")
	checkRequiredFlags(c)

	name := c.String(FlLdapName.Name)
	domain := c.String(FlLdapDomain.Name)
	bindDn := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)
	userSearchBase := c.String(FlLdapUserSearchBase.Name)
	userSearchFilter := c.String(FlLdapUserSearchFilter.Name)
	userSearchAttribute := c.String(FlLdapUserSearchAttribute.Name)
	groupSearchBase := c.String(FlLdapGroupSearchBase.Name)
	server := c.String(FlLdapServer.Name)
	portSeparatorIndex := strings.LastIndex(server, ":")
	if (!strings.Contains(server, "ldap://") && !strings.Contains(server, "ldaps://")) || portSeparatorIndex == -1 {
		logErrorAndExit(errors.New("invalid ldap server address format, e.g: ldaps://10.0.0.1:389"))
	}
	serverPort, _ := strconv.Atoi(server[portSeparatorIndex+1:])
	serverSSL := false
	if strings.Contains(server, "ldaps") {
		serverSSL = true
	}

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	return createLDAPImpl(cbClient.Cloudbreak.Ldap.PostPublicLdap, name, server, int32(serverPort), serverSSL, domain, bindDn, bindPassword,
		userSearchBase, userSearchFilter, userSearchAttribute, groupSearchBase)
}

func createLDAPImpl(createLDAP func(*ldap.PostPublicLdapParams) (*ldap.PostPublicLdapOK, error),
	name string, server string, port int32, ssl bool, domain string, bindDn string, bindPassword string,
	userSearchBase string, userSearchFilter string, userSearchAttribute string, groupSearchBase string) error {

	log.Infof("[createLDAPImpl] create LDAP with name: %s", name)
	resp, err := createLDAP(&ldap.PostPublicLdapParams{
		Body: &models_cloudbreak.LdapConfigRequest{
			Name:                name,
			ServerHost:          server[0:strings.LastIndex(server, ":")],
			ServerPort:          port,
			ServerSSL:           &ssl,
			Domain:              &domain,
			BindDn:              bindDn,
			BindPassword:        bindPassword,
			UserSearchBase:      userSearchBase,
			UserSearchFilter:    &userSearchFilter,
			UserSearchAttribute: &userSearchAttribute,
			GroupSearchBase:     &groupSearchBase,
		}})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[createLDAPImpl] LDAP created with name: %s, id: %d", name, *resp.Payload.ID)
	return nil
}
