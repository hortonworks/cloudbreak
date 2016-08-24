package cli

import (
	"encoding/json"
	"fmt"
	log "github.com/Sirupsen/logrus"
	apiclient "github.com/sequenceiq/hdc-cli/client"
	"github.com/sequenceiq/hdc-cli/client/stacks"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
)

type Cluster struct {
	ClusterName              string `json:"ClusterName" yaml:"ClusterName"`
	HDPVersion               string `json:"HDPVersion" yaml:"HDPVersion"`
	MasterInstanceType       string `json:"MasterInstanceType" yaml:"MasterInstanceType"`
	WorkerInstanceType       string `json:"WorkerInstanceType" yaml:"WorkerInstanceType"`
	InstanceCount            int    `json:"InstanceCount" yaml:"InstanceCount"`
	SSHKeyName               string `json:"SSHKeyName" yaml:"SSHKeyName"`
	RemoteAccess             string `json:"RemoteAccess" yaml:"RemoteAccess"`
	ClusterAndAmbariUser     string `json:"ClusterAndAmbariUser" yaml:"ClusterAndAmbariUser"`
	ClusterAndAmbariPassword string `json:"ClusterAndAmbariPassword" yaml:"ClusterAndAmbariPassword"`
	WebAccess                string `json:"WebAccess" yaml:"WebAccess"`
	InstanceRole             string `json:"InstanceRole" yaml:"InstanceRole"`
	HiveMetastoreUrl         string `json:"HiveMetastoreUrl" yaml:"HiveMetastoreUrl"`
	HiveMetastoreUser        string `json:"HiveMetastoreUser" yaml:"HiveMetastoreUser"`
	HiveMetastorePassword    string `json:"HiveMetastorePassword" yaml:"HiveMetastorePassword"`
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
	fmt.Println("not implemented yet, use the generate-cli-skeleton subcommand")
	return nil
}

func GenerateCreateClusterSkeleton(c *cli.Context) error {
	fmt.Println(Cluster{}.Json())
	return nil
}
