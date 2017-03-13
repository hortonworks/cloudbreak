package cli

import (
	"fmt"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/cluster"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/stacks"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"

	"errors"
	"strconv"
)

var BASE_EXPOSED_SERVICES = []string{"AMBARI", "AMBARIUI", "ZEPPELINWS", "ZEPPELINUI"}
var HIVE_EXPOSED_SERVICES = []string{"HIVE"}
var CLUSTER_MANAGER_EXPOSED_SERVICES = []string{"HDFSUI", "YARNUI", "JOBHISTORYUI", "SPARKHISTORYUI"}

func (c *Cloudbreak) GetClusterByName(name string) *models_cloudbreak.StackResponse {
	defer timeTrack(time.Now(), "get cluster by name")

	stack, err := c.Cloudbreak.Stacks.GetStacksUserName(&stacks.GetStacksUserNameParams{Name: name})
	if err != nil {
		logErrorAndExit(err)
	}
	return stack.Payload
}

func (c *Cloudbreak) FetchCluster(stack *models_cloudbreak.StackResponse) (*ClusterSkeletonResult, error) {
	defer timeTrack(time.Now(), "fetch cluster")

	return fetchClusterImpl(stack)
}

func fetchClusterImpl(stack *models_cloudbreak.StackResponse) (*ClusterSkeletonResult, error) {
	var blueprint *models_cloudbreak.BlueprintResponse = nil
	if stack.Cluster != nil {
		blueprint = stack.Cluster.Blueprint
	}

	var templateMap = make(map[string]*models_cloudbreak.TemplateResponse)
	for _, v := range stack.InstanceGroups {
		templateMap[v.Group] = v.Template
	}

	var recipeMap = make(map[string][]*models_cloudbreak.RecipeResponse)
	var recoveryModeMap = make(map[string]string)
	if stack.Cluster != nil {
		for _, hg := range stack.Cluster.HostGroups {
			for _, recipe := range hg.Recipes {
				recipeMap[hg.Name] = append(recipeMap[hg.Name], recipe)
			}
			recoveryModeMap[hg.Name] = *hg.RecoveryMode
		}
	}

	var securityMap = make(map[string][]*models_cloudbreak.SecurityRuleResponse)
	for _, v := range stack.InstanceGroups {
		for _, sr := range v.SecurityGroup.SecurityRules {
			securityMap[sr.Subnet] = append(securityMap[sr.Subnet], sr)
		}
	}

	var credential *models_cloudbreak.CredentialResponse = stack.Credential
	var network *models_cloudbreak.NetworkResponse = stack.Network

	clusterSkeleton := &ClusterSkeletonResult{}
	clusterSkeleton.fill(stack, credential, blueprint, templateMap, securityMap, network, stack.Cluster.RdsConfigs, recipeMap, recoveryModeMap)

	return clusterSkeleton, nil
}

func DescribeCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "describe cluster")

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, []string{FlClusterName.Name})
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	format := c.String(FlOutput.Name)
	clusterSkeleton := describeClusterImpl(clusterName, format, oAuth2Client.GetClusterByName, oAuth2Client.FetchCluster)

	output := Output{Format: format}
	output.Write(ClusterSkeletonHeader, clusterSkeleton)

	return nil
}

func describeClusterImpl(clusterName string, format string,
	getCluster func(string) *models_cloudbreak.StackResponse,
	fetchCluster func(*models_cloudbreak.StackResponse) (*ClusterSkeletonResult, error)) *ClusterSkeletonResult {
	stack := getCluster(clusterName)
	clusterSkeleton, _ := fetchCluster(stack)
	if format == "table" {
		clusterSkeleton.Master.Recipes = []Recipe{}
		clusterSkeleton.Worker.Recipes = []Recipe{}
		clusterSkeleton.Compute.Recipes = []Recipe{}
	}
	clusterSkeleton.Nodes = ""
	return clusterSkeleton
}

func TerminateCluster(c *cli.Context) error {
	clusterName := c.String(FlClusterName.Name)

	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, []string{FlClusterName.Name})
	}

	log.Infof("[TerminateCluster] sending request to terminate cluster: %s", clusterName)
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	deleteDependencies := true
	err := oAuth2Client.Cloudbreak.Stacks.DeleteStacksUserName(&stacks.DeleteStacksUserNameParams{Name: clusterName, DeleteDependencies: &deleteDependencies})

	if err != nil {
		logErrorAndExit(err)
	}

	oAuth2Client.waitForClusterToTerminate(clusterName, c)
	return nil
}

func CreateCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "create cluster")
	skeleton := assembleClusterSkeleton(c)

	if err := skeleton.Validate(); err != nil {
		logErrorAndExit(err)
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	stackId := createClusterImpl(skeleton,
		createMasterTemplateRequest,
		createWorkerTemplateRequest,
		createComputeTemplateRequest,
		createSecurityGroupRequest,
		createCredentialRequest,
		createNetworkRequest,
		createRecipeRequests,
		createBlueprintRequest,
		createRDSRequest,
		oAuth2Client.GetBlueprintByName,
		oAuth2Client.GetCredential,
		oAuth2Client.GetNetwork,
		oAuth2Client.Cloudbreak.Stacks.PostStacksUser,
		oAuth2Client.GetRDSConfigByName,
		oAuth2Client.Cloudbreak.Cluster.PostStacksIDCluster)

	oAuth2Client.waitForClusterToFinish(stackId, c)
	return nil
}

func createClusterImpl(skeleton ClusterSkeleton,
	createMasterTemplateRequest func(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest,
	createWorkerTemplateRequest func(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest,
	createComputeTemplateRequest func(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest,
	createSecurityGroupRequest func(skeleton ClusterSkeleton, group string) *models_cloudbreak.SecurityGroupRequest,
	createCredentialRequest func(name string, defaultCredential models_cloudbreak.CredentialResponse, existingKey string) *models_cloudbreak.CredentialRequest,
	createNetworkRequest func(skeleton ClusterSkeleton, getNetwork func(string) models_cloudbreak.NetworkResponse) *models_cloudbreak.NetworkRequest,
	createRecipeRequests func(recipes []Recipe) []*models_cloudbreak.RecipeRequest,
	createBlueprintRequest func(skeleton ClusterSkeleton, blueprint *models_cloudbreak.BlueprintResponse) *models_cloudbreak.BlueprintRequest,
	createRDSRequest func(metastore MetaStore, rdsType string, hdpVersion string) *models_cloudbreak.RDSConfig,
	getBlueprint func(string) *models_cloudbreak.BlueprintResponse,
	getCredential func(string) models_cloudbreak.CredentialResponse,
	getNetwork func(name string) models_cloudbreak.NetworkResponse,
	postStack func(*stacks.PostStacksUserParams) (*stacks.PostStacksUserOK, error),
	getRdsConfig func(string) models_cloudbreak.RDSConfigResponse,
	postCluster func(*cluster.PostStacksIDClusterParams) (*cluster.PostStacksIDClusterOK, error)) int64 {

	blueprint := getBlueprint(skeleton.ClusterType)

	// create stack

	stackId := func() int64 {
		failureAction := "DO_NOTHING"
		masterType := "GATEWAY"
		workerType := "CORE"
		computeType := "CORE"
		platform := "AWS"

		instanceGroups := []*models_cloudbreak.InstanceGroups{
			{
				Group:         MASTER,
				NodeCount:     1,
				Type:          &masterType,
				Template:      createMasterTemplateRequest(skeleton),
				SecurityGroup: createSecurityGroupRequest(skeleton, MASTER),
			},
			{
				Group:         WORKER,
				NodeCount:     skeleton.Worker.InstanceCount,
				Type:          &workerType,
				Template:      createWorkerTemplateRequest(skeleton),
				SecurityGroup: createSecurityGroupRequest(skeleton, WORKER),
			},
			{
				Group:         COMPUTE,
				NodeCount:     skeleton.Compute.InstanceCount,
				Type:          &computeType,
				Template:      createComputeTemplateRequest(skeleton),
				SecurityGroup: createSecurityGroupRequest(skeleton, COMPUTE),
			},
		}

		var stackParameters = make(map[string]string)
		InstanceProfileDefinition := skeleton.InstanceRole
		if len(InstanceProfileDefinition) > 0 {
			if InstanceProfileDefinition == "CREATE" {
				stackParameters["instanceProfileStrategy"] = "CREATE"
			} else {
				stackParameters["instanceProfileStrategy"] = "USE_EXISTING"
				stackParameters["instanceProfile"] = InstanceProfileDefinition
			}
		}

		orchestrator := models_cloudbreak.OrchestratorRequest{Type: "SALT"}

		log.Infof("[CreateStack] selected HDPVersion %s", skeleton.HDPVersion)

		var ambariVersion = "2.4"
		if skeleton.HDPVersion == "2.6" {
			ambariVersion = "2.5"
		}

		log.Infof("[CreateStack] selected ambariVersion %s", ambariVersion)

		credentialName := "cred" + strconv.FormatInt(time.Now().UnixNano(), 10)
		credReq := createCredentialRequest(credentialName, getCredential("aws-access"), skeleton.SSHKeyName)
		userDefinedTags := make(map[string]interface{})
		if len(skeleton.Tags) > 0 {
			userDefinedTags[USER_TAGS] = skeleton.Tags
		}

		stackReq := models_cloudbreak.StackRequest{
			Name:            skeleton.ClusterName,
			Credential:      credReq,
			FailurePolicy:   &models_cloudbreak.FailurePolicyRequest{AdjustmentType: "BEST_EFFORT"},
			OnFailureAction: &failureAction,
			InstanceGroups:  instanceGroups,
			Parameters:      stackParameters,
			CloudPlatform:   &platform,
			PlatformVariant: &platform,
			Network:         createNetworkRequest(skeleton, getNetwork),
			AmbariVersion:   &ambariVersion,
			HdpVersion:      &skeleton.HDPVersion,
			Orchestrator:    &orchestrator,
			Tags:            userDefinedTags,
		}

		log.Infof("[CreateStack] sending stack create request with name: %s", skeleton.ClusterName)
		resp, err := postStack(&stacks.PostStacksUserParams{Body: &stackReq})

		if err != nil {
			logErrorAndExit(err)
		}

		log.Infof("[CreateStack] stack created, id: %d", resp.Payload.ID)
		return *resp.Payload.ID
	}()

	// create cluster

	func() {
		masterConstraint := models_cloudbreak.Constraint{
			InstanceGroupName: MASTER,
			HostCount:         int32(1),
		}

		workerConstraint := models_cloudbreak.Constraint{
			InstanceGroupName: WORKER,
			HostCount:         int32(skeleton.Worker.InstanceCount),
		}

		computeConstraint := models_cloudbreak.Constraint{
			InstanceGroupName: COMPUTE,
			HostCount:         int32(skeleton.Compute.InstanceCount),
		}

		var workerRecoveryMode string
		if skeleton.Worker.RecoveryMode != "" {
			workerRecoveryMode = skeleton.Worker.RecoveryMode
		} else {
			workerRecoveryMode = "AUTO"
		}
		var computeRecoveryMode string
		if skeleton.Compute.RecoveryMode != "" {
			computeRecoveryMode = skeleton.Compute.RecoveryMode
		} else {
			if skeleton.Compute.SpotPrice != "" {
				computeRecoveryMode = "MANUAL"
			} else {
				computeRecoveryMode = "AUTO"
			}
		}

		hostGroups := []*models_cloudbreak.HostGroupRequest{
			{
				Name:       MASTER,
				Constraint: &masterConstraint,
				Recipes:    createRecipeRequests(skeleton.Master.Recipes),
			},
			{
				Name:         WORKER,
				Constraint:   &workerConstraint,
				Recipes:      createRecipeRequests(skeleton.Worker.Recipes),
				RecoveryMode: &workerRecoveryMode,
			},
			{
				Name:         COMPUTE,
				Constraint:   &computeConstraint,
				Recipes:      createRecipeRequests(skeleton.Compute.Recipes),
				RecoveryMode: &computeRecoveryMode,
			},
		}

		rdsConfigs := make([]*models_cloudbreak.RDSConfig, 0)
		rdsConfigIds := make([]int64, 0)
		// hive metastore
		hiveMetastore := skeleton.HiveMetastore
		if hiveMetastore != nil {
			if len(hiveMetastore.URL) > 0 {
				hiveRds := createRDSRequest(hiveMetastore.MetaStore, HIVE_RDS, skeleton.HDPVersion)
				rdsConfigs = append(rdsConfigs, hiveRds)
			} else if len(hiveMetastore.Name) > 0 {
				rdsConfigIds = append(rdsConfigIds, *getRdsConfig(hiveMetastore.Name).ID)
			}
		}
		// druid metastore
		druidMetastore := skeleton.DruidMetastore
		if druidMetastore != nil {
			if len(druidMetastore.URL) > 0 {
				druidRds := createRDSRequest(druidMetastore.MetaStore, DRUID_RDS, skeleton.HDPVersion)
				rdsConfigs = append(rdsConfigs, druidRds)
			} else if len(druidMetastore.Name) > 0 {
				rdsConfigIds = append(rdsConfigIds, *getRdsConfig(druidMetastore.Name).ID)
			}
		}

		var inputs []*models_cloudbreak.BlueprintInput
		for key, value := range skeleton.ClusterInputs {
			newKey := key
			newValue := value
			inputs = append(inputs, &models_cloudbreak.BlueprintInput{Name: &newKey, PropertyValue: &newValue})
		}

		exposedServices := []string{}

		if skeleton.WebAccess {
			exposedServices = append(exposedServices, BASE_EXPOSED_SERVICES...)
		}

		if skeleton.HiveJDBCAccess {
			exposedServices = append(exposedServices, HIVE_EXPOSED_SERVICES...)
		}

		if skeleton.ClusterComponentAccess {
			exposedServices = append(exposedServices, CLUSTER_MANAGER_EXPOSED_SERVICES...)
		}

		enableKnoxGateway := true
		knoxTopologyName := "hdc"
		clusterReq := models_cloudbreak.ClusterRequest{
			Name:                      skeleton.ClusterName,
			Blueprint:                 createBlueprintRequest(skeleton, blueprint),
			HostGroups:                hostGroups,
			UserName:                  skeleton.ClusterAndAmbariUser,
			Password:                  skeleton.ClusterAndAmbariPassword,
			RdsConfigJsons:            rdsConfigs,
			RdsConfigIds:              rdsConfigIds,
			BlueprintInputs:           inputs,
			BlueprintCustomProperties: skeleton.Configurations,
			Gateway: &models_cloudbreak.GatewayJSON{
				EnableGateway:   &enableKnoxGateway,
				TopologyName:    &knoxTopologyName,
				ExposedServices: exposedServices,
			},
		}

		resp, err := postCluster(&cluster.PostStacksIDClusterParams{ID: stackId, Body: &clusterReq})

		if err != nil {
			logErrorAndExit(err)
		}

		log.Infof("[CreateCluster] cluster created, id: %d", resp.Payload.ID)
	}()

	return stackId
}

func ValidateCreateClusterSkeleton(c *cli.Context) error {
	skeleton := assembleClusterSkeleton(c)
	return skeleton.Validate()
}

func RepairCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "repair cluster")
	checkRequiredFlags(c)

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	clusterName := c.String(FlClusterName.Name)
	nodeType := c.String(FlNodeType.Name)
	removeOnly := c.Bool(FlRemoveOnly.Name)
	if nodeType != WORKER && nodeType != COMPUTE {
		logMissingParameterMessageAndExit(c, fmt.Sprintf("the %s must be one of [worker, compute]\n", FlNodeType.Name))
	}
	if nodeType == COMPUTE && !removeOnly {
		logMissingParameterMessageAndExit(c, fmt.Sprintf("compute nodes cannot be replaced, please use the `--%s true` option", FlRemoveOnly.Name))
	}

	repairClusterImp(clusterName, nodeType, removeOnly, oAuth2Client.GetClusterByName, oAuth2Client.Cloudbreak.Cluster.RepairCluster)
	return nil
}

func repairClusterImp(clusterName string, nodeType string, removeOnly bool,
	getStack func(string) *models_cloudbreak.StackResponse,
	repairCluster func(params *cluster.RepairClusterParams) error) {

	stack := getStack(clusterName)

	repairBody := &models_cloudbreak.ClusterRepairRequest{HostGroups: []string{nodeType}, RemoveOnly: &removeOnly}
	if err := repairCluster(&cluster.RepairClusterParams{Body: repairBody, ID: *stack.ID}); err != nil {
		logErrorAndExit(err)
	}
}

func ResizeCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "resize cluster")
	checkRequiredFlags(c)

	adjustment := int32(c.Int(FlScalingAdjustment.Name))
	if adjustment == 0 {
		logMissingParameterMessageAndExit(c, fmt.Sprintf("the %s cannot be 0\n", FlScalingAdjustment.Name))
	}

	nodeType := c.String(FlNodeType.Name)
	if len(nodeType) == 0 || (nodeType != WORKER && nodeType != COMPUTE) {
		logMissingParameterMessageAndExit(c, fmt.Sprintf("the %s must be one of [worker, compute]\n", FlNodeType.Name))
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	clusterName := c.String(FlClusterName.Name)
	stack := resizeClusterImpl(clusterName, nodeType, adjustment,
		oAuth2Client.GetClusterByName,
		oAuth2Client.Cloudbreak.Stacks.PutStacksID,
		oAuth2Client.Cloudbreak.Cluster.PutStacksIDCluster)

	oAuth2Client.waitForClusterToFinish(*stack.ID, c)

	return nil
}

func resizeClusterImpl(clusterName string, nodeType string, adjustment int32,
	getStack func(string) *models_cloudbreak.StackResponse,
	putStack func(*stacks.PutStacksIDParams) error,
	putCluster func(*cluster.PutStacksIDClusterParams) error) *models_cloudbreak.StackResponse {

	stack := getStack(clusterName)

	if adjustment > 0 {
		withClusterScale := true
		update := &models_cloudbreak.UpdateStack{
			InstanceGroupAdjustment: &models_cloudbreak.InstanceGroupAdjustment{
				InstanceGroup:     nodeType,
				ScalingAdjustment: adjustment,
				WithClusterEvent:  &withClusterScale,
			},
		}
		if err := putStack(&stacks.PutStacksIDParams{ID: *stack.ID, Body: update}); err != nil {
			logErrorAndExit(err)
		}
	} else {
		validateNodeCount := true
		for _, v := range stack.InstanceGroups {
			if nodeType == v.Group {
				if WORKER == nodeType {
					if len(v.Metadata)+int(adjustment) < 3 {
						logErrorAndExit(errors.New("You cannot scale down the worker host group below 3, because it can cause data loss"))
					}
				} else if COMPUTE == nodeType {
					validateNodeCount = false
					if len(v.Metadata)+int(adjustment) < 0 {
						logErrorAndExit(errors.New("You cannot scale the compute nodes below 0"))
					}
				}
			}
		}
		withStackScale := true
		update := &models_cloudbreak.UpdateCluster{
			HostGroupAdjustment: &models_cloudbreak.HostGroupAdjustment{
				ScalingAdjustment: adjustment,
				HostGroup:         nodeType,
				WithStackUpdate:   &withStackScale,
				ValidateNodeCount: &validateNodeCount,
			},
		}
		if err := putCluster(&cluster.PutStacksIDClusterParams{ID: *stack.ID, Body: update}); err != nil {
			logErrorAndExit(err)
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
	checkRequiredFlags(c)

	skeleton := getBaseSkeleton()
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	clusterType := c.String(FlClusterType.Name)
	clusterName := c.String(FlClusterNameOptional.Name)

	generateCreateSharedClusterSkeletonImpl(skeleton, clusterName, clusterType,
		oAuth2Client.GetBlueprintByName,
		oAuth2Client.GetClusterByName,
		oAuth2Client.GetClusterConfig)

	Println(skeleton.JsonPretty())
	return nil
}

func generateCreateSharedClusterSkeletonImpl(skeleton *ClusterSkeleton, clusterName string, clusterType string,
	getBlueprint func(string) *models_cloudbreak.BlueprintResponse,
	getCluster func(string) *models_cloudbreak.StackResponse,
	getClusterConfig func(int64, []*models_cloudbreak.BlueprintParameter) []*models_cloudbreak.BlueprintInput) {

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
			logErrorAndExit(errors.New("the cluster is not 'AVAILABLE' yet, please try again later"))
			return
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

		network := stack.Network
		if network != nil && network.Parameters["internetGatewayId"] == nil {
			skeleton.Network = &Network{VpcId: network.Parameters["vpcId"].(string), SubnetId: network.Parameters["subnetId"].(string)}
		}

		if stack.Cluster != nil && len(stack.Cluster.RdsConfigs) > 0 {
			for _, rds := range stack.Cluster.RdsConfigs {
				if rds.Type != nil {
					if *rds.Type == HIVE_RDS {
						skeleton.HiveMetastore.Name = rds.Name
					} else if *rds.Type == DRUID_RDS {
						skeleton.DruidMetastore.Name = rds.Name
					}
				}
			}
		}

		wg.Wait()
	}
}

func getBaseSkeleton() *ClusterSkeleton {
	return &ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterType: getDefaultClusterType(),
			HDPVersion:  "2.5",
			Master: InstanceConfig{
				InstanceType:  "m4.4xlarge",
				VolumeType:    "gp2",
				VolumeCount:   &(&int32Wrapper{1}).i,
				VolumeSize:    &(&int32Wrapper{32}).i,
				InstanceCount: 1,
				Recipes:       []Recipe{},
			},
			Worker: InstanceConfig{
				InstanceType:  "m3.xlarge",
				VolumeType:    "ephemeral",
				VolumeCount:   &(&int32Wrapper{2}).i,
				VolumeSize:    &(&int32Wrapper{40}).i,
				InstanceCount: 3,
				Recipes:       []Recipe{},
				RecoveryMode:  "AUTO",
			},
			Compute: SpotInstanceConfig{
				InstanceConfig: InstanceConfig{
					InstanceType:  "m3.xlarge",
					VolumeType:    "ephemeral",
					VolumeCount:   &(&int32Wrapper{1}).i,
					VolumeSize:    &(&int32Wrapper{40}).i,
					InstanceCount: 0,
					Recipes:       []Recipe{},
					RecoveryMode:  "AUTO",
				},
				SpotPrice: "0",
			},
			WebAccess:              true,
			HiveJDBCAccess:         true,
			ClusterComponentAccess: false,
			InstanceRole:           "CREATE",
			Network:                &Network{},
			Tags:                   make(map[string]string, 0),
		},
		HiveMetastore:  &HiveMetastore{},
		DruidMetastore: &DruidMetastore{},
		Configurations: []models_cloudbreak.Configurations{},
	}
}
