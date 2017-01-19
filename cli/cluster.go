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
)

func (c *Cloudbreak) GetClusterByName(name string) *models.StackResponse {
	defer timeTrack(time.Now(), "get cluster by name")

	stack, err := c.Cloudbreak.Stacks.GetStacksUserName(&stacks.GetStacksUserNameParams{Name: name})
	if err != nil {
		logErrorAndExit(c.GetClusterByName, err.Error())
	}
	return stack.Payload
}

func (c *Cloudbreak) FetchCluster(stack *models.StackResponse) (*ClusterSkeletonResult, error) {
	defer timeTrack(time.Now(), "fetch cluster")

	return fetchClusterImpl(stack)
}

func fetchClusterImpl(stack *models.StackResponse) (*ClusterSkeletonResult, error) {
	var blueprint *models.BlueprintResponse = nil
	if stack.Cluster != nil {
		blueprint = stack.Cluster.Blueprint
	}

	var templateMap = make(map[string]*models.TemplateResponse)
	for _, v := range stack.InstanceGroups {
		templateMap[v.Group] = v.Template
	}

	var recipeMap = make(map[string][]*models.RecipeResponse)
	if stack.Cluster != nil {
		for _, hg := range stack.Cluster.HostGroups {
			for _, recipe := range hg.Recipes {
				recipeMap[hg.Name] = append(recipeMap[hg.Name], recipe)
			}
		}
	}

	var securityMap = make(map[string][]*models.SecurityRuleResponse)
	for _, v := range stack.InstanceGroups {
		for _, sr := range v.SecurityGroup.SecurityRules {
			securityMap[sr.Subnet] = append(securityMap[sr.Subnet], sr)
		}
	}

	var credential *models.CredentialResponse = stack.Credential
	var network *models.NetworkResponse = stack.Network

	var rdsConfig *models.RDSConfigResponse = nil
	if stack.Cluster != nil {
		rdsConfig = stack.Cluster.RdsConfig
	}

	clusterSkeleton := &ClusterSkeletonResult{}
	clusterSkeleton.fill(stack, credential, blueprint, templateMap, securityMap, network, rdsConfig, recipeMap)

	return clusterSkeleton, nil
}

func DescribeCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "describe cluster")

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, DescribeCluster)
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	format := c.String(FlOutput.Name)
	clusterSkeleton := describeClusterImpl(clusterName, format, oAuth2Client.GetClusterByName, oAuth2Client.FetchCluster)

	output := Output{Format: format}
	output.Write(ClusterSkeletonHeader, clusterSkeleton)

	return nil
}

func describeClusterImpl(clusterName string, format string,
	getCluster func(string) *models.StackResponse,
	fetchCluster func(*models.StackResponse) (*ClusterSkeletonResult, error)) *ClusterSkeletonResult {
	stack := getCluster(clusterName)
	clusterSkeleton, _ := fetchCluster(stack)
	if format == "table" {
		clusterSkeleton.Master.Recipes = []Recipe{}
		clusterSkeleton.Worker.Recipes = []Recipe{}
		clusterSkeleton.Compute.Recipes = []Recipe{}
	}
	return clusterSkeleton
}

func TerminateCluster(c *cli.Context) error {
	clusterName := c.String(FlClusterName.Name)

	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, TerminateCluster)
	}

	log.Infof("[TerminateCluster] sending request to terminate cluster: %s", clusterName)
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	deleteDependencies := true
	err := oAuth2Client.Cloudbreak.Stacks.DeleteStacksUserName(&stacks.DeleteStacksUserNameParams{Name: clusterName, DeleteDependencies: &deleteDependencies})

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

	stackId := createClusterImpl(skeleton,
		createMasterTemplateRequest,
		createWorkerTemplateRequest,
		createComputeTemplateRequest,
		createSecurityGroupRequest,
		createCredentialRequest,
		createNetworkRequest,
		createRecipeRequests,
		createBlueprintRequest,
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
	createMasterTemplateRequest func(skeleton ClusterSkeleton) *models.TemplateRequest,
	createWorkerTemplateRequest func(skeleton ClusterSkeleton) *models.TemplateRequest,
	createComputeTemplateRequest func(skeleton ClusterSkeleton) *models.TemplateRequest,
	createSecurityGroupRequest func(skeleton ClusterSkeleton) *models.SecurityGroupRequest,
	createCredentialRequest func(name string, defaultCredential models.CredentialResponse, existingKey string) *models.CredentialRequest,
	createNetworkRequest func(skeleton ClusterSkeleton, getNetwork func(string) models.NetworkResponse) *models.NetworkRequest,
	createRecipeRequests func(recipes []Recipe) []*models.RecipeRequest,
	createBlueprintRequest func(skeleton ClusterSkeleton, blueprint *models.BlueprintResponse) *models.BlueprintRequest,
	getBlueprint func(string) *models.BlueprintResponse,
	getCredential func(string) models.CredentialResponse,
	getNetwork func(name string) models.NetworkResponse,
	postStack func(*stacks.PostStacksUserParams) (*stacks.PostStacksUserOK, error),
	getRdsConfig func(string) models.RDSConfigResponse,
	postCluster func(*cluster.PostStacksIDClusterParams) (*cluster.PostStacksIDClusterOK, error)) int64 {

	blueprint := getBlueprint(skeleton.ClusterType)

	// create stack

	stackId := func() int64 {
		failureAction := "DO_NOTHING"
		masterType := "GATEWAY"
		workerType := "CORE"
		computeType := "CORE"
		platform := "AWS"

		instanceGroups := []*models.InstanceGroups{
			{
				Group:         MASTER,
				NodeCount:     1,
				Type:          &masterType,
				Template:      createMasterTemplateRequest(skeleton),
				SecurityGroup: createSecurityGroupRequest(skeleton),
			},
			{
				Group:         WORKER,
				NodeCount:     skeleton.Worker.InstanceCount,
				Type:          &workerType,
				Template:      createWorkerTemplateRequest(skeleton),
				SecurityGroup: createSecurityGroupRequest(skeleton),
			},
			{
				Group:         COMPUTE,
				NodeCount:     skeleton.Compute.InstanceCount,
				Type:          &computeType,
				Template:      createComputeTemplateRequest(skeleton),
				SecurityGroup: createSecurityGroupRequest(skeleton),
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

		orchestrator := models.OrchestratorRequest{Type: "SALT"}

		log.Infof("[CreateStack] selected HDPVersion %s", skeleton.HDPVersion)

		var ambariVersion = "2.4"
		if skeleton.HDPVersion == "2.6" {
			ambariVersion = "2.5"
		}

		log.Infof("[CreateStack] selected ambariVersion %s", ambariVersion)

		credentialName := "cred" + strconv.FormatInt(time.Now().UnixNano(), 10)
		credReq := createCredentialRequest(credentialName, getCredential("aws-access"), skeleton.SSHKeyName)

		stackReq := models.StackRequest{
			Name:            skeleton.ClusterName,
			Credential:      credReq,
			FailurePolicy:   &models.FailurePolicyRequest{AdjustmentType: "BEST_EFFORT"},
			OnFailureAction: &failureAction,
			InstanceGroups:  instanceGroups,
			Parameters:      stackParameters,
			CloudPlatform:   &platform,
			PlatformVariant: &platform,
			Network:         createNetworkRequest(skeleton, getNetwork),
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
		return *resp.Payload.ID
	}()

	// create cluster

	func() {
		masterConstraint := models.Constraint{
			InstanceGroupName: MASTER,
			HostCount:         int32(1),
		}

		workerConstraint := models.Constraint{
			InstanceGroupName: WORKER,
			HostCount:         int32(skeleton.Worker.InstanceCount),
		}

		computeConstraint := models.Constraint{
			InstanceGroupName: COMPUTE,
			HostCount:         int32(skeleton.Compute.InstanceCount),
		}

		hostGroups := []*models.HostGroupRequest{
			{
				Name:       MASTER,
				Constraint: &masterConstraint,
				Recipes:    createRecipeRequests(skeleton.Master.Recipes),
			},
			{
				Name:       WORKER,
				Constraint: &workerConstraint,
				Recipes:    createRecipeRequests(skeleton.Worker.Recipes),
			},
			{
				Name:       COMPUTE,
				Constraint: &computeConstraint,
				Recipes:    createRecipeRequests(skeleton.Compute.Recipes),
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
				rdsId = getRdsConfig(ms.Name).ID
			}
		}

		var inputs []*models.BlueprintInput
		for key, value := range skeleton.ClusterInputs {
			newKey := key
			newValue := value
			inputs = append(inputs, &models.BlueprintInput{Name: &newKey, PropertyValue: &newValue})
		}

		clusterReq := models.ClusterRequest{
			Name:                      skeleton.ClusterName,
			Blueprint:                 createBlueprintRequest(skeleton, blueprint),
			HostGroups:                hostGroups,
			UserName:                  skeleton.ClusterAndAmbariUser,
			Password:                  skeleton.ClusterAndAmbariPassword,
			RdsConfigJSON:             rdsConfig,
			RdsConfigID:               rdsId,
			BlueprintInputs:           inputs,
			BlueprintCustomProperties: skeleton.Configurations,
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

	nodeType := c.String(FlNodeType.Name)
	if len(nodeType) == 0 || (nodeType != WORKER && nodeType != COMPUTE) {
		logMissingParameterAndExit(c, ResizeCluster, fmt.Sprintf("the %s must be one of [worker, compute]\n", FlNodeType.Name))
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	stack := resizeClusterImpl(clusterName, nodeType, adjustment, oAuth2Client.GetClusterByName, oAuth2Client.Cloudbreak.Stacks.PutStacksID, oAuth2Client.Cloudbreak.Cluster.PutStacksIDCluster)

	oAuth2Client.waitForClusterToFinish(*stack.ID, c)

	return nil
}

func resizeClusterImpl(clusterName string, nodeType string, adjustment int32, getStack func(string) *models.StackResponse, putStack func(*stacks.PutStacksIDParams) error,
	putCluster func(*cluster.PutStacksIDClusterParams) error) *models.StackResponse {

	stack := getStack(clusterName)

	if adjustment > 0 {
		withClusterScale := true
		update := &models.UpdateStack{
			InstanceGroupAdjustment: &models.InstanceGroupAdjustment{
				InstanceGroup:     nodeType,
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
				HostGroup:         nodeType,
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

	generateCreateSharedClusterSkeletonImpl(skeleton, clusterName, clusterType,
		oAuth2Client.GetBlueprintByName,
		oAuth2Client.GetClusterByName,
		oAuth2Client.GetClusterConfig,
		oAuth2Client.GetNetworkById,
		oAuth2Client.GetRDSConfigById)

	Println(skeleton.JsonPretty())
	return nil
}

func generateCreateSharedClusterSkeletonImpl(skeleton *ClusterSkeleton, clusterName string, clusterType string,
	getBlueprint func(string) *models.BlueprintResponse,
	getCluster func(string) *models.StackResponse,
	getClusterConfig func(int64, []*models.BlueprintParameter) []*models.BlueprintInput,
	getNetwork func(int64) *models.NetworkResponse,
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

		wg.Add(1)
		go func() {
			defer wg.Done()
			network := getNetwork(*stack.NetworkID)
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
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterType: getDefaultClusterType(),
			HDPVersion:  "2.5",
			Master: InstanceConfig{
				InstanceType: "m4.4xlarge",
				VolumeType:   "gp2",
				VolumeCount:  &(&int32Wrapper{1}).i,
				VolumeSize:   &(&int32Wrapper{32}).i,
				Recipes:      []Recipe{},
			},
			Worker: InstanceConfig{
				InstanceType:  "m3.xlarge",
				VolumeType:    "ephemeral",
				VolumeCount:   &(&int32Wrapper{2}).i,
				VolumeSize:    &(&int32Wrapper{40}).i,
				InstanceCount: 2,
				Recipes:       []Recipe{},
			},
			Compute: SpotInstanceConfig{
				InstanceConfig: InstanceConfig{
					InstanceType:  "m3.xlarge",
					VolumeType:    "ephemeral",
					VolumeCount:   &(&int32Wrapper{1}).i,
					VolumeSize:    &(&int32Wrapper{40}).i,
					InstanceCount: 0,
					Recipes:       []Recipe{},
				},
				SpotPrice: "0",
			},
			WebAccess:    true,
			InstanceRole: "CREATE",
			Network:      &Network{},
		},
		HiveMetastore:  &HiveMetastore{},
		Configurations: []models.Configurations{},
	}
}
