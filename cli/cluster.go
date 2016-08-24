package cli

import (
	"encoding/json"
	"fmt"
	log "github.com/Sirupsen/logrus"
	apiclient "github.com/sequenceiq/hdc-cli/client"
	"github.com/sequenceiq/hdc-cli/client/stacks"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
)

type Cluster struct {
	ClusterName         string        `json:"ClusterName" yaml:"ClusterName"`
	MasterInstanceGroup InstanceGroup `json:"MasterInstanceGroup" yaml:"MasterInstanceGroup"`
	WorkerInstanceGroup InstanceGroup `json:"WorkerInstanceGroup" yaml:"WorkerInstanceGroup"`
	CredentialId        string        `json:"CredentialId" yaml:"CredentialId"`
	Region              string        `json:"Region" yaml:"Region"`
	NetworkId           string        `json:"NetworkId" yaml:"NetworkId"`
	SecurityGroupId     string        `json:"SecurityGroupId" yaml:"SecurityGroupId"`
	AvailabilityZone    string        `json:"AvailabilityZone" yaml:"AvailabilityZone"`

	//RemoteAccess             string `json:"RemoteAccess" yaml:"RemoteAccess"`
	//SSHKeyName               string `json:"SSHKeyName" yaml:"SSHKeyName"`
	//HDPVersion               string `json:"HDPVersion" yaml:"HDPVersion"`
	//InstanceCount            int    `json:"InstanceCount" yaml:"InstanceCount"`
	//ClusterAndAmbariUser     string `json:"ClusterAndAmbariUser" yaml:"ClusterAndAmbariUser"`
	//ClusterAndAmbariPassword string `json:"ClusterAndAmbariPassword" yaml:"ClusterAndAmbariPassword"`
	//WebAccess                string `json:"WebAccess" yaml:"WebAccess"`
	//InstanceRole             string `json:"InstanceRole" yaml:"InstanceRole"`
	//HiveMetastoreUrl         string `json:"HiveMetastoreUrl" yaml:"HiveMetastoreUrl"`
	//HiveMetastoreUser        string `json:"HiveMetastoreUser" yaml:"HiveMetastoreUser"`
	//HiveMetastorePassword    string `json:"HiveMetastorePassword" yaml:"HiveMetastorePassword"`
}

type InstanceGroup struct {
	Name            string `json:"Name,omitempty" yaml:"Name"`
	TemplateId      string `json:"TemplateId" yaml:"TemplateId"`
	NodeCount       int    `json:"NodeCount" yaml:"NodeCount"`
	SecurityGroupId string `json:"SecurityGroupId,omitempty," ymal:"SecurityGroupId"`
	Type            string `json:"Type,omitempty" yaml:"Type"`
}

func (c Cluster) Json() string {
	j, _ := json.MarshalIndent(c, "", "  ")
	return string(j)
}

func (c Cluster) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func ListClusters(c *cli.Context) error {
	client := apiclient.NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))

	resp, err := client.Stacks.GetStacksUser(&stacks.GetStacksUserParams{})
	if err != nil {
		log.Error(err)
	}

	for _, v := range resp.Payload {
		fmt.Printf("%s\n", v.Name)
	}
	return nil
}

func CreateCluster(c *cli.Context) error {
	path := c.String(FlCBInputJson.Name)
	if len(path) == 0 {
		log.Errorf("[CreateCluster] missing parameter: %s", FlCBInputJson.Name)
		return cli.NewExitError("", 1)
	}

	if _, err := os.Stat(path); os.IsNotExist(err) {
		log.Errorf("[CreateCluster] %s", err.Error())
		return newExitError()
	}

	log.Infof("[CreateCluster] read cluster create json from file: %s", path)
	content, err := ioutil.ReadFile(path)
	if err != nil {
		log.Errorf("[CreateCluster] %s", err.Error())
		return newExitError()
	}

	var cluster Cluster
	err = json.Unmarshal(content, &cluster)
	if err != nil {
		log.Errorf("[CreateCluster] %s", err.Error())
		return newExitError()
	}

	fmt.Println(cluster.Json())
	return nil
}

func GenerateCreateClusterSkeleton(c *cli.Context) error {
	fmt.Println(Cluster{}.Json())
	return nil
}
