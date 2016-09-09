package cli

import (
	"encoding/json"
	"errors"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
	"strconv"
	"strings"
	"time"
)

func (c *ClusterSkeleton) Json() string {
	j, _ := json.Marshal(c)
	return string(j)
}

func (c *ClusterSkeleton) JsonPretty() string {
	j, _ := json.MarshalIndent(c, "", "  ")
	return string(j)
}

func (c *ClusterSkeleton) DataAsStringArray() []string {
	return []string{c.ClusterName, c.HDPVersion, c.ClusterType, c.Master.Yaml(), c.Worker.Yaml(),
		c.SSHKeyName, c.RemoteAccess, strconv.FormatBool(c.WebAccess), c.ClusterAndAmbariUser, c.Status, c.StatusReason}
}

func (c *InstanceConfig) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func (c *InstanceConfig) fill(instanceGroup *models.InstanceGroup, template *models.TemplateResponse) error {
	c.InstanceCount = instanceGroup.NodeCount
	c.InstanceType = template.InstanceType
	c.VolumeType = SafeStringConvert(template.VolumeType)
	c.VolumeSize = SafeInt32Convert(template.VolumeSize)
	c.VolumeCount = template.VolumeCount
	return nil
}

func assembleClusterSkeleton(c *cli.Context) ClusterSkeleton {
	path := c.String(FlCBInputJson.Name)
	if len(path) == 0 {
		log.Error("[AssembleClusterSkeleton] There are missing parameters.\n")
		cli.ShowSubcommandHelp(c)
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

func (c *ClusterSkeleton) fill(stack *models.StackResponse, credential *models.CredentialResponse, blueprint *models.BlueprintResponse,
	templateMap map[string]*models.TemplateResponse, securityMap map[string][]*models.SecurityRule,
	network *models.NetworkJSON, rdsConfig *models.RDSConfigResponse) error {

	if stack == nil {
		return errors.New("Stack definition is not returned from Cloudbreak")
	}
	c.ClusterName = stack.Name
	c.Status = SafeStringConvert(stack.Status)

	parameters := stack.Parameters
	if len(parameters) > 0 && len(parameters["instanceProfileStrategy"]) > 0 {
		if parameters["instanceProfileStrategy"] == "USE_EXISTING" {
			c.InstanceRole = parameters["s3Role"]
		} else {
			c.InstanceRole = parameters["instanceProfileStrategy"]
		}
	}

	if stack.Cluster != nil {
		if c.Status == "AVAILABLE" {
			c.Status = SafeStringConvert(stack.Cluster.Status)
			c.StatusReason = SafeStringConvert(stack.Cluster.StatusReason)
		} else {
			c.StatusReason = SafeStringConvert(stack.StatusReason)
		}
		c.ClusterAndAmbariUser = stack.Cluster.UserName
		c.ClusterAndAmbariPassword = stack.Cluster.Password

	}

	c.HDPVersion = SafeStringConvert(stack.HdpVersion)
	if len(c.HDPVersion) > 3 {
		c.HDPVersion = c.HDPVersion[0:3]
	}

	if blueprint != nil {
		c.ClusterType = getFancyBlueprintName(blueprint)
	}

	if network != nil && network.Parameters["internetGatewayId"] == nil {
		net := Network{VpcId: network.Parameters["vpcId"].(string), SubnetId: network.Parameters["subnetId"].(string)}
		c.Network = &net
	}

	if rdsConfig != nil {
		rdsConfig := HiveMetastore{
			Name: rdsConfig.Name,
		}
		c.HiveMetastore = &rdsConfig
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
			}
		}
	}

	if credential != nil {
		if str, ok := credential.Parameters["existingKeyPairName"].(string); ok {
			c.SSHKeyName = str
		}
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
