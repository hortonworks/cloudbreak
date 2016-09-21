package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"sync"

	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
	"time"
)

var ClusterListHeader = []string{"Cluster Name", "Status", "HDP Version", "Cluster Type"}
var ClusterNodeHeader = []string{"Instance ID", "Hostname", "Public IP", "Private IP", "Type"}

type ClusterListElement struct {
	ClusterName string `json:"ClusterName" yaml:"ClusterName"`
	HDPVersion  string `json:"HDPVersion" yaml:"HDPVersion"`
	ClusterType string `json:"ClusterType" yaml:"ClusterType"`
	Status      string `json:"Status,omitempty" yaml:"Status,omitempty"`
}

func (c *ClusterListElement) DataAsStringArray() []string {
	return []string{c.ClusterName, c.Status, c.HDPVersion, c.ClusterType}
}

type ClusterNode struct {
	InstanceId string `json:"InstanceId" yaml:"InstanceId"`
	Hostname   string `json:"Hostname" yaml:"Hostname"`
	PublicIP   string `json:"PublicIP,omitempty" yaml:"PublicIP,omitempty"`
	PrivateIP  string `json:"PrivateIP" yaml:"PrivateIP"`
	Type       string
}

func (c *ClusterNode) DataAsStringArray() []string {
	return []string{c.InstanceId, c.Hostname, c.PublicIP, c.PrivateIP, c.Type}
}

func ListClusters(c *cli.Context) error {
	defer timeTrack(time.Now(), "list clusters")
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	respStacks, err := oAuth2Client.Cloudbreak.Stacks.GetStacksUser(&stacks.GetStacksUserParams{})
	if err != nil {
		log.Error(err)
		return err
	}
	var wg sync.WaitGroup
	clusters := make([]ClusterSkeleton, len(respStacks.Payload))
	for i, stack := range respStacks.Payload {
		wg.Add(1)
		go func(i int, stack *models.StackResponse) {

			defer wg.Done()
			clusterSkeleton, _ := oAuth2Client.FetchCluster(stack, true)
			clusters[i] = *clusterSkeleton

		}(i, stack)
	}
	wg.Wait()

	tableRows := make([]Row, len(respStacks.Payload))

	for i, v := range clusters {
		clusterListElement := &ClusterListElement{
			ClusterName: v.ClusterName,
			HDPVersion:  v.HDPVersion,
			ClusterType: v.ClusterType,
			Status:      v.Status,
		}
		tableRows[i] = clusterListElement
	}
	output := Output{Format: c.String(FlOutput.Name)}
	output.WriteList(ClusterListHeader, tableRows)

	return nil
}

func ListClusterNodes(c *cli.Context) error {
	defer timeTrack(time.Now(), "list cluster nodes")

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, ListClusterNodes)
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	respStack, err := oAuth2Client.Cloudbreak.Stacks.GetStacksUserName(&stacks.GetStacksUserNameParams{Name: clusterName})
	if err != nil {
		logErrorAndExit(ListClusterNodes, err.Error())
	}

	var tableRows []Row
	for _, instanceGroup := range respStack.Payload.InstanceGroups {
		metadataArray := instanceGroup.Metadata
		for _, metadata := range metadataArray {
			data := *metadata
			if data.DiscoveryFQDN == nil {
				continue
			}
			nodeType := *data.InstanceGroup
			if nodeType == MASTER {
				nodeType = "master - ambari server"
			}
			row := &ClusterNode{
				InstanceId: SafeStringConvert(data.InstanceID),
				Hostname:   SafeStringConvert(data.DiscoveryFQDN),
				PublicIP:   SafeStringConvert(data.PublicIP),
				PrivateIP:  SafeStringConvert(data.PrivateIP),
				Type:       nodeType,
			}
			tableRows = append(tableRows, row)
		}
	}

	output := Output{Format: c.String(FlOutput.Name)}
	output.WriteList(ClusterNodeHeader, tableRows)
	return nil
}
