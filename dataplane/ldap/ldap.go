package ldap

import (
	"encoding/base64"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"crypto/tls"
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1ldaps"
	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/model"
	"github.com/hortonworks/cb-cli/dataplane/env"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
	"golang.org/x/text/encoding/unicode"
	ldaputils "gopkg.in/ldap.v2"
)

var Header = []string{"Name", "Description", "Server", "Domain", "DirectoryType",
	"UserSearchBase", "UserDnPattern", "UserNameAttribute", "UserObjectClass",
	"GroupMemberAttribute", "GroupNameAttribute", "GroupObjectClass", "GroupSearchBase", "Certificate", "Environments"}

type ldap struct {
	Name                 string `json:"Name" yaml:"Name"`
	Description          string `json:"Description" yaml:"Description"`
	Server               string `json:"Server" yaml:"Server"`
	Domain               string `json:"Domain,omitempty" yaml:"Domain,omitempty"`
	DirectoryType        string `json:"DirectoryType" yaml:"DirectoryType"`
	UserSearchBase       string `json:"UserSearchBase" yaml:"UserSearchBase"`
	UserDnPattern        string `json:"UserDnPattern" yaml:"UserDnPattern"`
	UserNameAttribute    string `json:"UserNameAttribute,omitempty" yaml:"UserNameAttribute,omitempty"`
	UserObjectClass      string `json:"UserObjectClass,omitempty" yaml:"UserObjectClass,omitempty"`
	GroupMemberAttribute string `json:"GroupMemberAttribute,omitempty" yaml:"GroupMemberAttribute,omitempty"`
	GroupNameAttribute   string `json:"GroupNameAttribute,omitempty" yaml:"GroupNameAttribute,omitempty"`
	GroupObjectClass     string `json:"GroupObjectClass,omitempty" yaml:"GroupObjectClass,omitempty"`
	GroupSearchBase      string `json:"GroupSearchBase,omitempty" yaml:"GroupSearchBase,omitempty"`
	AdminGroup           string `json:"AdminGroup,omitempty" yaml:"AdminGroup,omitempty"`
	Certificate          string `json:"Certificate,omitempty" yaml:"Certificate,omitempty"`
	EnvironmentCrn       string
}

type ldapOutDescribe struct {
	*ldap
	Crn string `json:"Crn" yaml:"Crn"`
}

func (l *ldap) DataAsStringArray() []string {
	return []string{l.Name, l.Description, l.Server, l.Domain, l.DirectoryType, l.UserSearchBase, l.UserDnPattern, l.UserNameAttribute,
		l.UserObjectClass, l.GroupMemberAttribute, l.GroupNameAttribute, l.GroupObjectClass, l.GroupSearchBase, l.EnvironmentCrn}
}

func (l *ldapOutDescribe) DataAsStringArray() []string {
	return append(l.ldap.DataAsStringArray(), l.Crn)
}

var Users = []string{"Distinguished Names", "Email", "Member of Group"}

type ldapUserSearchResult struct {
	Name     string `json:"Name" yaml:"Name"`
	Email    string `json:"Email" yaml:"Email"`
	MemberOf string `json:"MemberOf" yaml:"MemberOf"`
}

func (l *ldapUserSearchResult) DataAsStringArray() []string {
	return []string{l.Name, l.Email, l.MemberOf}
}

var Groups = []string{"Distinguished Names"}

type ldapGroupSearchResult struct {
	Name string `json:"Name" yaml:"Name"`
}

func (l *ldapGroupSearchResult) DataAsStringArray() []string {
	return []string{l.Name}
}

type ldapClient interface {
	CreateLdapConfigV1(params *v1ldaps.CreateLdapConfigV1Params) (*v1ldaps.CreateLdapConfigV1OK, error)
}

func CreateLDAP(c *cli.Context) error {

	name := c.String(fl.FlName.Name)
	description := c.String(fl.FlDescriptionOptional.Name)
	domain := c.String(fl.FlLdapDomain.Name)
	bindDn := c.String(fl.FlLdapBindDN.Name)
	bindPassword := c.String(fl.FlLdapBindPassword.Name)

	directoryType := c.String(fl.FlLdapDirectoryType.Name)
	userSearchBase := c.String(fl.FlLdapUserSearchBase.Name)
	userDnPattern := c.String(fl.FlLdapUserDnPattern.Name)
	userNameAttribute := c.String(fl.FlLdapUserNameAttribute.Name)
	userObjectClass := c.String(fl.FlLdapUserObjectClass.Name)

	groupSearchBase := c.String(fl.FlLdapGroupSearchBase.Name)
	groupMemberAttribute := c.String(fl.FlLdapGroupMemberAttribute.Name)
	groupNameAttribute := c.String(fl.FlLdapGroupNameAttribute.Name)
	groupObjectClass := c.String(fl.FlLdapGroupObjectClass.Name)
	adminGroup := c.String(fl.FlLdapAdminGroup.Name)
	certificateLocation := c.String(fl.FlLdapCertificate.Name)
	certificate := ""
	if certificateLocation != "" {
		certificate = base64.StdEncoding.EncodeToString(utils.ReadFile(certificateLocation))
	}
	server := c.String(fl.FlLdapServer.Name)

	environmentName := c.String(fl.FlEnvironmentName.Name)
	environment := env.GetEnvirontmentCrnByName(c, environmentName)

	ldapRegexp := regexp.MustCompile("^(?:ldap://|ldaps://)[a-z0-9-.]+:\\d+$")
	if !ldapRegexp.MatchString(server) {
		utils.LogErrorMessageAndExit("Invalid ldap server address format, e.g: ldaps://10.0.0.1:389")
	}

	portSeparatorIndex := strings.LastIndex(server, ":")
	serverPort, _ := strconv.Atoi(server[portSeparatorIndex+1:])
	protocol := server[0:strings.Index(server, ":")]

	freeIpaClient := oauth.FreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa

	return createLDAPImpl(freeIpaClient.V1ldaps, int32(serverPort), name, description, server, protocol, domain, bindDn, bindPassword, directoryType,
		userSearchBase, userDnPattern, userNameAttribute, userObjectClass, groupSearchBase, groupMemberAttribute, groupNameAttribute, groupObjectClass, adminGroup, certificate, environment)
}

func createLDAPImpl(ldapClient ldapClient, port int32, name, description, server, protocol, domain, bindDn, bindPassword, directoryType,
	userSearchBase, userDnPattern, userNameAttribute, userObjectClass, groupSearchBase, groupMemberAttribute, groupNameAttribute,
	groupObjectClass, adminGroup, certificate string, environment string) error {
	defer utils.TimeTrack(time.Now(), "create ldap")

	host := server[strings.LastIndex(server, "/")+1 : strings.LastIndex(server, ":")]
	ldapConfigRequest := &model.CreateLdapConfigV1Request{
		Name:                 &name,
		Description:          &description,
		Host:                 &host,
		Port:                 &port,
		Protocol:             protocol,
		Domain:               domain,
		BindDn:               &bindDn,
		BindPassword:         &bindPassword,
		DirectoryType:        directoryType,
		UserSearchBase:       &userSearchBase,
		UserDnPattern:        &userDnPattern,
		UserNameAttribute:    userNameAttribute,
		UserObjectClass:      userObjectClass,
		GroupMemberAttribute: groupMemberAttribute,
		GroupNameAttribute:   groupNameAttribute,
		GroupObjectClass:     groupObjectClass,
		GroupSearchBase:      groupSearchBase,
		AdminGroup:           adminGroup,
		Certificate:          certificate,
		EnvironmentCrn:       &environment,
	}

	log.Infof("[createLDAPImpl] create ldap with name: %s", name)
	resp, err := ldapClient.CreateLdapConfigV1(v1ldaps.NewCreateLdapConfigV1Params().WithBody(ldapConfigRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	ldap := resp.Payload

	log.Infof("[createLDAPImpl] ldap created with name: %s, id: %s", name, ldap.Crn)
	return nil
}

func DeleteLdap(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete an ldap")

	environmentName := c.String(fl.FlEnvironmentName.Name)
	environment := env.GetEnvirontmentCrnByName(c, environmentName)
	log.Infof("[DeleteLdap] ldap config deleted from environment: %s", environment)
	freeIpaClient := oauth.FreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa

	if err := freeIpaClient.V1ldaps.DeleteLdapConfigV1(v1ldaps.NewDeleteLdapConfigV1Params().WithEnvironmentCrn(&environment)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteLdap] ldap config deleted: %s", environment)
	return nil
}

func DescribeLdap(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe an ldap")

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	environmentName := c.String(fl.FlEnvironmentName.Name)
	environment := env.GetEnvirontmentCrnByName(c, environmentName)
	log.Infof("[DescribeLdap] describe ldap config in environment: %s", environment)
	freeIpaClient := oauth.FreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa

	resp, err := freeIpaClient.V1ldaps.GetLdapConfigV1(v1ldaps.NewGetLdapConfigV1Params().WithEnvironmentCrn(&environment))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	l := resp.Payload
	server := fmt.Sprintf("%s://%s:%d", l.Protocol, utils.SafeStringConvert(l.Host), utils.SafeInt32Convert(l.Port))
	output.Write(append(Header, "Crn"), &ldapOutDescribe{
		&ldap{
			Name:                 *l.Name,
			Description:          utils.SafeStringConvert(l.Description),
			Server:               server,
			Domain:               l.Domain,
			DirectoryType:        l.DirectoryType,
			UserSearchBase:       utils.SafeStringConvert(l.UserSearchBase),
			UserDnPattern:        utils.SafeStringConvert(l.UserDnPattern),
			UserNameAttribute:    l.UserNameAttribute,
			UserObjectClass:      l.UserObjectClass,
			GroupMemberAttribute: l.GroupMemberAttribute,
			GroupNameAttribute:   l.GroupNameAttribute,
			GroupObjectClass:     l.GroupObjectClass,
			GroupSearchBase:      l.GroupSearchBase,
			AdminGroup:           l.AdminGroup,
			Certificate:          l.Certificate,
			EnvironmentCrn:       *l.EnvironmentCrn,
		},
		l.Crn})
}

func CreateLdapUser(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "create ldap user")

	directoryType := c.String(fl.FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(fl.FlLdapBindDN.Name)
	bindPassword := c.String(fl.FlLdapBindPassword.Name)
	ldapServer := c.String(fl.FlLdapServer.Name)
	userName := c.String(fl.FlLdapUserToCreate.Name)
	password := c.String(fl.FlLdapUserToCreatePassword.Name)
	email := c.String(fl.FlLdapUserToCreateEmail.Name)
	baseDn := c.String(fl.FlLdapUserToCreateBase.Name)
	domain := c.String(fl.FlLdapDomain.Name)
	groups := c.String(fl.FlLdapUserToCreateGroups.Name)
	ldaps := c.Bool(fl.FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	encoder := unicode.UTF16(unicode.LittleEndian, unicode.IgnoreBOM).NewEncoder()
	utfLePassword, err := encoder.String("\"" + password + "\"")
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	// NORMAL_ACCOUNT (512) + DONT_EXPIRE_PASSWORD (65536)
	accountControl := "66048"
	userAttributes := []ldaputils.Attribute{
		{Type: "objectclass", Vals: []string{"person", "user"}},
		{Type: "givenName", Vals: []string{userName}},
		{Type: "sn", Vals: []string{userName}},
		{Type: "sAMAccountName", Vals: []string{userName}},
		{Type: "userPrincipalName", Vals: []string{fmt.Sprintf("%s@%s", userName, strings.ToUpper(domain))}},
		{Type: "userAccountControl", Vals: []string{accountControl}},
		{Type: "unicodePwd", Vals: []string{utfLePassword}},
		{Type: "mail", Vals: []string{email}},
	}

	userDn := fmt.Sprintf("CN=%s,%s", userName, baseDn)
	ldapAdd(ldap, userAttributes, userDn)

	if len(groups) > 0 {
		groupArray := utils.DelimitedStringToArray(groups, ";")
		for _, group := range groupArray {
			log.Infof("[CreateLdapUser] add user %s to group: %s", userDn, group)
			modifyAttributes := []ldaputils.PartialAttribute{
				{Type: "member", Vals: []string{userDn}},
			}
			modifyRequest := &ldaputils.ModifyRequest{
				DN:            group,
				AddAttributes: modifyAttributes,
			}
			if err := ldap.Modify(modifyRequest); err != nil {
				utils.LogErrorAndExit(err)
			}
			log.Infof("[CreateLdapUser] user %s successfully added to group: %s", userDn, group)
		}
	}

	return nil
}

func DeleteLdapUser(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete ldap user")

	directoryType := c.String(fl.FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(fl.FlLdapBindDN.Name)
	bindPassword := c.String(fl.FlLdapBindPassword.Name)
	ldapServer := c.String(fl.FlLdapServer.Name)
	userName := c.String(fl.FlLdapUserToDelete.Name)
	baseDn := c.String(fl.FlLdapUserToDeleteBase.Name)
	ldaps := c.Bool(fl.FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	dn := fmt.Sprintf("CN=%s,%s", userName, baseDn)
	ldapDel(ldap, dn)

	return nil
}

func CreateLdapGroup(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "create ldap group")

	directoryType := c.String(fl.FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(fl.FlLdapBindDN.Name)
	bindPassword := c.String(fl.FlLdapBindPassword.Name)
	ldapServer := c.String(fl.FlLdapServer.Name)
	group := c.String(fl.FlLdapGroupToCreate.Name)
	baseDn := c.String(fl.FlLdapGroupToCreateBase.Name)
	ldaps := c.Bool(fl.FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	groupAttributes := []ldaputils.Attribute{
		{Type: "objectclass", Vals: []string{"group"}},
		{Type: "sAMAccountName", Vals: []string{group}},
	}

	groupDn := fmt.Sprintf("CN=%s,%s", group, baseDn)
	ldapAdd(ldap, groupAttributes, groupDn)

	return nil
}

func DeleteLdapGroup(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete ldap group")

	directoryType := c.String(fl.FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(fl.FlLdapBindDN.Name)
	bindPassword := c.String(fl.FlLdapBindPassword.Name)
	ldapServer := c.String(fl.FlLdapServer.Name)
	group := c.String(fl.FlLdapGroupToDelete.Name)
	baseDn := c.String(fl.FlLdapGroupToDeleteBase.Name)
	ldaps := c.Bool(fl.FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	dn := fmt.Sprintf("CN=%s,%s", group, baseDn)
	ldapDel(ldap, dn)

	return nil
}

func ListLdapUsers(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list ldap users")

	directoryType := c.String(fl.FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(fl.FlLdapBindDN.Name)
	bindPassword := c.String(fl.FlLdapBindPassword.Name)
	ldapServer := c.String(fl.FlLdapServer.Name)
	searchBase := c.String(fl.FlLdapUserSearchBase.Name)
	ldaps := c.Bool(fl.FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	searchReq := ldaputils.NewSearchRequest(searchBase,
		ldaputils.ScopeWholeSubtree, ldaputils.NeverDerefAliases, 0, 0, false,
		"(&(objectClass=user))", []string{"dn", "memberOf", "mail"}, nil,
	)

	result := ldapSearch(ldap, searchReq)
	searchResults := convertLdapEntryToUserSearchResult(result.Entries)

	var tableRows []utils.Row
	for _, l := range searchResults {
		row := &ldapUserSearchResult{
			Name:     l.Name,
			Email:    l.Email,
			MemberOf: l.MemberOf,
		}
		tableRows = append(tableRows, row)
	}
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	output.WriteList(Users, tableRows)

	return nil
}

func ListLdapGroups(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list ldap groups")

	directoryType := c.String(fl.FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(fl.FlLdapBindDN.Name)
	bindPassword := c.String(fl.FlLdapBindPassword.Name)
	ldapServer := c.String(fl.FlLdapServer.Name)
	searchBase := c.String(fl.FlLdapGroupSearchBase.Name)
	ldaps := c.Bool(fl.FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	searchReq := ldaputils.NewSearchRequest(searchBase,
		ldaputils.ScopeWholeSubtree, ldaputils.NeverDerefAliases, 0, 0, false,
		"(&(objectClass=group))", []string{"dn"}, nil,
	)

	result := ldapSearch(ldap, searchReq)
	searchResults := convertLdapEntryToGroupSearchResult(result.Entries)

	var tableRows []utils.Row
	for _, l := range searchResults {
		row := &ldapGroupSearchResult{
			Name: l.Name,
		}
		tableRows = append(tableRows, row)
	}
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	output.WriteList(Groups, tableRows)

	return nil
}

func ldapAdd(ldap *ldaputils.Conn, objectAttributes []ldaputils.Attribute, objectDn string) {
	log.Infof("[ldapAdd] create object: %s", objectDn)
	addRequest := &ldaputils.AddRequest{
		Attributes: objectAttributes,
		DN:         objectDn,
	}
	if err := ldap.Add(addRequest); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ldapAdd] object successfully created: %s", objectDn)
}

func ldapDel(ldap *ldaputils.Conn, dn string) {
	log.Infof("[ldapDel] delete object: %s", dn)
	delRequest := &ldaputils.DelRequest{DN: dn}
	if err := ldap.Del(delRequest); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ldapDel] object successfully deleted: %s", dn)
}

func ldapSearch(ldap *ldaputils.Conn, searchRequest *ldaputils.SearchRequest) *ldaputils.SearchResult {
	log.Infof("[ldapSearch] execute LDAP search: %s with filter %s", searchRequest.BaseDN, searchRequest.Filter)
	result, err := ldap.Search(searchRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ldapSearch] LDAP search successfully finished")
	return result
}

func convertLdapEntryToUserSearchResult(entries []*ldaputils.Entry) []ldapUserSearchResult {
	var result = make([]ldapUserSearchResult, 0)
	for _, e := range entries {
		memberOf := ""
		email := ""
		for _, attr := range e.Attributes {
			if len(attr.Values) > 0 && len(attr.Name) > 0 {
				switch attr.Name {
				case "memberOf":
					memberOf = strings.Join(attr.Values, ";")
				case "mail":
					email = strings.Join(attr.Values, ";")
				}
			}
		}
		result = append(result, ldapUserSearchResult{e.DN, email, memberOf})
	}
	return result
}

func convertLdapEntryToGroupSearchResult(entries []*ldaputils.Entry) []ldapGroupSearchResult {
	var result = make([]ldapGroupSearchResult, 0)
	if entries != nil {
		for _, e := range entries {
			result = append(result, ldapGroupSearchResult{e.DN})
		}
	}
	return result
}

func connectLdap(ldapServer, bindUser, bindPassword string, secure bool) *ldaputils.Conn {
	var ldap *ldaputils.Conn = nil
	var err error
	if secure {
		ldap, err = ldaputils.DialTLS("tcp", ldapServer, &tls.Config{InsecureSkipVerify: true})
	} else {
		ldap, err = ldaputils.Dial("tcp", ldapServer)
	}
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[connectLdap] connecting to LDAP %s with bind %s", ldapServer, bindUser)
	if err = ldap.Bind(bindUser, bindPassword); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Info("[connectLdap] successfully connected to LDAP")
	return ldap
}

func validateDirectoryTypeToManageUsers(directoryType string) {
	if len(directoryType) == 0 || (len(directoryType) > 0 && "ACTIVE_DIRECTORY" != directoryType) {
		utils.LogErrorAndExit(errors.New("only ACTIVE_DIRECTORY is supported at the moment"))
	}
}
