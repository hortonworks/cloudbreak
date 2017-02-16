package cli

import (
	"sync"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/stacks"

	"time"

	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
)

var ClusterListHeader = []string{"Cluster Name", "Status", "HDP Version", "Cluster Type", "Nodes status"}
var ClusterNodeHeader = []string{"Instance ID", "Hostname", "Public IP", "Private IP", "Instance Status", "Host Status", "Type"}

type ClusterListElement struct {
	ClusterName string `json:"ClusterName" yaml:"ClusterName"`
	HDPVersion  string `json:"HDPVersion" yaml:"HDPVersion"`
	ClusterType string `json:"ClusterType" yaml:"ClusterType"`
	Status      string `json:"Status,omitempty" yaml:"Status,omitempty"`
	Nodes       string `json:"NodesStatus,omitempty" yaml:"NodesStatus,omitempty"`
}

func (c *ClusterListElement) DataAsStringArray() []string {
	return []string{c.ClusterName, c.Status, c.HDPVersion, c.ClusterType, c.Nodes}
}

type ClusterNode struct {
	InstanceId     string `json:"InstanceId" yaml:"InstanceId"`
	Hostname       string `json:"Hostname" yaml:"Hostname"`
	PublicIP       string `json:"PublicIP,omitempty" yaml:"PublicIP,omitempty"`
	PrivateIP      string `json:"PrivateIP" yaml:"PrivateIP"`
	InstanceStatus string `json:"InstanceStatus" yaml:"InstanceStatus"`
	HostStatus     string `json:"HostStatus" yaml:"HostStatus"`
	Type           string
}

func (c *ClusterNode) DataAsStringArray() []string {
	return []string{c.InstanceId, c.Hostname, c.PublicIP, c.PrivateIP, c.InstanceStatus, c.HostStatus, c.Type}
}

func ListClusters(c *cli.Context) error {
	defer timeTrack(time.Now(), "list clusters")
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := Output{Format: c.String(FlOutput.Name)}

	return listClustersImpl(oAuth2Client.Cloudbreak.Stacks.GetStacksUser, oAuth2Client.FetchCluster, output.WriteList)
}

func listClustersImpl(getStacks func(*stacks.GetStacksUserParams) (*stacks.GetStacksUserOK, error),
	fetchCluster func(*models.StackResponse) (*ClusterSkeletonResult, error), writer func([]string, []Row)) error {

	respStacks, err := getStacks(&stacks.GetStacksUserParams{})
	if err != nil {
		log.Error(err)
		return err
	}
	var wg sync.WaitGroup
	clusters := make([]ClusterSkeletonResult, len(respStacks.Payload))
	for i, stack := range respStacks.Payload {
		wg.Add(1)
		go func(i int, stack *models.StackResponse) {
			defer wg.Done()

			clusterSkeleton, _ := fetchCluster(stack)
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
			Nodes:       v.Nodes,
		}
		tableRows[i] = clusterListElement
	}

	writer(ClusterListHeader, tableRows)

	return nil
}

func ListClusterNodes(c *cli.Context) error {
	defer timeTrack(time.Now(), "list cluster nodes")

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, ListClusterNodes)
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := Output{Format: c.String(FlOutput.Name)}

	listClusterNodesImpl(clusterName, oAuth2Client.Cloudbreak.Stacks.GetStacksUserName, output.WriteList)

	return nil
}

func listClusterNodesImpl(clusterName string, getStack func(*stacks.GetStacksUserNameParams) (*stacks.GetStacksUserNameOK, error), writer func([]string, []Row)) {
	respStack, err := getStack(&stacks.GetStacksUserNameParams{Name: clusterName})
	if err != nil {
		logErrorAndExit(listClusterNodesImpl, err.Error())
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
			var hostStatus string = getHostStatus(respStack.Payload, metadata)
			row := &ClusterNode{
				InstanceId:     SafeStringConvert(data.InstanceID),
				Hostname:       SafeStringConvert(data.DiscoveryFQDN),
				PublicIP:       SafeStringConvert(data.PublicIP),
				PrivateIP:      SafeStringConvert(data.PrivateIP),
				InstanceStatus: SafeStringConvert(data.InstanceStatus),
				HostStatus:     hostStatus,
				Type:           nodeType,
			}
			tableRows = append(tableRows, row)
		}
	}

	writer(ClusterNodeHeader, tableRows)
}

func getHostStatus(stack *models.StackResponse, imd *models.InstanceMetaData) string {
	var result string = ""
	if stack.Cluster != nil && imd.DiscoveryFQDN != nil {
		for _, hg := range stack.Cluster.HostGroups {
			if hg.Name == *imd.InstanceGroup {
				for _, hmd := range hg.Metadata {
					if hmd.Name == *imd.DiscoveryFQDN {
						result = *hmd.State
						break
					}
				}
				if result != "" {
					break
				}
			}
		}
	}
	return result
}
