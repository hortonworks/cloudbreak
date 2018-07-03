package cli

import (
	"time"

	"crypto/tls"
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1ldap"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"golang.org/x/text/encoding/unicode"
	ldaputils "gopkg.in/ldap.v2"
)

var LdapHeader = []string{"Name", "Server", "Domain", "BindDn", "DirectoryType",
	"UserSearchBase", "UserDnPattern", "UserNameAttribute", "UserObjectClass",
	"GroupMemberAttribute", "GroupNameAttribute", "GroupObjectClass", "GroupSearchBase"}

type ldap struct {
	Name                 string `json:"Name" yaml:"Name"`
	Server               string `json:"Server" yaml:"Server"`
	Domain               string `json:"Domain,omitempty" yaml:"Domain,omitempty"`
	BindDn               string `json:"BindDn" yaml:"BindDn"`
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
}

func (l *ldap) DataAsStringArray() []string {
	return []string{l.Name, l.Server, l.Domain, l.BindDn, l.DirectoryType, l.UserSearchBase, l.UserDnPattern, l.UserNameAttribute,
		l.UserObjectClass, l.GroupMemberAttribute, l.GroupNameAttribute, l.GroupObjectClass, l.GroupSearchBase}
}

var LdapUsers = []string{"Distinguished Names", "Email", "Member of Group"}

type ldapUserSearchResult struct {
	Name     string `json:"Name" yaml:"Name"`
	Email    string `json:"Email" yaml:"Email"`
	MemberOf string `json:"MemberOf" yaml:"MemberOf"`
}

func (l *ldapUserSearchResult) DataAsStringArray() []string {
	return []string{l.Name, l.Email, l.MemberOf}
}

var LdapGroups = []string{"Distinguished Names"}

type ldapGroupSearchResult struct {
	Name string `json:"Name" yaml:"Name"`
}

func (l *ldapGroupSearchResult) DataAsStringArray() []string {
	return []string{l.Name}
}

type ldapClient interface {
	PostPrivateLdap(params *v1ldap.PostPrivateLdapParams) (*v1ldap.PostPrivateLdapOK, error)
	PostPublicLdap(params *v1ldap.PostPublicLdapParams) (*v1ldap.PostPublicLdapOK, error)
	GetPublicsLdap(params *v1ldap.GetPublicsLdapParams) (*v1ldap.GetPublicsLdapOK, error)
}

func CreateLDAP(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)

	name := c.String(FlName.Name)
	domain := c.String(FlLdapDomain.Name)
	bindDn := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)

	directoryType := c.String(FlLdapDirectoryType.Name)
	userSearchBase := c.String(FlLdapUserSearchBase.Name)
	userDnPattern := c.String(FlLdapUserDnPattern.Name)
	userNameAttribute := c.String(FlLdapUserNameAttribute.Name)
	userObjectClass := c.String(FlLdapUserObjectClass.Name)

	groupSearchBase := c.String(FlLdapGroupSearchBase.Name)
	groupMemberAttribute := c.String(FlLdapGroupMemberAttribute.Name)
	groupNameAttribute := c.String(FlLdapGroupNameAttribute.Name)
	groupObjectClass := c.String(FlLdapGroupObjectClass.Name)
	adminGroup := c.String(FlLdapAdminGroup.Name)

	server := c.String(FlLdapServer.Name)
	public := c.Bool(FlPublicOptional.Name)

	ldapRegexp := regexp.MustCompile("^(?:ldap://|ldaps://)[a-z0-9-.]+:\\d+$")
	if !ldapRegexp.MatchString(server) {
		utils.LogErrorMessageAndExit("Invalid ldap server address format, e.g: ldaps://10.0.0.1:389")
	}

	portSeparatorIndex := strings.LastIndex(server, ":")
	serverPort, _ := strconv.Atoi(server[portSeparatorIndex+1:])
	protocol := server[0:strings.Index(server, ":")]

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	return createLDAPImpl(cbClient.Cloudbreak.V1ldap, int32(serverPort), name, server, protocol, domain, bindDn, bindPassword, directoryType,
		userSearchBase, userDnPattern, userNameAttribute, userObjectClass, groupSearchBase, groupMemberAttribute, groupNameAttribute, groupObjectClass, adminGroup, public)
}

func createLDAPImpl(ldapClient ldapClient, port int32, name, server, protocol, domain, bindDn, bindPassword, directoryType,
	userSearchBase, userDnPattern, userNameAttribute, userObjectClass, groupSearchBase, groupMemberAttribute, groupNameAttribute,
	groupObjectClass, adminGroup string, public bool) error {
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
		UserDnPattern:        &userDnPattern,
		UserNameAttribute:    userNameAttribute,
		UserObjectClass:      userObjectClass,
		GroupMemberAttribute: groupMemberAttribute,
		GroupNameAttribute:   groupNameAttribute,
		GroupObjectClass:     groupObjectClass,
		GroupSearchBase:      groupSearchBase,
		AdminGroup:           adminGroup,
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
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list ldap configs")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

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
			UserDnPattern:        utils.SafeStringConvert(l.UserDnPattern),
			UserNameAttribute:    l.UserNameAttribute,
			UserObjectClass:      l.UserObjectClass,
			GroupMemberAttribute: l.GroupMemberAttribute,
			GroupNameAttribute:   l.GroupNameAttribute,
			GroupObjectClass:     l.GroupObjectClass,
			GroupSearchBase:      l.GroupSearchBase,
			AdminGroup:           l.AdminGroup,
		}
		tableRows = append(tableRows, row)
	}

	writer(LdapHeader, tableRows)
	return nil
}

func DeleteLdap(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete an ldap")

	ldapName := c.String(FlName.Name)
	log.Infof("[DeleteLdap] delete ldap config by name: %s", ldapName)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	if err := cbClient.Cloudbreak.V1ldap.DeletePublicLdap(v1ldap.NewDeletePublicLdapParams().WithName(ldapName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteLdap] ldap config deleted: %s", ldapName)
	return nil
}

func CreateLdapUser(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create ldap user")

	directoryType := c.String(FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)
	ldapServer := c.String(FlLdapServer.Name)
	userName := c.String(FlLdapUserToCreate.Name)
	password := c.String(FlLdapUserToCreatePassword.Name)
	email := c.String(FlLdapUserToCreateEmail.Name)
	baseDn := c.String(FlLdapUserToCreateBase.Name)
	groups := c.String(FlLdapUserToCreateGroups.Name)
	ldaps := c.Bool(FlLdapSecureOptional.Name)

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
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete ldap user")

	directoryType := c.String(FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)
	ldapServer := c.String(FlLdapServer.Name)
	userName := c.String(FlLdapUserToDelete.Name)
	baseDn := c.String(FlLdapUserToDeleteBase.Name)
	ldaps := c.Bool(FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	dn := fmt.Sprintf("CN=%s,%s", userName, baseDn)
	ldapDel(ldap, dn)

	return nil
}

func CreateLdapGroup(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create ldap group")

	directoryType := c.String(FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)
	ldapServer := c.String(FlLdapServer.Name)
	group := c.String(FlLdapGroupToCreate.Name)
	baseDn := c.String(FlLdapGroupToCreateBase.Name)
	ldaps := c.Bool(FlLdapSecureOptional.Name)

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
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete ldap group")

	directoryType := c.String(FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)
	ldapServer := c.String(FlLdapServer.Name)
	group := c.String(FlLdapGroupToDelete.Name)
	baseDn := c.String(FlLdapGroupToDeleteBase.Name)
	ldaps := c.Bool(FlLdapSecureOptional.Name)

	ldap := connectLdap(ldapServer, bindUser, bindPassword, ldaps)
	defer ldap.Close()

	dn := fmt.Sprintf("CN=%s,%s", group, baseDn)
	ldapDel(ldap, dn)

	return nil
}

func ListLdapUsers(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list ldap users")

	directoryType := c.String(FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)
	ldapServer := c.String(FlLdapServer.Name)
	searchBase := c.String(FlLdapUserSearchBase.Name)
	ldaps := c.Bool(FlLdapSecureOptional.Name)

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
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	output.WriteList(LdapUsers, tableRows)

	return nil
}

func ListLdapGroups(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list ldap groups")

	directoryType := c.String(FlLdapDirectoryType.Name)
	validateDirectoryTypeToManageUsers(directoryType)

	bindUser := c.String(FlLdapBindDN.Name)
	bindPassword := c.String(FlLdapBindPassword.Name)
	ldapServer := c.String(FlLdapServer.Name)
	searchBase := c.String(FlLdapGroupSearchBase.Name)
	ldaps := c.Bool(FlLdapSecureOptional.Name)

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
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	output.WriteList(LdapGroups, tableRows)

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
	var err error = nil
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
