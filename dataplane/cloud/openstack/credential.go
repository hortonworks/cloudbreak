package openstack

import (
	"errors"
	"fmt"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/cloud"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

var FACINGS = []string{"public", "admin", "internal"}
var SCOPES = []string{"project", "domain"}

func (p *OpenstackProvider) GetCredentialRequest(stringFinder func(string) string, govCloud bool) (*model.CredentialV4Request, error) {
	facing, err := validateAndGet(stringFinder("facing"), FACINGS)
	if err != nil {
		return nil, err
	}

	parameters := &model.OpenstackCredentialV4Parameters{
		Facing:   &facing,
		UserName: &(&types.S{S: stringFinder("tenant-user")}).S,
		Password: &(&types.S{S: stringFinder("tenant-password")}).S,
		Endpoint: &(&types.S{S: stringFinder("endpoint")}).S,
	}

	if len(stringFinder("user-domain")) != 0 {
		scope, err := validateAndGet(stringFinder("keystone-scope"), SCOPES)
		if err != nil {
			return nil, err
		}
		if "project" == scope {
			parameters.KeystoneV3 = &model.KeystoneV3Parameters{
				Project: &model.ProjectKeystoneV3Parameters{
					UserDomain:        &(&types.S{S: stringFinder("user-domain")}).S,
					ProjectDomainName: &(&types.S{S: stringFinder("project-domain-name")}).S,
					ProjectName:       &(&types.S{S: stringFinder("project-name")}).S,
				},
			}
		} else {
			parameters.KeystoneV3 = &model.KeystoneV3Parameters{
				Domain: &model.DomainKeystoneV3Parameters{
					UserDomain: &(&types.S{S: stringFinder("user-domain")}).S,
					DomainName: &(&types.S{S: stringFinder("domain-name")}).S,
				},
			}
		}
	} else {
		parameters.KeystoneV2 = &model.KeystoneV2Parameters{
			TenantName: &(&types.S{S: stringFinder("tenant-name")}).S,
		}
	}

	credReq := cloud.CreateBaseCredentialRequest(stringFinder)
	credReq.Openstack = parameters
	return credReq, nil
}

func validateAndGet(value string, values []string) (string, error) {
	if len(value) == 0 {
		return values[0], nil
	} else {
		for _, v := range values {
			if v == value {
				return value, nil
			}
		}
	}
	return "", errors.New(fmt.Sprintf("%s not allowed", value))
}
