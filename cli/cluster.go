package cli

import (
	"fmt"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/cluster"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"

	"strconv"

	"github.com/hortonworks/hdc-cli/client/blueprints"
	"github.com/hortonworks/hdc-cli/client/templates"
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

	return fetchClusterImpl(stack, reduced, c.Cloudbreak.Blueprints.GetBlueprintsID, c.Cloudbreak.Templates.GetTemplatesID, c.GetSecurityDetails, c.GetCredentialById, c.GetNetworkById, c.GetRDSConfigById)
}

func fetchClusterImpl(stack *models.StackResponse, reduced bool, getBlueprint func(*blueprints.GetBlueprintsIDParams) (*blueprints.GetBlueprintsIDOK, error),
	getTemplate func(*templates.GetTemplatesIDParams) (*templates.GetTemplatesIDOK, error),
	getSecurityDetails func(*models.StackResponse) (securityMap map[string][]*models.SecurityRule, err error),
	getCredential func(int64) (*models.CredentialResponse, error), getNetwork func(int64) *models.NetworkJSON, getRdsConfig func(int64) *models.RDSConfigResponse) (*ClusterSkeleton, error) {

	var wg sync.WaitGroup

	wg.Add(1)
	var blueprint *models.BlueprintResponse = nil
	go func() {
		defer wg.Done()
		if stack.Cluster != nil && stack.Cluster.BlueprintID != nil {
			respBlueprint, _ := getBlueprint(&blueprints.GetBlueprintsIDParams{ID: *stack.Cluster.BlueprintID})
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
				respTemplate, err := getTemplate(&templates.GetTemplatesIDParams{ID: instanceGroup.TemplateID})
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
			securityMap, _ = getSecurityDetails(stack)
		}()

		wg.Add(1)
		go func() {
			defer wg.Done()
			cred, _ := getCredential(stack.CredentialID)
			credential = cred
		}()

		wg.Add(1)
		go func() {
			defer wg.Done()
			network = getNetwork(stack.NetworkID)
		}()

		if stack.Cluster != nil && stack.Cluster.RdsConfigID != nil {
			wg.Add(1)
			go func() {
				defer wg.Done()
				rdsConfig = getRdsConfig(*stack.Cluster.RdsConfigID)
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

	clusterSkeleton := describeClusterImpl(clusterName, oAuth2Client.GetClusterByName, oAuth2Client.FetchCluster)

	output := Output{Format: c.String(FlOutput.Name)}
	output.Write(ClusterSkeletonHeader, clusterSkeleton)

	return nil
}

func describeClusterImpl(clusterName string, getCluster func(string) *models.StackResponse, fetchCluster func(*models.StackResponse, bool) (*ClusterSkeleton, error)) *ClusterSkeleton {
	stack := getCluster(clusterName)
	clusterSkeleton, _ := fetchCluster(stack, false)
	clusterSkeleton.ClusterAndAmbariPassword = ""
	return clusterSkeleton
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

	stackId := createClusterImpl(skeleton, oAuth2Client.GetBlueprintByName, oAuth2Client.CopyDefaultCredential, oAuth2Client.CreateTemplate, oAuth2Client.CreateSecurityGroup, oAuth2Client.CreateNetwork,
		oAuth2Client.CreateBlueprint, oAuth2Client.Cloudbreak.Stacks.PostStacksUser, oAuth2Client.GetRDSConfigByName, oAuth2Client.Cloudbreak.Cluster.PostStacksIDCluster)

	oAuth2Client.waitForClusterToFinish(stackId, c)
	return nil
}

func createClusterImpl(skeleton ClusterSkeleton, getBlueprint func(string) *models.BlueprintResponse, copyCredential func(ClusterSkeleton, chan int64, *sync.WaitGroup),
	createTemplate func(ClusterSkeleton, chan int64, *sync.WaitGroup), createSecurityGroup func(ClusterSkeleton, chan int64, *sync.WaitGroup), createNetwork func(ClusterSkeleton, chan int64, *sync.WaitGroup),
	createBlueprint func(ClusterSkeleton, *models.BlueprintResponse, chan int64, *sync.WaitGroup), postStack func(*stacks.PostStacksUserParams) (*stacks.PostStacksUserOK, error),
	getRdsConfig func(string) models.RDSConfigResponse, postCluster func(*cluster.PostStacksIDClusterParams) (*cluster.PostStacksIDClusterOK, error)) int64 {

	blueprint := getBlueprint(skeleton.ClusterType)

	var wg sync.WaitGroup
	wg.Add(5)

	credentialId := make(chan int64, 1)
	go copyCredential(skeleton, credentialId, &wg)

	templateIds := make(chan int64, 2)
	go createTemplate(skeleton, templateIds, &wg)

	var secGroupId = make(chan int64, 1)
	go createSecurityGroup(skeleton, secGroupId, &wg)

	networkId := make(chan int64, 1)
	go createNetwork(skeleton, networkId, &wg)

	blueprintId := make(chan int64, 1)
	go createBlueprint(skeleton, blueprint, blueprintId, &wg)

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
		resp, err := postStack(&stacks.PostStacksUserParams{Body: &stackReq})

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
		validate := false
		if skeleton.HiveMetastore != nil {
			if len(skeleton.HiveMetastore.URL) > 0 {
				rdsConfig = &models.RDSConfig{
					Name:               ms.Name,
					ConnectionUserName: ms.Username,
					ConnectionPassword: ms.Password,
					ConnectionURL:      extendRdsUrl(ms.URL),
					DatabaseType:       ms.DatabaseType,
					HdpVersion:         skeleton.HDPVersion,
					Validated:          &validate,
				}
			} else if len(ms.Name) > 0 {
				id, err := strconv.ParseInt(*getRdsConfig(ms.Name).ID, 10, 64)
				if err != nil {
					logErrorAndExit(CreateCluster, err.Error())
				}
				rdsId = &id
			}
		}

		var inputs []*models.BlueprintInputJSON
		for key, value := range skeleton.ClusterInputs {
			newKey := key
			newValue := value
			inputs = append(inputs, &models.BlueprintInputJSON{Name: &newKey, PropertyValue: &newValue})
		}

		clusterReq := models.ClusterRequest{
			Name:            skeleton.ClusterName,
			BlueprintID:     <-blueprintId,
			HostGroups:      hostGroups,
			UserName:        skeleton.ClusterAndAmbariUser,
			Password:        skeleton.ClusterAndAmbariPassword,
			RdsConfigJSON:   rdsConfig,
			RdsConfigID:     rdsId,
			BlueprintInputs: inputs,
		}

		resp, err := postCluster(&cluster.PostStacksIDClusterParams{ID: stackId, Body: &clusterReq})

		if err != nil {
			logErrorAndExit(CreateCluster, err.Error())
		}

		log.Infof("[CreateCluster] cluster created, id: %d", resp.Payload.ID)
	}()

	return stackId
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

	stack := resizeClusterImpl(clusterName, adjustment, oAuth2Client.GetClusterByName, oAuth2Client.Cloudbreak.Stacks.PutStacksID, oAuth2Client.Cloudbreak.Cluster.PutStacksIDCluster)

	oAuth2Client.waitForClusterToFinish(*stack.ID, c)

	return nil
}

func resizeClusterImpl(clusterName string, adjustment int32, getStack func(string) *models.StackResponse, putStack func(*stacks.PutStacksIDParams) error,
	putCluster func(*cluster.PutStacksIDClusterParams) error) *models.StackResponse {

	stack := getStack(clusterName)

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
		if err := putStack(&stacks.PutStacksIDParams{ID: *stack.ID, Body: update}); err != nil {
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
		if err := putCluster(&cluster.PutStacksIDClusterParams{ID: *stack.ID, Body: update}); err != nil {
			logErrorAndExit(ResizeCluster, err.Error())
		}
	}

	return stack
}

func GenerateCreateClusterSkeleton(c *cli.Context) error {
	Println(getBaseSkeleton().JsonPretty())
	return nil
}

func GenerateCreateSharedClusterSkeleton(c *cli.Context) error {
	defer timeTrack(time.Now(), "generate shared cluster skeleton")
	checkRequiredFlags(c, GenerateCreateSharedClusterSkeleton)

	skeleton := getBaseSkeleton()
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	clusterType := c.String(FlClusterType.Name)
	clusterName := c.String(FlClusterNameOptional.Name)

	generateCreateSharedClusterSkeletonImpl(skeleton, clusterType, clusterName, oAuth2Client.GetBlueprintByName, oAuth2Client.GetClusterByName, oAuth2Client.GetClusterConfig, oAuth2Client.GetNetworkById,
		oAuth2Client.GetRDSConfigById)

	Println(skeleton.JsonPretty())
	return nil
}

func generateCreateSharedClusterSkeletonImpl(skeleton *ClusterSkeleton, clusterName string, clusterType string, getBlueprint func(string) *models.BlueprintResponse,
	getCluster func(string) *models.StackResponse, getClusterConfig func(int64, []*models.BlueprintParameterJSON) []*models.BlueprintInputJSON, getNetwork func(int64) *models.NetworkJSON,
	getRdsConfig func(int64) *models.RDSConfigResponse) {

	ambariBp := getBlueprint(clusterType)

	skeleton.ClusterType = clusterType
	skeleton.HDPVersion = ambariBp.Blueprint.StackVersion
	var inputs = make(map[string]string)
	for _, input := range ambariBp.Inputs {
		inputs[*input.Name] = ""
	}
	skeleton.ClusterInputs = inputs
	log.Infof("[GenerateCreateSharedClusterSkeleton] inputs for cluster type: %+v", inputs)

	if len(clusterName) > 0 {
		stack := getCluster(clusterName)
		if *stack.Status != "AVAILABLE" && *stack.Cluster.Status != "AVAILABLE" {
			logErrorAndExit(GenerateCreateSharedClusterSkeleton, "the cluster is not 'AVAILABLE' yet, please try again later")
		}

		var wg sync.WaitGroup

		wg.Add(1)
		go func() {
			defer wg.Done()
			configs := getClusterConfig(*stack.ID, ambariBp.Inputs)
			for _, input := range configs {
				inputs[*input.Name] = *input.PropertyValue
			}
		}()

		wg.Add(1)
		go func() {
			defer wg.Done()
			network := getNetwork(stack.NetworkID)
			if network.Parameters["internetGatewayId"] == nil {
				skeleton.Network = &Network{VpcId: network.Parameters["vpcId"].(string), SubnetId: network.Parameters["subnetId"].(string)}
			}
		}()

		if stack.Cluster != nil && stack.Cluster.RdsConfigID != nil {
			wg.Add(1)
			go func() {
				defer wg.Done()
				rdsConfig := getRdsConfig(*stack.Cluster.RdsConfigID)
				skeleton.HiveMetastore.Name = rdsConfig.Name
			}()
		}

		wg.Wait()
	}
}

func getBaseSkeleton() *ClusterSkeleton {
	return &ClusterSkeleton{
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
		WebAccess:      true,
		InstanceRole:   "CREATE",
		Network:        &Network{},
		HiveMetastore:  &HiveMetastore{},
		Configurations: []models.Configurations{},
	}
}
