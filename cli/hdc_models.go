package cli

import "github.com/hortonworks/hdc-cli/models"

const (
	MASTER   = "master"
	WORKER   = "worker"
	COMPUTE	 = "compute"
	POSTGRES = "POSTGRES"
	PRE      = "pre"
	POST     = "post"
)

var SUPPORTED_HDP_VERSIONS = [...]float64{2.5}

var ClusterSkeletonHeader []string = []string{"Cluster Name", "HDP Version", "Cluster Type", "Master", "Worker",
	"SSH Key Name", "Remote Access", "WebAccess", "User", "Status", "Status Reason"}

type ClusterSkeletonBase struct {
	ClusterName              string            	`json:"ClusterName" yaml:"ClusterName"`
	HDPVersion               string            	`json:"HDPVersion" yaml:"HDPVersion"`
	ClusterType              string            	`json:"ClusterType" yaml:"ClusterType"`
	Master                   InstanceConfig    	`json:"Master" yaml:"Master"`
	Worker                   InstanceConfig    	`json:"Worker" yaml:"Worker"`
	Compute                  SpotInstanceConfig   	`json:"Compute" yaml:"Compute"`
	SSHKeyName               string            	`json:"SSHKeyName" yaml:"SSHKeyName"`
	RemoteAccess             string            	`json:"RemoteAccess" yaml:"RemoteAccess"`
	WebAccess                bool              	`json:"WebAccess" yaml:"WebAccess"`
	ClusterAndAmbariUser     string            	`json:"ClusterAndAmbariUser" yaml:"ClusterAndAmbariUser"`
	ClusterAndAmbariPassword string            	`json:"ClusterAndAmbariPassword" yaml:"ClusterAndAmbariPassword"`
	InstanceRole             string            	`json:"InstanceRole,omitempty" yaml:"InstanceRole"`
	Network                  *Network          	`json:"Network,omitempty" yaml:"Network,omitempty"`
	ClusterInputs            map[string]string 	`json:"ClusterInputs,omitempty" yaml:"ClusterInputs,omitempty"`
}

type ClusterSkeleton struct {
	ClusterSkeletonBase
	HiveMetastore  *HiveMetastore          `json:"HiveMetastore,omitempty" yaml:"HiveMetastore,omitempty"`
	Configurations []models.Configurations `json:"Configurations" yaml:"Configurations"`
}

type ClusterSkeletonResult struct {
	ClusterSkeletonBase
	HiveMetastore  *HiveMetastoreResult    `json:"HiveMetastore,omitempty" yaml:"HiveMetastore,omitempty"`
	Configurations []models.Configurations `json:"Configurations,omitempty" yaml:"Configurations,omitempty"`
	Status         string                  `json:"Status,omitempty" yaml:"Status,omitempty"`
	StatusReason   string                  `json:"StatusReason,omitempty" yaml:"StatusReason,omitempty"`
}

type InstanceConfig struct {
	InstanceType  string   `json:"InstanceType" yaml:"InstanceType"`
	VolumeType    string   `json:"VolumeType" yaml:"VolumeType"`
	VolumeSize    *int32   `json:"VolumeSize" yaml:"VolumeSize"`
	VolumeCount   *int32   `json:"VolumeCount" yaml:"VolumeCount"`
	InstanceCount int32    `json:"InstanceCount,omitempty" yaml:"InstanceCount,omitempty"`
	Recipes       []Recipe `json:"Recipes" yaml:"Recipes"`
}

type SpotInstanceConfig struct {
	InstanceConfig
	SpotPrice     string   `json:"SpotPrice" yaml:"SpotPrice"`
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
