package cli

import "github.com/hortonworks/hdc-cli/models_cloudbreak"

const (
	MASTER    = "master"
	WORKER    = "worker"
	COMPUTE   = "compute"
	POSTGRES  = "POSTGRES"
	PRE       = "pre"
	POST      = "post"
	USER_TAGS = "userDefined"
	UNKNOWN   = "UNKNOWN"
	HEALTHY   = "HEALTHY"
	UNHEALTHY = "UNHEALTHY"
	HIVE_RDS  = "HIVE"
	DRUID_RDS = "DRUID"
)

var SUPPORTED_HDP_VERSIONS = [...]float64{2.5, 2.6}

var ClusterSkeletonHeader []string = []string{"Cluster Name", "HDP Version", "Cluster Type", "Master", "Worker", "Compute",
	"SSH Key Name", "Remote Access", "WebAccess", "User", "Status", "Status Reason"}

type ClusterSkeletonBase struct {
	ClusterName              string               `json:"ClusterName" yaml:"ClusterName"`
	HDPVersion               string               `json:"HDPVersion" yaml:"HDPVersion"`
	ClusterType              string               `json:"ClusterType" yaml:"ClusterType"`
	Master                   InstanceConfig       `json:"Master" yaml:"Master"`
	Worker                   InstanceConfig       `json:"Worker" yaml:"Worker"`
	Compute                  SpotInstanceConfig   `json:"Compute" yaml:"Compute"`
	SSHKeyName               string               `json:"SSHKeyName" yaml:"SSHKeyName"`
	RemoteAccess             string               `json:"RemoteAccess" yaml:"RemoteAccess"`
	WebAccess                bool                 `json:"WebAccess" yaml:"WebAccess"`
	HiveJDBCAccess           bool                 `json:"HiveJDBCAccess" yaml:"HiveJDBCAccess"`
	ClusterComponentAccess   bool                 `json:"ClusterComponentAccess" yaml:"ClusterComponentAccess"`
	ClusterAndAmbariUser     string               `json:"ClusterAndAmbariUser" yaml:"ClusterAndAmbariUser"`
	ClusterAndAmbariPassword string               `json:"ClusterAndAmbariPassword" yaml:"ClusterAndAmbariPassword"`
	InstanceRole             string               `json:"InstanceRole,omitempty" yaml:"InstanceRole"`
	Network                  *Network             `json:"Network,omitempty" yaml:"Network,omitempty"`
	ClusterInputs            map[string]string    `json:"ClusterInputs,omitempty" yaml:"ClusterInputs,omitempty"`
	Tags                     map[string]string    `json:"Tags" yaml:"Tags"`
	Autoscaling              *AutoscalingSkeleton `json:"Autoscaling,omitempty" yaml:"Autoscaling,omitempty"`
}

type ClusterSkeleton struct {
	ClusterSkeletonBase
	HiveMetastore  *HiveMetastore                     `json:"HiveMetastore,omitempty" yaml:"HiveMetastore,omitempty"`
	DruidMetastore *DruidMetastore                    `json:"DruidMetastore,omitempty" yaml:"DruidMetastore,omitempty"`
	Configurations []models_cloudbreak.Configurations `json:"Configurations" yaml:"Configurations"`
}

type ClusterSkeletonResult struct {
	ClusterSkeletonBase
	HiveMetastore  *HiveMetastoreResult               `json:"HiveMetastore,omitempty" yaml:"HiveMetastore,omitempty"`
	DruidMetastore *DruidMetastoreResult              `json:"DruidMetastore,omitempty" yaml:"DruidMetastore,omitempty"`
	Configurations []models_cloudbreak.Configurations `json:"Configurations,omitempty" yaml:"Configurations,omitempty"`
	Nodes          string                             `json:"NodesStatus,omitempty" yaml:"NodesStatus,omitempty"`
	Status         string                             `json:"Status,omitempty" yaml:"Status,omitempty"`
	StatusReason   string                             `json:"StatusReason,omitempty" yaml:"StatusReason,omitempty"`
}

type InstanceConfig struct {
	InstanceType  string   `json:"InstanceType" yaml:"InstanceType"`
	VolumeType    string   `json:"VolumeType" yaml:"VolumeType"`
	VolumeSize    *int32   `json:"VolumeSize" yaml:"VolumeSize"`
	VolumeCount   *int32   `json:"VolumeCount" yaml:"VolumeCount"`
	InstanceCount int32    `json:"InstanceCount" yaml:"InstanceCount"`
	Recipes       []Recipe `json:"Recipes" yaml:"Recipes"`
	RecoveryMode  string   `json:"RecoveryMode,omitempty" yaml:"RecoveryMode,omitempty"`
}

type SpotInstanceConfig struct {
	InstanceConfig
	SpotPrice string `json:"SpotPrice" yaml:"SpotPrice"`
}

type Recipe struct {
	URI   string `json:"URI" yaml:"URI"`
	Phase string `json:"Phase" yaml:"Phase"`
}

type Network struct {
	VpcId    string `json:"VpcId" yaml:"VpcId"`
	SubnetId string `json:"SubnetId" yaml:"SubnetId"`
}

type MetaStore struct {
	Name         string `json:"Name" yaml:"Name"`
	Username     string `json:"Username" yaml:"Username"`
	Password     string `json:"Password" yaml:"Password"`
	URL          string `json:"URL" yaml:"URL"`
	DatabaseType string `json:"DatabaseType" yaml:"DatabaseType"`
}

type HiveMetastore struct {
	MetaStore
}

type HiveMetastoreResult struct {
	Name string `json:"Name" yaml:"Name"`
}

type DruidMetastore struct {
	MetaStore
}

type DruidMetastoreResult struct {
	Name string `json:"Name" yaml:"Name"`
}

type AutoscalingSkeleton struct {
	Configuration *AutoscalingConfiguration `json:"Configurations,omitempty" yaml:"Configurations,omitempty"`
	Policies      []AutoscalingPolicy       `json:"AutoscalingPolicies" yaml:"AutoscalingPolicies"`
}

type AutoscalingConfiguration struct {
	CooldownTime   int32 `json:"CooldownTime" yaml:"CooldownTime"`
	ClusterMinSize int32 `json:"ClusterMinSize" yaml:"ClusterMinSize"`
	ClusterMaxSize int32 `json:"ClusterMaxSize" yaml:"ClusterMaxSize"`
}

type AutoscalingPolicy struct {
	Name              string  `json:"Name" yaml:"Name"`
	ScalingAdjustment int32   `json:"ScalingAdjustment" yaml:"ScalingAdjustment"`
	ScalingDefinition string  `json:"ScalingDefinition" yaml:"ScalingDefinition"`
	Operator          string  `json:"Operator" yaml:"Operator"`
	Threshold         float64 `json:"Threshold" yaml:"Threshold"`
	Period            int32   `json:"Period" yaml:"Period"`
	NodeType          string  `json:"NodeType" yaml:"NodeType"`
}
