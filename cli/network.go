package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/networks"
	"github.com/sequenceiq/hdc-cli/models"
	"strconv"
	"sync"
	"time"
)

func (c *Cloudbreak) CopyDefaultNetwork(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defaultNetwork := c.GetNetwork("aws-network")
	channel <- c.CreateNetwork(defaultNetwork)
	wg.Done()
}

func (c *Cloudbreak) CreateNetwork(defaultNetwork models.NetworkJSON) int64 {
	networkName := "net" + strconv.FormatInt(time.Now().UnixNano(), 10)

	var vpcParams = make(map[string]interface{})
	vpcParams["vpcId"] = defaultNetwork.Parameters["vpcId"]
	vpcParams["internetGatewayId"] = defaultNetwork.Parameters["internetGatewayId"]

	network := models.NetworkJSON{
		Name:          networkName,
		SubnetCIDR:    defaultNetwork.SubnetCIDR,
		CloudPlatform: "AWS",
		Parameters:    vpcParams,
	}

	log.Infof("[CreateNetwork] sending network create request with name: %s", networkName)
	resp, err := c.Cloudbreak.Networks.PostNetworksAccount(&networks.PostNetworksAccountParams{&network})

	if err != nil {
		log.Errorf("[CreateNetwork] %s", err.Error())
		newExitReturnError()
	}

	log.Infof("[CreateNetwork] network created, id: %d", resp.Payload.ID)
	return resp.Payload.ID
}

func (c *Cloudbreak) GetNetwork(name string) models.NetworkJSON {
	log.Infof("[GetNetwork] sending get request to find network with name: %s", name)
	resp, err := c.Cloudbreak.Networks.GetNetworksAccountName(&networks.GetNetworksAccountNameParams{Name: name})

	if err != nil {
		log.Errorf("[GetNetwork] %s", err.Error())
		newExitReturnError()
	}

	defaultNetwork := *resp.Payload
	log.Infof("[GetNetwork] found network, name: %s id: %d", defaultNetwork.Name, defaultNetwork.ID)
	return defaultNetwork
}
