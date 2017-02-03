package cli

import (
	"encoding/json"
	"errors"
	"io/ioutil"
	"os"
	"strconv"
	"strings"
	"time"

	"fmt"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
)

func (c *ClusterSkeleton) Json() string {
	j, _ := json.Marshal(c)
	return string(j)
}

func (c *ClusterSkeleton) JsonPretty() string {
	j, _ := json.MarshalIndent(c, "", "  ")
	return string(j)
}

func (c *ClusterSkeletonResult) DataAsStringArray() []string {
	return []string{c.ClusterName, c.HDPVersion, c.ClusterType, c.Master.Yaml(), c.Worker.Yaml(), c.Compute.Yaml(),
		c.SSHKeyName, c.RemoteAccess, strconv.FormatBool(c.WebAccess), c.ClusterAndAmbariUser, c.Status, c.StatusReason}
}

func (c *InstanceConfig) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func (c *InstanceConfig) fill(instanceGroup *models.InstanceGroupResponse, template *models.TemplateResponse) error {
	c.InstanceCount = instanceGroup.NodeCount
	c.InstanceType = template.InstanceType
	c.VolumeType = SafeStringConvert(template.VolumeType)
	c.VolumeSize = &template.VolumeSize
	c.VolumeCount = &template.VolumeCount
	return nil
}

func assembleClusterSkeleton(c *cli.Context) ClusterSkeleton {
	path := c.String(FlInputJson.Name)
	if len(path) == 0 {
		logMissingParameterAndExit(c, assembleClusterSkeleton)
	}

	if _, err := os.Stat(path); os.IsNotExist(err) {
		logErrorAndExit(assembleClusterSkeleton, err.Error())
	}

	log.Infof("[AssembleClusterSkeleton] read cluster create json from file: %s", path)
	content, err := ioutil.ReadFile(path)
	if err != nil {
		logErrorAndExit(assembleClusterSkeleton, err.Error())
	}

	var skeleton ClusterSkeleton
	err = json.Unmarshal(content, &skeleton)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		logErrorAndExit(assembleClusterSkeleton, msg)
	}

	log.Infof("[AssembleClusterSkeleton] assemble cluster based on skeleton: %s", skeleton.Json())
	return skeleton
}

func (c *ClusterSkeletonResult) fill(
	stack *models.StackResponse,
	credential *models.CredentialResponse,
	blueprint *models.BlueprintResponse,
	templateMap map[string]*models.TemplateResponse,
	securityMap map[string][]*models.SecurityRuleResponse,
	network *models.NetworkResponse,
	rdsConfig *models.RDSConfigResponse,
	recipeMap map[string][]*models.RecipeResponse,
	recoveryModeMap map[string]string) error {

	if stack == nil {
		return errors.New("Stack definition is not returned from Cloudbreak")
	}
	c.ClusterName = stack.Name

	parameters := stack.Parameters
	if len(parameters) > 0 && len(parameters["instanceProfileStrategy"]) > 0 {
		if parameters["instanceProfileStrategy"] == "USE_EXISTING" {
			c.InstanceRole = parameters["instanceProfile"]
		} else {
			c.InstanceRole = parameters["instanceProfileStrategy"]
		}
	}

	c.Status = SafeStringConvert(stack.Status)
	c.StatusReason = SafeStringConvert(stack.StatusReason)
	if stack.Cluster != nil {
		clusterStatus := SafeStringConvert(stack.Cluster.Status)
		clusterStatusReason := SafeStringConvert(stack.Cluster.StatusReason)
		if clusterStatus != "AVAILABLE" {
			c.Status = clusterStatus
		}
		if clusterStatusReason != "" {
			c.StatusReason = clusterStatusReason
		}

		c.ClusterAndAmbariUser = SafeStringConvert(stack.Cluster.UserName)
		if len(stack.Cluster.BlueprintInputs) > 0 {
			var inputs = make(map[string]string)
			for _, input := range stack.Cluster.BlueprintInputs {
				inputs[*input.Name] = *input.PropertyValue
			}
			c.ClusterInputs = inputs
		}
		c.Worker.RecoveryMode = recoveryModeMap[WORKER]
		c.Compute.RecoveryMode = recoveryModeMap[COMPUTE]

		fillWebAccess(stack.Cluster, c)

	}

	c.HDPVersion = SafeStringConvert(stack.HdpVersion)
	if len(c.HDPVersion) > 3 {
		c.HDPVersion = c.HDPVersion[0:3]
	}

	if blueprint != nil {
		c.ClusterType = getFancyBlueprintName(blueprint)
	}

	if network != nil && network.Parameters["vpcId"] != nil && network.Parameters["subnetId"] != nil {
		net := Network{VpcId: network.Parameters["vpcId"].(string), SubnetId: network.Parameters["subnetId"].(string)}
		c.Network = &net
	}

	if rdsConfig != nil {
		rdsConfig := HiveMetastoreResult{
			Name: rdsConfig.Name,
		}
		c.HiveMetastore = &rdsConfig
	}

	c.Master.Recipes = convertRecipes(recipeMap[MASTER])
	c.Worker.Recipes = convertRecipes(recipeMap[WORKER])
	c.Compute.Recipes = convertRecipes(recipeMap[COMPUTE])

	if stack.InstanceGroups != nil {
		for _, v := range stack.InstanceGroups {
			if c.Nodes == UNHEALTHY {
				break
			}
			for _, metadata := range v.Metadata {
				hostStatus := getHostStatus(stack, metadata)
				if hostStatus == UNHEALTHY {
					c.Nodes = UNHEALTHY
					break
				} else if len(hostStatus) > 0 {
					c.Nodes = HEALTHY
				}
			}
		}
		if len(c.Nodes) == 0 {
			c.Nodes = UNKNOWN
		}
	} else {
		c.Nodes = UNKNOWN
	}

	if securityMap != nil {
		if stack.InstanceGroups != nil {
			for _, v := range stack.InstanceGroups {
				if v.Group == MASTER {
					c.Master.fill(v, templateMap[v.Group])
				}
				if v.Group == WORKER {
					c.Worker.fill(v, templateMap[v.Group])
				}
				if v.Group == COMPUTE {
					c.Compute.fill(v, templateMap[v.Group])
					if templateMap[v.Group].Parameters != nil && templateMap[v.Group].Parameters["spotPrice"] != nil {
						c.Compute.SpotPrice = string(templateMap[v.Group].Parameters["spotPrice"].(json.Number))
					}
				}
			}
		}
	}

	if credential != nil {
		if str, ok := credential.Parameters["existingKeyPairName"].(string); ok {
			c.SSHKeyName = str
		}
	}

	keys := make([]string, 0, len(securityMap))
	for k, _ := range securityMap {
		keys = append(keys, k)
	}
	c.RemoteAccess = strings.Join(keys, ",")

	if stack.Cluster != nil && stack.Cluster.BlueprintCustomProperties != nil {
		c.Configurations = stack.Cluster.BlueprintCustomProperties
	}

	var tags = make(map[string]string, 0)
	if len(stack.Tags) > 0 {
		userTags := stack.Tags[USER_TAGS]
		if userTags != nil {
			for k, v := range userTags.(map[string]interface{}) {
				tags[k] = v.(string)
			}
		}
	}
	c.Tags = tags

	return nil
}

func fillWebAccess(cluster *models.ClusterResponse, skeleton *ClusterSkeletonResult) {
	serviceMap := make(map[string]int)
	for index, service := range cluster.ExposedKnoxServices {
		serviceMap[service] = index
	}
	for _, service := range BASE_EXPOSED_SERVICES {
		if _, ok := serviceMap[service]; ok {
			skeleton.WebAccess = true
		}
	}

	for _, service := range HIVE_EXPOSED_SERVICES {
		if _, ok := serviceMap[service]; ok {
			skeleton.HiveJDBCAccess = true
		}
	}

	for _, service := range CLUSTER_MANAGER_EXPOSED_SERVICES {
		if _, ok := serviceMap[service]; ok {
			skeleton.ClusterComponentAccess = true
		}
	}
}

func convertRecipes(recipes []*models.RecipeResponse) []Recipe {
	var convertedRecipes []Recipe = []Recipe{}
	for _, resp := range recipes {
		convertedRecipes = append(convertedRecipes, Recipe{URI: *resp.URI, Phase: strings.ToLower(resp.RecipeType)})
	}
	return convertedRecipes
}

func (c *Cloudbreak) waitForClusterToFinish(stackId int64, context *cli.Context) {
	if context.Bool(FlWait.Name) {
		defer timeTrack(time.Now(), "cluster installation/update")

		log.Infof("[WaitForClusterToFinish] wait for cluster to finish")
		waitForClusterToFinishImpl(stackId, c.Cloudbreak.Stacks.GetStacksID)
	}
}

func waitForClusterToFinishImpl(stackId int64, getStack func(params *stacks.GetStacksIDParams) (*stacks.GetStacksIDOK, error)) {
	for {
		resp, err := getStack(&stacks.GetStacksIDParams{ID: stackId})

		if err != nil {
			logErrorAndExit(waitForClusterToFinishImpl, err.Error())
		}

		desiredStatus := "AVAILABLE"
		stackStatus := *resp.Payload.Status
		clusterStatus := *resp.Payload.Cluster.Status
		log.Infof("[WaitForClusterToFinish] stack status: %s, cluster status: %s", stackStatus, clusterStatus)

		if stackStatus == desiredStatus && clusterStatus == desiredStatus {
			log.Infof("[WaitForClusterToFinish] cluster operation successfully finished")
			break
		}
		if strings.Contains(stackStatus, "FAILED") || strings.Contains(clusterStatus, "FAILED") {
			logErrorAndExit(waitForClusterToFinishImpl, "cluster operation failed")
		}

		log.Infof("[WaitForClusterToFinish] cluster is in progress, wait for 20 seconds")
		time.Sleep(20 * time.Second)
	}
}

func (c *Cloudbreak) waitForClusterToTerminate(clusterName string, context *cli.Context) {
	if context.Bool(FlWait.Name) {
		defer timeTrack(time.Now(), "cluster termination")

		log.Infof("[waitForClusterToTerminate] wait for cluster to terminate")
		waitForClusterToTerminateImpl(clusterName, c.Cloudbreak.Stacks.GetStacksUserName)
	}
}

func waitForClusterToTerminateImpl(clusterName string, getStack func(*stacks.GetStacksUserNameParams) (*stacks.GetStacksUserNameOK, error)) {
	for {
		resp, err := getStack(&stacks.GetStacksUserNameParams{Name: clusterName})

		if err != nil {
			errorMessage := err.Error()
			// shouldn't happen, but handle anyway
			if strings.Contains(errorMessage, "status 404") {
				log.Infof("[waitForClusterToTerminate] cluster is terminated")
				break
			}
			logErrorAndExit(waitForClusterToTerminateImpl, errorMessage)
		}

		stackStatus := *resp.Payload.Status
		log.Infof("[waitForClusterToTerminate] stack status: %s", stackStatus)

		if strings.Contains(stackStatus, "FAILED") {
			logErrorAndExit(waitForClusterToTerminateImpl, "cluster termination failed")
		}
		if strings.Contains(stackStatus, "DELETE_COMPLETED") {
			log.Infof("[waitForClusterToTerminate] cluster is terminated")
			break
		}

		log.Infof("[waitForClusterToTerminate] cluster is in progress, wait for 20 seconds")
		time.Sleep(20 * time.Second)
	}
}
