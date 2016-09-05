package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"sync"

	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
)

var ClusterListHeader []string = []string{"Cluster Name", "Status", "HDP Version", "Cluster Type"}

type ClusterListElement struct {
	ClusterName string `json:"ClusterName" yaml:"ClusterName"`
	HDPVersion  string `json:"HDPVersion" yaml:"HDPVersion"`
	ClusterType string `json:"ClusterType" yaml:"ClusterType"`
	Status      string `json:"Status,omitempty" yaml:"Status,omitempty"`
}

func (c *ClusterListElement) DataAsStringArray() []string {
	return []string{c.ClusterName, c.Status, c.HDPVersion, c.ClusterType}
}

func ListClusters(c *cli.Context) error {
	oAuth2Client, err := NewOAuth2HTTPClient(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))

	if err != nil {
		log.Error(err)
		newExitReturnError()
	}

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

	output := Output{Format: c.String(FlCBOutput.Name)}

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
	output.WriteList(ClusterListHeader, tableRows)

	return nil
}
