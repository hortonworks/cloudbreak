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
	"github.com/hortonworks/hdc-cli/client_cloudbreak/stacks"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
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

func (c *InstanceConfig) fill(instanceGroup *models_cloudbreak.InstanceGroupResponse, template *models_cloudbreak.TemplateResponse) error {
	c.InstanceCount = instanceGroup.NodeCount
	c.InstanceType = template.InstanceType
	c.VolumeType = SafeStringConvert(template.VolumeType)
	c.VolumeSize = &template.VolumeSize
	c.VolumeCount = &template.VolumeCount
	parameters := template.Parameters
	if len(parameters) > 0 {
		if encrypted, ok := parameters[ENCRYPTED]; ok {
			c.Encrypted = &(&boolWrapper{encrypted.(bool)}).b
		}
	} else {
		c.Encrypted = &(&boolWrapper{false}).b
	}
	return nil
}

func assembleClusterSkeleton(c *cli.Context) ClusterSkeleton {
	path := c.String(FlInputJson.Name)
	if len(path) == 0 {
		logMissingParameterAndExit(c, []string{FlInputJson.Name})
	}

	if _, err := os.Stat(path); os.IsNotExist(err) {
		logErrorAndExit(err)
	}

	log.Infof("[AssembleClusterSkeleton] read cluster create json from file: %s", path)
	content, err := ioutil.ReadFile(path)
	if err != nil {
		logErrorAndExit(err)
	}

	var skeleton ClusterSkeleton
	err = json.Unmarshal(content, &skeleton)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		logErrorAndExit(errors.New(msg))
	}

	ambariPassword := c.String(FlAmbariPasswordOptional.Name)
	if len(ambariPassword) != 0 {
		skeleton.ClusterAndAmbariPassword = ambariPassword
	}

	clusterNameParam := c.String(FlClusterNameParamOptional.Name)
	if len(clusterNameParam) != 0 {
		skeleton.ClusterName = clusterNameParam
	}

	return skeleton
}

func (c *ClusterSkeletonResult) fill(
	stack *models_cloudbreak.StackResponse,
	credential *models_cloudbreak.CredentialResponse,
	blueprint *models_cloudbreak.BlueprintResponse,
	templateMap map[string]*models_cloudbreak.TemplateResponse,
	securityMap map[string][]*models_cloudbreak.SecurityRuleResponse,
	network *models_cloudbreak.NetworkResponse,
	rdsConfigs []*models_cloudbreak.RDSConfigResponse,
	recipeMap map[string][]*models_cloudbreak.RecipeResponse,
	recoveryModeMap map[string]string,
	autoscaling *AutoscalingSkeletonResult) error {

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
		c.Worker.RecoveryMode = recoveryModeMap[WORKER]
		c.Compute.RecoveryMode = recoveryModeMap[COMPUTE]

		fillWebAccess(stack.Cluster, c)

		if stack.Cluster.LdapConfig != nil {
			c.Ldap = &stack.Cluster.LdapConfig.Name
		}

		if stack.Cluster.AmbariDatabaseDetails != nil {
			db := stack.Cluster.AmbariDatabaseDetails
			if db.Host != "localhost" {
				c.AmbariDatabase = &AmbariDatabase{
					DatabaseName: db.Name,
					Host:         db.Host,
					Port:         db.Port,
					DatabaseType: db.Vendor,
				}
			}
		}
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

	for _, rds := range rdsConfigs {
		if rds.Type != nil {
			if *rds.Type == HIVE_RDS {
				rdsConfig := HiveMetastoreResult{
					Name: rds.Name,
				}
				c.HiveMetastore = &rdsConfig
			} else if *rds.Type == DRUID_RDS {
				rdsConfig := DruidMetastoreResult{
					Name: rds.Name,
				}
				c.DruidMetastore = &rdsConfig
			}
		}
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

	if autoscaling != nil && (autoscaling.Configuration != nil || len(autoscaling.Policies) > 0) {
		c.Autoscaling = autoscaling
	}

	if stack.FlexSubscription != nil {
		c.FlexSubscription = &FlexSubscriptionBase{stack.FlexSubscription.Name}
	}

	return nil
}

func fillWebAccess(cluster *models_cloudbreak.ClusterResponse, skeleton *ClusterSkeletonResult) {
	serviceMap := make(map[string]int)

	if cluster.Gateway != nil {
		for index, service := range cluster.Gateway.ExposedServices {
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
}

func convertRecipes(recipes []*models_cloudbreak.RecipeResponse) []Recipe {
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
		waitForClusterToFinishImpl(stackId, c.Cloudbreak.Stacks.GetStack)
	}
}

func waitForClusterToFinishImpl(stackId int64, getStack func(params *stacks.GetStackParams) (*stacks.GetStackOK, error)) {
	for {
		resp, err := getStack(&stacks.GetStackParams{ID: stackId})

		if err != nil {
			logErrorAndExit(err)
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
			logErrorAndExit(errors.New("cluster operation failed"))
		}

		log.Infof("[WaitForClusterToFinish] cluster is in progress, wait for 20 seconds")
		time.Sleep(20 * time.Second)
	}
}

func (c *Cloudbreak) waitForClusterToTerminate(clusterName string, context *cli.Context) {
	if context.Bool(FlWait.Name) {
		defer timeTrack(time.Now(), "cluster termination")

		log.Infof("[waitForClusterToTerminate] wait for cluster to terminate")
		waitForClusterToTerminateImpl(clusterName, c.Cloudbreak.Stacks.GetPrivateStack)
	}
}

func waitForClusterToTerminateImpl(clusterName string, getStack func(*stacks.GetPrivateStackParams) (*stacks.GetPrivateStackOK, error)) {
	for {
		resp, err := getStack(&stacks.GetPrivateStackParams{Name: clusterName})

		if err != nil {
			errorMessage := err.Error()
			if strings.Contains(errorMessage, "status code: 404") {
				log.Infof("[waitForClusterToTerminate] cluster is terminated")
				break
			}
			logErrorAndExit(errors.New(errorMessage))
		}

		stackStatus := *resp.Payload.Status
		log.Infof("[waitForClusterToTerminate] stack status: %s", stackStatus)

		if strings.Contains(stackStatus, "FAILED") {
			logErrorAndExit(errors.New("cluster termination failed"))
		}
		if strings.Contains(stackStatus, "DELETE_COMPLETED") {
			log.Infof("[waitForClusterToTerminate] cluster is terminated")
			break
		}

		log.Infof("[waitForClusterToTerminate] cluster is in progress, wait for 20 seconds")
		time.Sleep(20 * time.Second)
	}
}
