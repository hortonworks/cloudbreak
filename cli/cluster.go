package cli

import (
	"encoding/json"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/cluster"
	"github.com/sequenceiq/hdc-cli/client/stacks"
	"github.com/sequenceiq/hdc-cli/models"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/go-swagger/go-swagger/errors"
	"github.com/go-swagger/go-swagger/httpkit/validate"
	"github.com/sequenceiq/hdc-cli/client/blueprints"
	"github.com/sequenceiq/hdc-cli/client/credentials"
	"github.com/sequenceiq/hdc-cli/client/templates"
)

var ClusterSkeletonListHeader []string = []string{"Cluster Name", "Status", "HDP Version", "Cluster Type"}

type ClusterSkeleton struct {
	ClusterName              string         `json:"ClusterName" yaml:"ClusterName"`
	HDPVersion               string         `json:"HDPVersion" yaml:"HDPVersion"`
	ClusterType              string         `json:"ClusterType" yaml:"ClusterType"`
	Master                   InstanceConfig `json:"Master" yaml:"Master"`
	Worker                   InstanceConfig `json:"Worker" yaml:"Worker"`
	InstanceCount            int32          `json:"InstanceCount" yaml:"InstanceCount"`
	SSHKeyName               string         `json:"SSHKeyName" yaml:"SSHKeyName"`
	RemoteAccess             string         `json:"RemoteAccess" yaml:"RemoteAccess"`
	WebAccess                bool           `json:"WebAccess" yaml:"WebAccess"`
	ClusterAndAmbariUser     string         `json:"ClusterAndAmbariUser" yaml:"ClusterAndAmbariUser"`
	ClusterAndAmbariPassword string         `json:"ClusterAndAmbariPassword" yaml:"ClusterAndAmbariPassword"`

	Status       string `json:"Status,omitempty" yaml:"Status,omitempty"`
	StatusReason string `json:"StatusReason,omitempty" yaml:"StatusReason,omitempty"`

	//InstanceRole             string `json:"InstanceRole" yaml:"InstanceRole"`
	//HiveMetastoreUrl         string `json:"HiveMetastoreUrl" yaml:"HiveMetastoreUrl"`
	//HiveMetastoreUser        string `json:"HiveMetastoreUser" yaml:"HiveMetastoreUser"`
	//HiveMetastorePassword    string `json:"HiveMetastorePassword" yaml:"HiveMetastorePassword"`
}

func (s *ClusterSkeleton) Validate() error {
	var res []error
	if err := validate.RequiredString("ClusterName", "body", string(s.ClusterName)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("HDPVersion", "body", string(s.HDPVersion)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("ClusterType", "body", string(s.ClusterType)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredNumber("InstanceCount", "body", float64(s.InstanceCount)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("SSHKeyName", "body", string(s.SSHKeyName)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("RemoteAccess", "body", string(s.RemoteAccess)); err != nil {
		res = append(res, err)
	}
	if err := validate.Required("WebAccess", "body", s.WebAccess); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("ClusterAndAmbariUser", "body", string(s.ClusterAndAmbariUser)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("ClusterAndAmbariPassword", "body", string(s.ClusterAndAmbariPassword)); err != nil {
		res = append(res, err)
	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}

type InstanceConfig struct {
	InstanceType  string `json:"InstanceType" yaml:"InstanceType"`
	VolumeType    string `json:"VolumeType" yaml:"VolumeType"`
	VolumeSize    int32  `json:"VolumeSize" yaml:"VolumeSize"`
	VolumeCount   int32  `json:"VolumeCount" yaml:"VolumeCount"`
	instanceCount int32
}

func (c *InstanceConfig) fill(instanceGroup *models.InstanceGroup, template *models.TemplateResponse) error {
	c.instanceCount = instanceGroup.NodeCount
	c.InstanceType = template.InstanceType
	c.VolumeType = *template.VolumeType
	c.VolumeSize = *template.VolumeSize
	c.VolumeCount = template.VolumeCount
	return nil
}

func (c *ClusterSkeleton) Json() string {
	j, _ := json.Marshal(c)
	return string(j)
}

func (c *ClusterSkeleton) JsonPretty() string {
	j, _ := json.MarshalIndent(c, "", "  ")
	return string(j)
}

func (c *ClusterSkeleton) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func (c *ClusterSkeleton) fill(stack *models.StackResponse, credential *models.CredentialResponse, blueprint *models.BlueprintResponse, templateMap map[string]*models.TemplateResponse, securityMap map[string][]*models.SecurityRule) error {
	c.ClusterName = stack.Name
	c.Status = *stack.Status
	if c.Status == "AVAILABLE" {
		c.Status = *stack.Cluster.Status
		c.StatusReason = *stack.Cluster.StatusReason
	} else {
		if stack.StatusReason != nil {
			c.StatusReason = *stack.StatusReason
		}
	}
	if stack.HdpVersion != nil {
		c.HDPVersion = *stack.HdpVersion
	}
	if blueprint != nil {
		c.ClusterType = blueprint.Name
	}

	for _, v := range stack.InstanceGroups {
		if v.Group == "master" {
			c.Master.fill(v, templateMap[v.Group])
		}
		if v.Group == "worker" {
			c.Worker.fill(v, templateMap[v.Group])
		}
	}
	if str, ok := credential.Parameters["existingKeyPairName"].(string); ok {
		c.SSHKeyName = str
	}

	keys := make([]string, 0, len(securityMap))
	for k := range securityMap {
		keys = append(keys, k)
	}
	c.RemoteAccess = strings.Join(keys, ",")

	for _, v := range securityMap {
		for _, sr := range v {
			log.Debugf("SecurityRule: %s", sr.Ports)
			if strings.Join(SECURITY_GROUP_DEFAULT_PORTS, ",") != sr.Ports {
				c.WebAccess = true
			}
		}
	}

	c.InstanceCount = c.Master.instanceCount + c.Worker.instanceCount
	c.ClusterAndAmbariUser = stack.Cluster.UserName
	c.ClusterAndAmbariPassword = stack.Cluster.Password

	return nil
}

func (c *ClusterSkeleton) DataAsStringArray() []string {
	return []string{c.ClusterName, c.Status, c.HDPVersion, c.ClusterType}
}

func FetchCluster(client *Cloudbreak, stack *models.StackResponse) (*ClusterSkeleton, error) {

	respCredential, _ := client.Cloudbreak.Credentials.GetCredentialsID(&credentials.GetCredentialsIDParams{ID: stack.CredentialID})

	var blueprint *models.BlueprintResponse = nil
	if stack.Cluster != nil && stack.Cluster.BlueprintID != nil {
		respBlueprint, _ := client.Cloudbreak.Blueprints.GetBlueprintsID(&blueprints.GetBlueprintsIDParams{ID: *stack.Cluster.BlueprintID})
		blueprint = respBlueprint.Payload
	}

	var templateMap map[string]*models.TemplateResponse = make(map[string]*models.TemplateResponse)
	for _, v := range stack.InstanceGroups {
		respTemplate, err := client.Cloudbreak.Templates.GetTemplatesID(&templates.GetTemplatesIDParams{ID: v.TemplateID})
		if err == nil {
			templateMap[v.Group] = respTemplate.Payload
		}
	}
	securityMap, _ := client.GetSecurityDetails(client, stack)
	clusterSkeleton := &ClusterSkeleton{}
	clusterSkeleton.fill(stack, respCredential.Payload, blueprint, templateMap, securityMap)
	return clusterSkeleton, nil
}

func DescribeCluster(c *cli.Context) error {
	oAuth2Client, err := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))
	if err != nil {
		log.Error(err)
		newExitReturnError()
	}

	clusterName := c.String(FlCBClusterName.Name)
	if len(clusterName) == 0 {
		log.Error(fmt.Sprintf("[DescribeCluster] You need to specify the %s parameter", FlCBClusterName.Name))
		return newExitError()
	}

	respStack, err := oAuth2Client.Cloudbreak.Stacks.GetStacksUserName(&stacks.GetStacksUserNameParams{Name: clusterName})
	if err != nil {
		log.Error(err)
		return newExitError()
	}
	clusterSkeleton, _ := FetchCluster(oAuth2Client, respStack.Payload)
	fmt.Println(clusterSkeleton.JsonPretty())

	return nil
}

func ListClusters(c *cli.Context) error {
	oAuth2Client, err := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))

	if err != nil {
		log.Error(err)
		newExitReturnError()
	}

	respStacks, err := oAuth2Client.Cloudbreak.Stacks.GetStacksUser(&stacks.GetStacksUserParams{})
	if err != nil {
		log.Error(err)
		return err
	}

	var tableRows []TableRow
	for _, stack := range respStacks.Payload {
		clusterSkeleton, _ := FetchCluster(oAuth2Client, stack)
		tableRows = append(tableRows, clusterSkeleton)
	}
	WriteTable(ClusterSkeletonListHeader, tableRows)
	return nil
}

func TerminateCluster(c *cli.Context) error {
	clusterName := c.String(FlCBClusterName.Name)

	if len(clusterName) == 0 {
		log.Error(fmt.Sprintf("[TerminateCluster] You need to specify the %s parameter", FlCBClusterName.Name))
		return newExitError()
	}

	log.Infof("[TerminateCluster] sending request to terminate cluster: %s")
	oAuth2Client, err := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))

	if err != nil {
		log.Error(err)
		newExitReturnError()
	}

	err = oAuth2Client.Cloudbreak.Stacks.DeleteStacksUserName(&stacks.DeleteStacksUserNameParams{Name: clusterName})

	if err != nil {
		log.Error(fmt.Sprintf("[TerminateCluster] Failed to terminate the cluster: %s, error: %s", clusterName, err.Error()))
		return newExitError()
	}

	oAuth2Client.waitForClusterToTerminate(clusterName, c)
	return nil
}

func AssembleClusterSkeleton(c *cli.Context) ClusterSkeleton {
	path := c.String(FlCBInputJson.Name)
	if len(path) == 0 {
		log.Errorf("[AssembleClusterSkeleton] missing parameter: %s", FlCBInputJson.Name)
		newExitReturnError()
	}

	if _, err := os.Stat(path); os.IsNotExist(err) {
		log.Errorf("[AssembleClusterSkeleton] %s", err.Error())
		newExitReturnError()
	}

	log.Infof("[AssembleClusterSkeleton] read cluster create json from file: %s", path)
	content, err := ioutil.ReadFile(path)
	if err != nil {
		log.Errorf("[AssembleClusterSkeleton] %s", err.Error())
		newExitReturnError()
	}

	var skeleton ClusterSkeleton
	err = json.Unmarshal(content, &skeleton)
	if err != nil {
		log.Errorf("[AssembleClusterSkeleton] %s", err.Error())
		newExitReturnError()
	}

	log.Infof("[AssembleClusterSkeleton] assemble cluster based on skeleton: %s", skeleton.Json())
	return skeleton
}

func CreateCluster(c *cli.Context) error {
	skeleton := AssembleClusterSkeleton(c)

	if err := skeleton.Validate(); err != nil {
		log.Errorf("[CreateCluster] %s", err)
		newExitReturnError()
	}

	oAuth2Client, err := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))

	if err != nil {
		log.Error(err)
		newExitReturnError()
	}

	blueprintId := oAuth2Client.GetBlueprintId(skeleton.ClusterType)

	var wg sync.WaitGroup
	wg.Add(4)

	credentialId := make(chan int64, 1)
	go oAuth2Client.CopyDefaultCredential(skeleton, credentialId, &wg)

	templateIds := make(chan int64, 2)
	go oAuth2Client.CreateTemplate(skeleton, templateIds, &wg)

	var secGroupId = make(chan int64, 1)
	go oAuth2Client.CreateSecurityGroup(skeleton, secGroupId, &wg)

	networkId := make(chan int64, 1)
	go oAuth2Client.CopyDefaultNetwork(skeleton, networkId, &wg)

	wg.Wait()

	// create stack

	stackId := func() int64 {
		failurePolicy := models.FailurePolicy{AdjustmentType: "BEST_EFFORT"}
		failureAction := "DO_NOTHING"
		masterType := "GATEWAY"
		workerType := "CORE"
		ambariVersion := "2.4"
		secGroupId := <-secGroupId
		platform := "AWS"

		instanceGroups := []*models.InstanceGroup{
			{
				Group:           "master",
				TemplateID:      <-templateIds,
				NodeCount:       1,
				Type:            &masterType,
				SecurityGroupID: secGroupId,
			},
			{
				Group:           "worker",
				TemplateID:      <-templateIds,
				NodeCount:       skeleton.InstanceCount - 1,
				Type:            &workerType,
				SecurityGroupID: secGroupId,
			},
		}

		var stackParameters = make(map[string]string)
		stackParameters["instanceProfileStrategy"] = "CREATE"

		orchestrator := models.OrchestratorRequest{Type: "SALT"}

		stackReq := models.StackRequest{
			Name:            skeleton.ClusterName,
			CredentialID:    <-credentialId,
			FailurePolicy:   &failurePolicy,
			OnFailureAction: &failureAction,
			InstanceGroups:  instanceGroups,
			Parameters:      stackParameters,
			CloudPlatform:   platform,
			PlatformVariant: &platform,
			NetworkID:       <-networkId,
			AmbariVersion:   &ambariVersion,
			HdpVersion:      &skeleton.HDPVersion,
			Orchestrator:    &orchestrator,
		}

		log.Infof("[CreateStack] sending stack create request with name: %s", skeleton.ClusterName)
		resp, err := oAuth2Client.Cloudbreak.Stacks.PostStacksUser(&stacks.PostStacksUserParams{&stackReq})

		if err != nil {
			log.Errorf("[CreateStack] %s", err.Error())
			newExitReturnError()
		}

		log.Infof("[CreateStack] stack created, id: %d", resp.Payload.ID)
		return resp.Payload.ID
	}()

	// create cluster

	func() {
		master := "master"
		worker := "worker"

		masterConstraint := models.Constraint{
			InstanceGroupName: &master,
			HostCount:         int32(1),
		}

		workerConstraint := models.Constraint{
			InstanceGroupName: &worker,
			HostCount:         int32(skeleton.InstanceCount - 1),
		}

		hostGroups := []*models.HostGroup{
			{
				Name:       master,
				Constraint: &masterConstraint,
			},
			{
				Name:       worker,
				Constraint: &workerConstraint,
			},
		}

		clusterReq := models.ClusterRequest{
			Name:        skeleton.ClusterName,
			BlueprintID: blueprintId,
			HostGroups:  hostGroups,
			UserName:    skeleton.ClusterAndAmbariUser,
			Password:    skeleton.ClusterAndAmbariPassword,
		}

		resp, err := oAuth2Client.Cloudbreak.Cluster.PostStacksIDCluster(&cluster.PostStacksIDClusterParams{ID: stackId, Body: &clusterReq})

		if err != nil {
			log.Errorf("[CreateCluster] %s", err.Error())
			newExitReturnError()
		}

		log.Infof("[CreateCluster] cluster created, id: %d", resp.Payload.ID)
	}()

	oAuth2Client.waitForClusterToFinish(stackId, c)
	return nil
}

func ValidateCreateClusterSkeleton(c *cli.Context) error {
	skeleton := AssembleClusterSkeleton(c)
	return skeleton.Validate()
}

func GenerateCreateClusterSkeleton(c *cli.Context) error {
	skeleton := ClusterSkeleton{
		ClusterType:   "EDW-ETL: Apache Spark 2.0-preview",
		HDPVersion:    "2.5",
		InstanceCount: 3,
		Master: InstanceConfig{
			InstanceType: "m4.xlarge",
			VolumeType:   "gp2",
			VolumeCount:  1,
			VolumeSize:   32,
		},
		Worker: InstanceConfig{
			InstanceType: "m3.xlarge",
			VolumeType:   "ephemeral",
			VolumeCount:  2,
			VolumeSize:   40,
		},
	}
	fmt.Println(skeleton.JsonPretty())
	return nil
}

func (c *Cloudbreak) waitForClusterToFinish(stackId int64, context *cli.Context) {
	if context.Bool(FlCBWait.Name) {
		defer timeTrack(time.Now(), "cluster installation")

		log.Infof("[WaitForClusterToFinish] wait for cluster to finish")
		for {
			resp, err := c.Cloudbreak.Stacks.GetStacksID(&stacks.GetStacksIDParams{ID: stackId})

			if err != nil {
				log.Infof("[WaitForClusterToFinish] %s", err.Error())
				newExitReturnError()
			}

			desiredStatus := "AVAILABLE"
			stackStatus := *resp.Payload.Status
			clusterStatus := *resp.Payload.Cluster.Status
			log.Infof("[WaitForClusterToFinish] stack status: %s, cluster status: %s", stackStatus, clusterStatus)

			if stackStatus == desiredStatus && clusterStatus == desiredStatus {
				log.Infof("[WaitForClusterToFinish] cluster successfully installed")
				break
			}
			if strings.Contains(stackStatus, "FAILED") || strings.Contains(clusterStatus, "FAILED") {
				log.Infof("[WaitForClusterToFinish] cluster installation failed")
				newExitReturnError()
			}

			log.Infof("[WaitForClusterToFinish] cluster is in progress, wait for 20 seconds")
			time.Sleep(20 * time.Second)
		}
	}
}

func (c *Cloudbreak) waitForClusterToTerminate(clusterName string, context *cli.Context) {
	if context.Bool(FlCBWait.Name) {
		defer timeTrack(time.Now(), "cluster termination")

		log.Infof("[waitForClusterToTerminate] wait for cluster to terminate")
		for {
			resp, err := c.Cloudbreak.Stacks.GetStacksUserName(&stacks.GetStacksUserNameParams{Name: clusterName})

			if err != nil {
				errorMessage := err.Error()
				// shouldn't happen, but handle anyway
				if strings.Contains(errorMessage, "status 404") {
					log.Infof("[waitForClusterToTerminate] cluster is terminated")
					break
				}
				log.Errorf("[waitForClusterToTerminate] %s", errorMessage)
				newExitReturnError()
			}

			stackStatus := *resp.Payload.Status
			log.Infof("[waitForClusterToTerminate] stack status: %s", stackStatus)

			if strings.Contains(stackStatus, "FAILED") {
				log.Infof("[waitForClusterToTerminate] cluster termination failed")
				newExitReturnError()
			}
			if strings.Contains(stackStatus, "DELETE_COMPLETED") {
				log.Infof("[waitForClusterToTerminate] cluster is terminated")
				break
			}

			log.Infof("[waitForClusterToTerminate] cluster is in progress, wait for 20 seconds")
			time.Sleep(20 * time.Second)
		}
	}
}
