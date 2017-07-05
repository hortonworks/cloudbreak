package cli

import (
	"fmt"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/cluster"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/stacks"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"

	"encoding/json"
	"errors"
	"strings"
)

var BASE_EXPOSED_SERVICES = []string{"AMBARI", "AMBARIUI", "ZEPPELINWS", "ZEPPELINUI"}
var HIVE_EXPOSED_SERVICES = []string{"HIVE"}
var RANGER_EXPOSED_SERVICES = []string{"RANGER", "RANGERUI"}
var CLUSTER_MANAGER_EXPOSED_SERVICES = []string{"HDFSUI", "YARNUI", "JOBHISTORYUI", "SPARKHISTORYUI"}

func (c *Cloudbreak) GetClusterByName(name string) *models_cloudbreak.StackResponse {
	defer timeTrack(time.Now(), "get cluster by name")

	stack, err := c.Cloudbreak.Stacks.GetPrivateStack(&stacks.GetPrivateStackParams{Name: name})
	if err != nil {
		logErrorAndExit(err)
	}
	return stack.Payload
}

func (c *Cloudbreak) FetchCluster(stack *models_cloudbreak.StackResponse, autoscaling *AutoscalingSkeletonResult) (*ClusterSkeletonResult, error) {
	defer timeTrack(time.Now(), "fetch cluster")

	return fetchClusterImpl(stack, autoscaling)
}

func fetchClusterImpl(stack *models_cloudbreak.StackResponse, autoscaling *AutoscalingSkeletonResult) (*ClusterSkeletonResult, error) {
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
	clusterSkeleton.fill(stack, credential, blueprint, templateMap, securityMap, network, stack.Cluster.RdsConfigs, recipeMap, recoveryModeMap, autoscaling)

	return clusterSkeleton, nil
}

func DescribeCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "describe cluster")

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, []string{FlClusterName.Name})
	}

	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	format := c.String(FlOutput.Name)
	clusterSkeleton := describeClusterImpl(clusterName, format, cbClient.GetClusterByName, cbClient.FetchCluster, asClient.getAutoscaling)

	output := Output{Format: format}
	output.Write(ClusterSkeletonHeader, clusterSkeleton)

	return nil
}

func describeClusterImpl(clusterName string, format string,
	getCluster func(string) *models_cloudbreak.StackResponse,
	fetchCluster func(*models_cloudbreak.StackResponse, *AutoscalingSkeletonResult) (*ClusterSkeletonResult, error),
	getAutoscaling func(string, int64) *AutoscalingSkeletonResult) *ClusterSkeletonResult {
	stack := getCluster(clusterName)
	autoscalingSkeleton := getAutoscaling(clusterName, *stack.ID)
	clusterSkeleton, _ := fetchCluster(stack, autoscalingSkeleton)
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
	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	asClient.deleteCluster(clusterName, cbClient.GetClusterByName)

	deleteDependencies := true
	err := cbClient.Cloudbreak.Stacks.DeletePrivateStack(&stacks.DeletePrivateStackParams{Name: clusterName, DeleteDependencies: &deleteDependencies})

	if err != nil {
		logErrorAndExit(err)
	}

	cbClient.waitForClusterToTerminate(clusterName, c)
	return nil
}

func CreateCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "create cluster")
	skeleton := assembleClusterSkeleton(c)

	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	if err := skeleton.Validate(cbClient); err != nil {
		logErrorAndExit(err)
	}

	stackId := createClusterImpl(skeleton,
		createMasterTemplateRequest,
		createWorkerTemplateRequest,
		createComputeTemplateRequest,
		createSecurityGroupRequest,
		createNetworkRequest,
		createRecipeRequests,
		createBlueprintRequest,
		createRDSRequest,
		cbClient.GetBlueprintByName,
		cbClient.GetNetwork,
		cbClient.Cloudbreak.Stacks.PostPrivateStack,
		cbClient.GetRDSConfigByName,
		cbClient.Cloudbreak.Cluster.PostCluster,
		asClient.createBaseAutoscalingCluster,
		asClient.setConfiguration,
		asClient.addPrometheusAlert,
		asClient.addScalingPolicy,
		cbClient.GetLdapByName,
		cbClient.GetClusterByName,
		cbClient.GetClusterConfig,
		cbClient.GetFlexSubscriptionByName)

	cbClient.waitForClusterToFinish(stackId, c)
	return nil
}

func createClusterImpl(skeleton ClusterSkeleton,
	createMasterTemplateRequest func(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest,
	createWorkerTemplateRequest func(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest,
	createComputeTemplateRequest func(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest,
	createSecurityGroupRequest func(skeleton ClusterSkeleton, group string) *models_cloudbreak.SecurityGroupRequest,
	createNetworkRequest func(skeleton ClusterSkeleton, getNetwork func(string) models_cloudbreak.NetworkResponse) *models_cloudbreak.NetworkRequest,
	createRecipeRequests func(recipes []Recipe) []*models_cloudbreak.RecipeRequest,
	createBlueprintRequest func(skeleton ClusterSkeleton, blueprint *models_cloudbreak.BlueprintResponse) *models_cloudbreak.BlueprintRequest,
	createRDSRequest func(metastore MetaStore, rdsType string, hdpVersion string, properties []*models_cloudbreak.RdsConfigProperty) *models_cloudbreak.RDSConfig,
	getBlueprint func(string) *models_cloudbreak.BlueprintResponse,
	getNetwork func(name string) models_cloudbreak.NetworkResponse,
	postStack func(*stacks.PostPrivateStackParams) (*stacks.PostPrivateStackOK, error),
	getRdsConfig func(string) models_cloudbreak.RDSConfigResponse,
	postCluster func(*cluster.PostClusterParams) (*cluster.PostClusterOK, error),
	createBaseAutoscalingCluster func(stackId int64) int64,
	setScalingConfigurations func(int64, AutoscalingConfiguration),
	addPrometheusAlert func(int64, AutoscalingPolicy) int64,
	addScalingPolicy func(int64, AutoscalingPolicy, int64) int64,
	getLdapConfig func(string) *models_cloudbreak.LdapConfigResponse,
	getCluster func(name string) *models_cloudbreak.StackResponse,
	getClusterConfig func(int64, []*models_cloudbreak.BlueprintParameter) []*models_cloudbreak.BlueprintInput,
	getFlexSubscriptionByName func(name string) *models_cloudbreak.FlexSubscriptionResponse) int64 {

	blueprint := getBlueprint(skeleton.ClusterType)
	dataLake := false
	var connectedClusterRequest *models_cloudbreak.ConnectedClusterRequest = nil
	bpName := blueprint.BlueprintName
	if bpName != nil && (*bpName == "hdp26-shared-services" || *bpName == "hdp26-shared-services-ha") {
		dataLake = true
		convertClusterInputs(&skeleton)
	} else if len(skeleton.SharedClusterName) > 0 {
		log.Infof("[CreateStack] ephemeral cluster name: %s", skeleton.ClusterName)
		fillSharedParameters(&skeleton, getBlueprint, getCluster, getClusterConfig)
		connectedClusterRequest = &models_cloudbreak.ConnectedClusterRequest{SourceClusterName: &skeleton.SharedClusterName}
	}

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

		credReq := &models_cloudbreak.CredentialSourceRequest{SourceName: "aws-access"}
		tags := make(map[string]interface{})
		if dataLake {
			tags["type"] = "datalake"
		}
		if len(skeleton.Tags) > 0 {
			tags[USER_TAGS] = skeleton.Tags
		}

		if len(skeleton.SharedClusterName) > 0 {
			tags["datalakeName"] = skeleton.SharedClusterName
			tags["datalakeId"] = *getCluster(skeleton.SharedClusterName).ID
			log.Infof("[CreateStack] tags added for datalake: %s", tags)
		}

		stackReq := models_cloudbreak.StackRequest{
			Name:             skeleton.ClusterName,
			CredentialSource: credReq,
			FailurePolicy:    &models_cloudbreak.FailurePolicyRequest{AdjustmentType: "BEST_EFFORT"},
			OnFailureAction:  &failureAction,
			InstanceGroups:   instanceGroups,
			Parameters:       stackParameters,
			CloudPlatform:    &platform,
			PlatformVariant:  &platform,
			Network:          createNetworkRequest(skeleton, getNetwork),
			AmbariVersion:    &ambariVersion,
			HdpVersion:       &skeleton.HDPVersion,
			Orchestrator:     &orchestrator,
			Tags:             tags,
		}

		// flex subscription
		flexSubscription := skeleton.FlexSubscription
		if flexSubscription != nil && len(flexSubscription.Name) > 0 {
			flexSubscription := getFlexSubscriptionByName(flexSubscription.Name)
			if flexSubscription != nil {
				stackReq.FlexID = flexSubscription.ID
			}
		}

		log.Infof("[CreateStack] sending stack create request with name: %s", skeleton.ClusterName)
		resp, err := postStack(&stacks.PostPrivateStackParams{Body: &stackReq})

		if err != nil {
			logErrorAndExit(err)
		}

		log.Infof("[CreateStack] stack created, id: %d", resp.Payload.ID)
		return *resp.Payload.ID
	}()

	// create cluster

	func() {
		masterConstraint := models_cloudbreak.Constraint{
			InstanceGroupName: &MASTER,
			HostCount:         int32(1),
		}

		workerConstraint := models_cloudbreak.Constraint{
			InstanceGroupName: &WORKER,
			HostCount:         int32(skeleton.Worker.InstanceCount),
		}

		computeConstraint := models_cloudbreak.Constraint{
			InstanceGroupName: &COMPUTE,
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
				hiveRds := createRDSRequest(hiveMetastore.MetaStore, HIVE_RDS, skeleton.HDPVersion, nil)
				rdsConfigs = append(rdsConfigs, hiveRds)
			} else if len(hiveMetastore.Name) > 0 {
				rdsConfigIds = append(rdsConfigIds, *getRdsConfig(hiveMetastore.Name).ID)
			}
		}
		// druid metastore
		druidMetastore := skeleton.DruidMetastore
		if druidMetastore != nil {
			if len(druidMetastore.URL) > 0 {
				druidRds := createRDSRequest(druidMetastore.MetaStore, DRUID_RDS, skeleton.HDPVersion, nil)
				rdsConfigs = append(rdsConfigs, druidRds)
			} else if len(druidMetastore.Name) > 0 {
				rdsConfigIds = append(rdsConfigIds, *getRdsConfig(druidMetastore.Name).ID)
			}
		}
		// ranger metastore
		rangerMetastore := skeleton.RangerMetastore
		if rangerMetastore != nil {
			if len(rangerMetastore.URL) > 0 {
				properties := []*models_cloudbreak.RdsConfigProperty{
					&models_cloudbreak.RdsConfigProperty{
						Name:  &(&stringWrapper{"rangerAdminPassword"}).s,
						Value: &(&stringWrapper{skeleton.ClusterAndAmbariPassword}).s,
					},
				}
				rangerRds := createRDSRequest(rangerMetastore.MetaStore, RANGER_RDS, skeleton.HDPVersion, properties)
				rdsConfigs = append(rdsConfigs, rangerRds)
			}
		}

		var inputs []*models_cloudbreak.BlueprintInput
		for key, value := range skeleton.ClusterInputs {
			newKey := key
			newValue := value
			inputs = append(inputs, &models_cloudbreak.BlueprintInput{Name: &newKey, PropertyValue: &newValue})
		}
		var ldapConfigId *int64 = nil
		if skeleton.Ldap != nil && len(*skeleton.Ldap) > 0 {
			ldap := getLdapConfig(*skeleton.Ldap)
			ldapConfigId = ldap.ID
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

		if dataLake {
			exposedServices = append(exposedServices, RANGER_EXPOSED_SERVICES...)
		}

		var ambariDatabase *models_cloudbreak.AmbariDatabaseDetails = nil
		if skeleton.AmbariDatabase != nil {
			db := skeleton.AmbariDatabase
			ambariDatabase = &models_cloudbreak.AmbariDatabaseDetails{
				Host:     db.Host,
				Port:     db.Port,
				UserName: db.Username,
				Password: db.Password,
				Name:     db.DatabaseName,
				Vendor:   db.DatabaseType,
			}
		}

		enableKnoxGateway := true
		gatewayType := "CENTRAL"

		clusterReq := models_cloudbreak.ClusterRequest{
			Name:                      skeleton.ClusterName,
			Blueprint:                 createBlueprintRequest(skeleton, blueprint),
			ConnectedCluster:          connectedClusterRequest,
			HostGroups:                hostGroups,
			UserName:                  skeleton.ClusterAndAmbariUser,
			Password:                  skeleton.ClusterAndAmbariPassword,
			RdsConfigJsons:            rdsConfigs,
			RdsConfigIds:              rdsConfigIds,
			BlueprintInputs:           inputs,
			LdapConfigID:              ldapConfigId,
			ValidateBlueprint:         &(&boolWrapper{false}).b,
			BlueprintCustomProperties: skeleton.Configurations,
			Gateway: &models_cloudbreak.GatewayJSON{
				EnableGateway:   &enableKnoxGateway,
				ExposedServices: exposedServices,
				GatewayType:     &gatewayType,
			},
			AmbariDatabaseDetails: ambariDatabase,
		}

		resp, err := postCluster(&cluster.PostClusterParams{ID: stackId, Body: &clusterReq})

		if err != nil {
			logErrorAndExit(err)
		}

		log.Infof("[CreateCluster] cluster created, id: %d", resp.Payload.ID)
	}()

	// create autoscaling policies

	func() {
		if skeleton.Autoscaling != nil && (len(skeleton.Autoscaling.Policies) > 0 || skeleton.Configurations != nil) {
			asClusterId := createBaseAutoscalingCluster(stackId)

			if skeleton.Autoscaling.Configuration != nil {
				setScalingConfigurations(asClusterId, *skeleton.Autoscaling.Configuration)
			}

			for _, p := range skeleton.Autoscaling.Policies {
				alertId := addPrometheusAlert(asClusterId, p)
				addScalingPolicy(asClusterId, p, alertId)
			}
		}
	}()

	return stackId
}

func ValidateCreateClusterSkeleton(c *cli.Context) error {
	skeleton := assembleClusterSkeleton(c)
	if skeleton.FlexSubscription != nil && len(skeleton.FlexSubscription.Name) > 0 {
		cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
		return skeleton.Validate(cbClient)
	}
	return skeleton.Validate(nil)
}

func RepairCluster(c *cli.Context) error {
	defer timeTrack(time.Now(), "repair cluster")
	checkRequiredFlags(c)

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
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

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	clusterName := c.String(FlClusterName.Name)
	stack := resizeClusterImpl(clusterName, nodeType, adjustment,
		oAuth2Client.GetClusterByName,
		oAuth2Client.Cloudbreak.Stacks.PutStack,
		oAuth2Client.Cloudbreak.Cluster.PutCluster)

	oAuth2Client.waitForClusterToFinish(*stack.ID, c)

	return nil
}

func resizeClusterImpl(clusterName string, nodeType string, adjustment int32,
	getStack func(string) *models_cloudbreak.StackResponse,
	putStack func(*stacks.PutStackParams) error,
	putCluster func(*cluster.PutClusterParams) error) *models_cloudbreak.StackResponse {

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
		if err := putStack(&stacks.PutStackParams{ID: *stack.ID, Body: update}); err != nil {
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
		if err := putCluster(&cluster.PutClusterParams{ID: *stack.ID, Body: update}); err != nil {
			logErrorAndExit(err)
		}
	}

	return stack
}

func GenerateCreateClusterSkeleton(c *cli.Context) error {
	Println(getBaseSkeleton().JsonPretty())
	return nil
}

func convertClusterInputs(skeleton *ClusterSkeleton) {
	if skeleton.RangerMetastore == nil {
		logErrorAndExit(errors.New("the cluster template shall contain RangerMetastore"))
		return
	}

	validationErrors := skeleton.RangerMetastore.Validate()
	if len(validationErrors) > 0 {
		var messages []string = nil
		for _, e := range validationErrors {
			messages = append(messages, e.Error())
		}
		logErrorAndExit(errors.New(strings.Join(messages, ", ")))
		return
	}

	if len(skeleton.CloudStoragePath) == 0 {
		logErrorAndExit(errors.New("CloudStoragePath is a required value"))
		return
	}

	putToClusterInput(skeleton, "S3_BUCKET", skeleton.CloudStoragePath)

}

func putToClusterInput(skeleton *ClusterSkeleton, key string, value string) {
	if skeleton.ClusterInputs == nil {
		skeleton.ClusterInputs = make(map[string]string)
	}

	skeleton.ClusterInputs[key] = value

}

func fillSharedParameters(skeleton *ClusterSkeleton,
	getBlueprint func(string) *models_cloudbreak.BlueprintResponse,
	getCluster func(string) *models_cloudbreak.StackResponse,
	getClusterConfig func(int64, []*models_cloudbreak.BlueprintParameter) []*models_cloudbreak.BlueprintInput) {

	stack := getCluster(skeleton.SharedClusterName)
	if *stack.Status != "AVAILABLE" && *stack.Cluster.Status != "AVAILABLE" {
		logErrorAndExit(errors.New("the cluster is not 'AVAILABLE' yet, please try again later"))
		return
	}

	ambariBp := getBlueprint(skeleton.ClusterType)

	skeleton.Ldap = &(&stringWrapper{" "}).s
	var inputs = make(map[string]string)
	for _, input := range ambariBp.Inputs {
		if !strings.Contains(*input.Name, "LDAP") {
			inputs[*input.Name] = ""
		}
	}
	skeleton.ClusterInputs = inputs

	if stack.Cluster != nil && stack.Cluster.LdapConfig != nil {
		skeleton.Ldap = &stack.Cluster.LdapConfig.Name
	}

	network := stack.Network
	if network != nil && network.Parameters["internetGatewayId"] == nil {
		skeleton.Network = &Network{VpcId: network.Parameters["vpcId"].(string), SubnetId: network.Parameters["subnetId"].(string)}
	}

	if stack.Cluster != nil && len(stack.Cluster.RdsConfigs) > 0 {
		for _, rds := range stack.Cluster.RdsConfigs {
			if rds.Type != nil {
				if *rds.Type == HIVE_RDS {
					skeleton.HiveMetastore = &HiveMetastore{
						MetaStore: MetaStore{Name: rds.Name},
					}
				} else if *rds.Type == DRUID_RDS {
					skeleton.DruidMetastore = &DruidMetastore{
						MetaStore: MetaStore{Name: rds.Name},
					}
				}
			}
		}
	}

	skeletonJson, _ := json.Marshal(skeleton)
	log.Debugf("[fillSharedParameters] ephemeral cluster skeleton: %s", string(skeletonJson))

}

func getBaseSkeleton() *ClusterSkeleton {
	return &ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterType: getDefaultClusterType(),
			HDPVersion:  "2.6",
			Master: InstanceConfig{
				InstanceType:  "m4.4xlarge",
				VolumeType:    "gp2",
				VolumeCount:   &(&int32Wrapper{1}).i,
				VolumeSize:    &(&int32Wrapper{32}).i,
				Encrypted:     &(&boolWrapper{false}).b,
				InstanceCount: 1,
				Recipes:       []Recipe{},
			},
			Worker: InstanceConfig{
				InstanceType:  "m3.xlarge",
				VolumeType:    "ephemeral",
				VolumeCount:   &(&int32Wrapper{2}).i,
				VolumeSize:    &(&int32Wrapper{40}).i,
				Encrypted:     &(&boolWrapper{false}).b,
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
					Encrypted:     &(&boolWrapper{false}).b,
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
			FlexSubscription:       &FlexSubscriptionBase{},
		},
		Autoscaling: &AutoscalingSkeletonBase{
			Configuration: &AutoscalingConfiguration{
				CooldownTime:   30,
				ClusterMinSize: 3,
				ClusterMaxSize: 100,
			},
			Policies: []AutoscalingPolicy{},
		},
		HiveMetastore:  &HiveMetastore{},
		DruidMetastore: &DruidMetastore{},
		Configurations: []models_cloudbreak.Configurations{},
	}
}
