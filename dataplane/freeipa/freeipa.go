package freeipa

import (
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1freeipa"
	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1freeipauser"
	freeIpaModel "github.com/hortonworks/cb-cli/dataplane/api-freeipa/model"
	"github.com/hortonworks/cb-cli/dataplane/env"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

type ClientFreeIpa oauth.FreeIpa

func CreateFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "create FreeIpa cluster")
	FreeIpaRequest := assembleFreeIpaRequest(c)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipa.CreateFreeIpaV1(v1freeipa.NewCreateFreeIpaV1Params().WithBody(FreeIpaRequest))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	freeIpaCluster := resp.Payload
	if freeIpaCluster.Name != nil {
		log.Infof("[createFreeIpa] FreeIpa cluster created with name: %s", *freeIpaCluster.Name)
	}
}

func DeleteFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "delete FreeIpa cluster")
	envName := c.String(fl.FlEnvironmentName.Name)
	envCrn := env.GetEnvirontmentCrnByName(c, envName)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	err := freeIpaClient.V1freeipa.DeleteFreeIpaByEnvironmentV1(v1freeipa.NewDeleteFreeIpaByEnvironmentV1Params().WithEnvironment(&envCrn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[deleteFreeIpa] FreeIpa cluster delete requested in enviornment %s", envName)
}

func DescribeFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "describe FreeIpa cluster")
	envName := c.String(fl.FlEnvironmentName.Name)
	envCrn := env.GetEnvirontmentCrnByName(c, envName)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipa.GetFreeIpaByEnvironmentV1(v1freeipa.NewGetFreeIpaByEnvironmentV1Params().WithEnvironment(&envCrn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[describeFreeIpa] FreeIpa cluster describe requested in enviornment %s", envName)
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	iparesp := resp.Payload
	freeIpaOut := freeIpaOutDescibe{
		*iparesp.Crn,
		&freeIpa{
			EnvironmentCrn: *iparesp.EnvironmentCrn,
			Name:           *iparesp.Name,
			Placement: &placement{
				AvailabilityZone: iparesp.Placement.AvailabilityZone,
				Region:           *iparesp.Placement.Region,
			},
			InstanceGroups: convertInstanceGroupModel(iparesp.InstanceGroups),
			Authentication: &authentication{
				PublicKey:     iparesp.Authentication.PublicKey,
				PublicKeyID:   iparesp.Authentication.PublicKeyID,
				LoginUserName: iparesp.Authentication.LoginUserName,
			},
			Image: &image{
				Catalog: iparesp.Image.Catalog,
				ID:      iparesp.Image.ID,
				Os:      iparesp.Image.Os,
			},
			FreeIpaServer: &freeIpaServer{
				Domain:   *iparesp.FreeIpa.Domain,
				Hostname: *iparesp.FreeIpa.Hostname,
			},
		},
	}
	if iparesp.Network.Aws != nil {
		freeIpaOut.Network = &network{
			Aws: &awsNetwork{
				VpcID:    iparesp.Network.Aws.VpcID,
				SubnetID: iparesp.Network.Aws.SubnetID,
			},
		}
	}
	if iparesp.Network.Azure != nil {
		freeIpaOut.Network = &network{
			Azure: &azureNetwork{
				NetworkID:         iparesp.Network.Azure.NetworkID,
				NoFirewallRules:   strconv.FormatBool(iparesp.Network.Azure.NoFirewallRules),
				NoPublicIP:        strconv.FormatBool(iparesp.Network.Azure.NoPublicIP),
				ResourceGroupName: iparesp.Network.Azure.ResourceGroupName,
				SubnetID:          iparesp.Network.Azure.SubnetID,
			},
		}
	}
	output.Write(header, &freeIpaOut)
}

func ListFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "list FreeIpa clusters")
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipa.ListFreeIpaClustersByAccountV1(v1freeipa.NewListFreeIpaClustersByAccountV1Params())
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[listFreeIpa] FreeIpa clusters list requested.")
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	var tableRows []commonutils.Row

	for _, response := range resp.Payload {
		row := &freeIpaDetails{
			Name:           *response.Name,
			CRN:            *response.Crn,
			EnvironmentCrn: *response.EnvironmentCrn,
			Status:         response.Status,
		}
		tableRows = append(tableRows, row)
	}
	output.WriteList(listHeader, tableRows)
}

func (f *freeIpaOutDescibe) DataAsStringArray() []string {
	return append(f.freeIpa.DataAsStringArray(), f.Crn)
}

func (f *freeIpa) DataAsStringArray() []string {
	var instanceGroups string
	for _, ig := range f.InstanceGroups {
		var group string
		group += fmt.Sprintf("Name: %s\n", ig.Name)
		group += fmt.Sprintf("InstanceTemplate: %s\n", ig.InstanceTemplate.DataAsString())
		group += fmt.Sprintf("SecurityGroup: %s\n", ig.SecurityGroup.DataAsString())
		group += fmt.Sprintf("NodeCount: %s\n", ig.NodeCount)
		group += fmt.Sprint("MetaData:\n")
		for _, metadata := range ig.MetaData {
			group += fmt.Sprintf("%s\n", metadata.DataAsString())
		}
		instanceGroups += fmt.Sprintf("%s\n\n", group)
	}
	freeIpaArr := []string{f.EnvironmentCrn, f.Name}
	freeIpaArr = append(freeIpaArr, f.Placement.DataAsString())
	freeIpaArr = append(freeIpaArr, f.Authentication.DataAsString())
	freeIpaArr = append(freeIpaArr, f.Network.DataAsString())
	freeIpaArr = append(freeIpaArr, f.Image.DataAsString())
	freeIpaArr = append(freeIpaArr, f.FreeIpaServer.DataAsString())
	freeIpaArr = append(freeIpaArr, instanceGroups)
	return freeIpaArr
}

func (it *instanceTemplate) DataAsString() string {
	var template string
	template += fmt.Sprintf("  InstanceType: %s\n", it.InstanceType)
	template += fmt.Sprint("  Volumes:\n")
	return template
}

func (sg *securityGroup) DataAsString() string {
	var group string
	group += fmt.Sprintf("  SecurityGroupIDs: %s\n", strings.Join(sg.SecurityGroupIDs, ", "))
	group += fmt.Sprintf("  SecurityRules:\n")
	for _, rules := range sg.SecurityRules {
		group += fmt.Sprintf("    Subnet: %s\n", rules.Subnet)
		group += fmt.Sprintf("    Ports: %s\n", strings.Join(rules.Ports, ", "))
		group += fmt.Sprintf("    Protocol: %s\n", rules.Protocol)
		group += fmt.Sprintf("    Modifiable: %s\n", rules.Modifiable)
	}
	return group
}

func (m *metadata) DataAsString() string {
	var md string
	md += fmt.Sprintf("  DiscoveryFQDN: %s\n", m.DiscoveryFQDN)
	md += fmt.Sprintf("  InstanceID: %s\n", m.InstanceID)
	md += fmt.Sprintf("  InstanceStatus: %s\n", m.InstanceStatus)
	md += fmt.Sprintf("  InstanceType: %s\n", m.InstanceType)
	md += fmt.Sprintf("  PrivateIP: %s\n", m.PrivateIP)
	md += fmt.Sprintf("  PublicIP: %s\n", m.PublicIP)
	md += fmt.Sprintf("  State: %s\n", m.State)
	return md
}

func (p *placement) DataAsString() string {
	var plc string
	plc += fmt.Sprintf("  AvailabilityZone: %s\n", p.AvailabilityZone)
	plc += fmt.Sprintf("  Region: %s\n", p.Region)
	return plc
}

func (a *authentication) DataAsString() string {
	var auth string
	auth += fmt.Sprintf("  PublicKey: %s\n", a.PublicKey)
	auth += fmt.Sprintf("  PublicKeyID: %s\n", a.PublicKeyID)
	auth += fmt.Sprintf("  PublicKey: %s\n", a.PublicKey)
	return auth
}

func (n *network) DataAsString() string {
	var nw string
	if n.Aws != nil {
		nw += fmt.Sprint("  Aws:\n")
		nw += fmt.Sprintf("    VpcID: %s\n", n.Aws.VpcID)
		nw += fmt.Sprintf("    SubnetID: %s\n", n.Aws.SubnetID)
	}
	if n.Azure != nil {
		nw += fmt.Sprint("  Azure:\n")
		nw += fmt.Sprintf("    NetworkID: %s\n", n.Azure.NetworkID)
		nw += fmt.Sprintf("    NoFirewallRules: %s\n", n.Azure.NoFirewallRules)
		nw += fmt.Sprintf("    NoPublicIP: %s\n", n.Azure.NoPublicIP)
		nw += fmt.Sprintf("    ResourceGroupName: %s\n", n.Azure.ResourceGroupName)
		nw += fmt.Sprintf("    SubnetID: %s\n", n.Azure.SubnetID)
	}
	return nw
}

func (i *image) DataAsString() string {
	var img string
	img += fmt.Sprintf("  Catalog: %s\n", i.Catalog)
	img += fmt.Sprintf("  ID: %s\n", i.ID)
	img += fmt.Sprintf("  Os: %s\n", i.Os)
	return img
}

func (f *freeIpaServer) DataAsString() string {
	var srv string
	srv += fmt.Sprintf("  Domain: %s\n", f.Domain)
	srv += fmt.Sprintf("  Hostname: %s\n", f.Hostname)
	return srv
}

func convertInstanceGroupModel(igmodels []*freeIpaModel.InstanceGroupV1Response) []instanceGroup {
	var instanceGroups []instanceGroup
	for _, ig := range igmodels {
		var mdArray = make([]metadata, 0)
		for _, md := range ig.MetaData {
			mdArray = append(mdArray, metadata{
				DiscoveryFQDN:  md.DiscoveryFQDN,
				InstanceID:     md.InstanceID,
				InstanceStatus: md.InstanceStatus,
				InstanceType:   md.InstanceType,
				PrivateIP:      md.PrivateIP,
				PublicIP:       md.PublicIP,
				State:          md.State,
			})
		}
		instanceGroups = append(instanceGroups, instanceGroup{
			Name:      *ig.Name,
			NodeCount: fmt.Sprint(*ig.NodeCount),
			InstanceTemplate: &instanceTemplate{
				InstanceType: ig.InstanceTemplate.InstanceType,
			},
			SecurityGroup: &securityGroup{
				SecurityGroupIDs: ig.SecurityGroup.SecurityGroupIds,
				SecurityRules:    converSecurityRulesModel(ig.SecurityGroup.SecurityRules),
			},
			MetaData: mdArray,
		})
	}
	return instanceGroups
}

func converSecurityRulesModel(rulemodel []*freeIpaModel.SecurityRuleV1Response) []securityRule {
	var securityRules []securityRule
	for _, rule := range rulemodel {
		securityRules = append(securityRules, securityRule{
			Subnet:     *rule.Subnet,
			Ports:      rule.Ports,
			Protocol:   *rule.Protocol,
			Modifiable: strconv.FormatBool(rule.Modifiable),
		})
	}
	return securityRules
}

func assembleFreeIpaRequest(c *cli.Context) *freeIpaModel.CreateFreeIpaV1Request {
	path := c.String(fl.FlInputJson.Name)
	if _, err := os.Stat(path); os.IsNotExist(err) {
		commonutils.LogErrorAndExit(err)
	}

	log.Infof("[assembleStackTemplate] read cluster create json from file: %s", path)
	content := commonutils.ReadFile(path)

	var req freeIpaModel.CreateFreeIpaV1Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	name := c.String(fl.FlName.Name)
	if len(name) != 0 {
		req.Name = &name
	}
	if req.Name == nil || len(*req.Name) == 0 {
		commonutils.LogErrorMessageAndExit("Name of the cluster must be set either in the template or with the --name command line option.")
	}
	if req.EnvironmentCrn == nil || len(*req.EnvironmentCrn) == 0 {
		environmentName := c.String(fl.FlEnvironmentNameOptional.Name)
		if len(environmentName) == 0 {
			commonutils.LogErrorMessageAndExit("Name of the environment must be set either in the template or with the --env-name command line option.")
		}
		crn := env.GetEnvirontmentCrnByName(c, environmentName)
		req.EnvironmentCrn = &crn
	}
	return &req
}

func SynchronizeAllUsers(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "sync users to FreeIpa")

	users := c.StringSlice(fl.FlIpaUsersSlice.Name)
	environments := c.StringSlice(fl.FlIpaEnvironmentsSlice.Name)
	SynchronizeAllUsersV1Request := freeIpaModel.SynchronizeAllUsersV1Request{
		Users:        users,
		Environments: environments,
	}

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SynchronizeAllUsersV1(v1freeipauser.NewSynchronizeAllUsersV1Params().WithBody(&SynchronizeAllUsersV1Request))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	synchronizeUsersResponse := resp.Payload
	log.Infof("[synchronizeAllUsers] User sync submitted with status: %s", *synchronizeUsersResponse.Status)
}

func SynchronizeCurrentUser(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "set user password in FreeIpa")

	var req freeIpaModel.SynchronizeUserV1Request

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SynchronizeUserV1(v1freeipauser.NewSynchronizeUserV1Params().WithBody(&req))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	synchronizeUserResponse := resp.Payload
	log.Infof("[synchronizeUser] Sync completed: %s", *synchronizeUserResponse)
}

func SetPassword(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "set user password in FreeIpa")

	password := c.String(fl.FlIpaUserPassword.Name)
	SetPasswordV1Request := freeIpaModel.SetPasswordV1Request{
		Password: password,
	}

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SetPasswordV1(v1freeipauser.NewSetPasswordV1Params().WithBody(&SetPasswordV1Request))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	setPasswordResponse := resp.Payload
	log.Infof("[setPassword] Set Password completed: %s", *setPasswordResponse)
}
