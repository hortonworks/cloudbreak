package cli

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/cluster"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
	"sync"
	"time"

	"github.com/hortonworks/hdc-cli/client/blueprints"
	"github.com/hortonworks/hdc-cli/client/templates"
	"strconv"
)

func (c *Cloudbreak) GetClusterByName(name string) *models.StackResponse {
	defer timeTrack(time.Now(), "get cluster by name")

	stack, err := c.Cloudbreak.Stacks.GetStacksUserName(&stacks.GetStacksUserNameParams{Name: name})
	if err != nil {
		logErrorAndExit(c.GetClusterByName, err.Error())
	}
	return stack.Payload
}

func (c *Cloudbreak) FetchCluster(stack *models.StackResponse, reduced bool) (*ClusterSkeleton, error) {
	defer timeTrack(time.Now(), "fetch cluster")
	var wg sync.WaitGroup

	wg.Add(1)
	var blueprint *models.BlueprintResponse = nil
	go func() {
		defer wg.Done()
		if stack.Cluster != nil && stack.Cluster.BlueprintID != nil {
			respBlueprint, _ := c.Cloudbreak.Blueprints.GetBlueprintsID(&blueprints.GetBlueprintsIDParams{ID: *stack.Cluster.BlueprintID})
			blueprint = respBlueprint.Payload
		}
	}()

	var templateMap map[string]*models.TemplateResponse = nil
	var securityMap map[string][]*models.SecurityRule = nil
	var credential *models.CredentialResponse = nil
	var network *models.NetworkJSON = nil
	var rdsConfig *models.RDSConfigResponse = nil
	// some operations does not require all info
	if !reduced {
		templateMap = make(map[string]*models.TemplateResponse)
		for i, v := range stack.InstanceGroups {
			wg.Add(1)
			go func(i int, instanceGroup *models.InstanceGroup) {
				defer wg.Done()
				respTemplate, err := c.Cloudbreak.Templates.GetTemplatesID(&templates.GetTemplatesIDParams{ID: instanceGroup.TemplateID})
				if err == nil {
					templateMap[instanceGroup.Group] = respTemplate.Payload
				} else {
					log.Warnf("[FetchCluster] failed to get the instance group info of %s", instanceGroup.Group)
				}
			}(i, v)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			securityMap, _ = c.GetSecurityDetails(stack)
		}()

		wg.Add(1)
		go func() {
			defer wg.Done()
			cred, _ := c.GetCredentialById(stack.CredentialID)
			credential = cred
		}()

		wg.Add(1)
		go func() {
			defer wg.Done()
			network = c.GetNetworkById(stack.NetworkID)
		}()

		if stack.Cluster != nil && stack.Cluster.RdsConfigID != nil {
			wg.Add(1)
			go func() {
				defer wg.Done()
				rdsConfig = c.GetRDSConfigById(*stack.Cluster.RdsConfigID)
			}()
		}
	}

	// synchronize here
	wg.Wait()

	clusterSkeleton := &ClusterSkeleton{}
	clusterSkeleton.fill(stack, credential, blueprint, templateMap, securityMap, network, rdsConfig)

	return clusterSkeleton, nil
}

func DescribeCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "describe cluster")

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, DescribeCluster)
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	stack := oAuth2Client.GetClusterByName(clusterName)
	clusterSkeleton, _ := oAuth2Client.FetchCluster(stack, false)
	clusterSkeleton.ClusterAndAmbariPassword = ""

	output := Output{Format: c.String(FlOutput.Name)}
	output.Write(ClusterSkeletonHeader, clusterSkeleton)

	return nil
}

func TerminateCluster(c *cli.Context) error {
	clusterName := c.String(FlClusterName.Name)

	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, TerminateCluster)
	}

	log.Infof("[TerminateCluster] sending request to terminate cluster: %s", clusterName)
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	err := oAuth2Client.Cloudbreak.Stacks.DeleteStacksUserName(&stacks.DeleteStacksUserNameParams{Name: clusterName})

	if err != nil {
		logErrorAndExit(TerminateCluster, err.Error())
	}

	oAuth2Client.waitForClusterToTerminate(clusterName, c)
	return nil
}

func CreateCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "create cluster")
	skeleton := assembleClusterSkeleton(c)

	if err := skeleton.Validate(); err != nil {
		logErrorAndExit(CreateCluster, err.Error())
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	blueprintId := oAuth2Client.GetBlueprintId(getRealBlueprintName(skeleton.ClusterType))

	var wg sync.WaitGroup
	wg.Add(4)

	credentialId := make(chan int64, 1)
	go oAuth2Client.CopyDefaultCredential(skeleton, credentialId, &wg)

	templateIds := make(chan int64, 2)
	go oAuth2Client.CreateTemplate(skeleton, templateIds, &wg)

	var secGroupId = make(chan int64, 1)
	go oAuth2Client.CreateSecurityGroup(skeleton, secGroupId, &wg)

	networkId := make(chan int64, 1)
	go oAuth2Client.CreateNetwork(skeleton, networkId, &wg)

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
				Group:           MASTER,
				TemplateID:      <-templateIds,
				NodeCount:       1,
				Type:            &masterType,
				SecurityGroupID: secGroupId,
			},
			{
				Group:           WORKER,
				TemplateID:      <-templateIds,
				NodeCount:       skeleton.Worker.InstanceCount,
				Type:            &workerType,
				SecurityGroupID: secGroupId,
			},
		}

		var stackParameters = make(map[string]string)
		s3Role := skeleton.InstanceRole
		if len(s3Role) > 0 {
			if s3Role == "CREATE" {
				stackParameters["instanceProfileStrategy"] = "CREATE"
			} else {
				stackParameters["instanceProfileStrategy"] = "USE_EXISTING"
				stackParameters["s3Role"] = s3Role
			}
		}

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
			logErrorAndExit(CreateCluster, err.Error())
		}

		log.Infof("[CreateStack] stack created, id: %d", resp.Payload.ID)
		return resp.Payload.ID
	}()

	// create cluster

	func() {
		master := MASTER
		worker := WORKER

		masterConstraint := models.Constraint{
			InstanceGroupName: &master,
			HostCount:         int32(1),
		}

		workerConstraint := models.Constraint{
			InstanceGroupName: &worker,
			HostCount:         int32(skeleton.Worker.InstanceCount),
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

		var rdsConfig *models.RDSConfig = nil
		var rdsId *int64 = nil
		ms := skeleton.HiveMetastore
		if skeleton.HiveMetastore != nil {
			if len(skeleton.HiveMetastore.URL) > 0 {
				var connUrl string
				if ms.DatabaseType == POSTGRES {
					connUrl = "jdbc:postgresql://" + ms.URL
				} else {
					connUrl = "jdbc:mysql://" + ms.URL
				}
				rdsConfig = &models.RDSConfig{
					Name:               ms.Name,
					ConnectionUserName: ms.Username,
					ConnectionPassword: ms.Password,
					ConnectionURL:      connUrl,
					DatabaseType:       ms.DatabaseType,
					HdpVersion:         skeleton.HDPVersion,
				}
			} else if len(ms.Name) > 0 {
				id, err := strconv.ParseInt(*oAuth2Client.GetRDSConfigByName(ms.Name).ID, 10, 64)
				if err != nil {
					logErrorAndExit(CreateCluster, err.Error())
				}
				rdsId = &id
			}
		}

		clusterReq := models.ClusterRequest{
			Name:          skeleton.ClusterName,
			BlueprintID:   blueprintId,
			HostGroups:    hostGroups,
			UserName:      skeleton.ClusterAndAmbariUser,
			Password:      skeleton.ClusterAndAmbariPassword,
			RdsConfigJSON: rdsConfig,
			RdsConfigID:   rdsId,
		}

		resp, err := oAuth2Client.Cloudbreak.Cluster.PostStacksIDCluster(&cluster.PostStacksIDClusterParams{ID: stackId, Body: &clusterReq})

		if err != nil {
			logErrorAndExit(CreateCluster, err.Error())
		}

		log.Infof("[CreateCluster] cluster created, id: %d", resp.Payload.ID)
	}()

	oAuth2Client.waitForClusterToFinish(stackId, c)
	return nil
}

func ValidateCreateClusterSkeleton(c *cli.Context) error {
	skeleton := assembleClusterSkeleton(c)
	return skeleton.Validate()
}

func ResizeCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "resize cluster")

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, ResizeCluster)
	}

	adjustment := int32(c.Int(FlScalingAdjustment.Name))
	if adjustment == 0 {
		logMissingParameterAndExit(c, ResizeCluster, fmt.Sprintf("the %s cannot be 0\n", FlScalingAdjustment.Name))
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	stack := oAuth2Client.GetClusterByName(clusterName)

	if adjustment > 0 {
		withClusterScale := true
		update := &models.UpdateStack{
			InstanceGroupAdjustment: &models.InstanceGroupAdjustment{
				InstanceGroup:     WORKER,
				ScalingAdjustment: adjustment,
				WithClusterEvent:  &withClusterScale,
			},
			Status: nil,
		}
		if err := oAuth2Client.Cloudbreak.Stacks.PutStacksID(&stacks.PutStacksIDParams{ID: *stack.ID, Body: update}); err != nil {
			logErrorAndExit(ResizeCluster, err.Error())
		}
	} else {
		withStackScale := true
		update := &models.UpdateCluster{
			HostGroupAdjustment: &models.HostGroupAdjustment{
				ScalingAdjustment: adjustment,
				HostGroup:         WORKER,
				WithStackUpdate:   &withStackScale,
			},
		}
		if err := oAuth2Client.Cloudbreak.Cluster.PutStacksIDCluster(&cluster.PutStacksIDClusterParams{ID: *stack.ID, Body: update}); err != nil {
			logErrorAndExit(ResizeCluster, err.Error())
		}
	}

	return nil
}

func GenerateCreateClusterSkeleton(c *cli.Context) error {
	skeleton := ClusterSkeleton{
		ClusterType: "EDW-ETL: Apache Spark 2.0-preview, Apache Hive 2.0",
		HDPVersion:  "2.5",
		Master: InstanceConfig{
			InstanceType: "m4.xlarge",
			VolumeType:   "gp2",
			VolumeCount:  1,
			VolumeSize:   32,
		},
		Worker: InstanceConfig{
			InstanceType:  "m3.xlarge",
			VolumeType:    "ephemeral",
			VolumeCount:   2,
			VolumeSize:    40,
			InstanceCount: 2,
		},
		WebAccess:     true,
		InstanceRole:  "CREATE",
		Network:       &Network{},
		HiveMetastore: &HiveMetastore{},
	}
	fmt.Println(skeleton.JsonPretty())
	return nil
}
