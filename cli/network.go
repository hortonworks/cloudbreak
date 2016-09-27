package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/networks"
	"github.com/hortonworks/hdc-cli/models"
	"strconv"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateNetwork(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defer timeTrack(time.Now(), "create network")
	defer wg.Done()

	networkName := "net" + strconv.FormatInt(time.Now().UnixNano(), 10)

	vpc := skeleton.Network
	var vpcParams = make(map[string]interface{})
	if vpc != nil && len(vpc.VpcId) > 0 && len(vpc.SubnetId) > 0 {
		vpcParams["vpcId"] = vpc.VpcId
		vpcParams["subnetId"] = vpc.SubnetId
	} else {
		defaultNetwork := c.GetNetwork("aws-network")
		vpcParams["vpcId"] = defaultNetwork.Parameters["vpcId"]
		vpcParams["internetGatewayId"] = defaultNetwork.Parameters["internetGatewayId"]
	}

	network := models.NetworkJSON{
		Name:          networkName,
		CloudPlatform: "AWS",
		Parameters:    vpcParams,
	}

	log.Infof("[CreateNetwork] sending network create request with name: %s", networkName)
	resp, err := c.Cloudbreak.Networks.PostNetworksAccount(&networks.PostNetworksAccountParams{&network})

	if err != nil {
		logErrorAndExit(c.CreateNetwork, err.Error())
	}

	log.Infof("[CreateNetwork] network created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID
}

func (c *Cloudbreak) GetNetwork(name string) models.NetworkJSON {
	log.Infof("[GetNetwork] sending get request to find network with name: %s", name)
	resp, err := c.Cloudbreak.Networks.GetNetworksAccountName(&networks.GetNetworksAccountNameParams{Name: name})

	if err != nil {
		logErrorAndExit(c.GetNetwork, err.Error())
	}

	defaultNetwork := *resp.Payload
	log.Infof("[GetNetwork] found network, name: %s", defaultNetwork.Name)
	return defaultNetwork
}

func (c *Cloudbreak) GetNetworkById(id int64) *models.NetworkJSON {
	log.Infof("[GetNetwork] sending get request to find network with id: %d", id)
	resp, err := c.Cloudbreak.Networks.GetNetworksID(&networks.GetNetworksIDParams{ID: id})

	if err != nil {
		logErrorAndExit(c.GetNetwork, err.Error())
	}

	network := resp.Payload
	log.Infof("[GetNetwork] found network, name: %s", network.Name)
	return network
}

func (c *Cloudbreak) GetPublicNetworks() []*models.NetworkJSON {
	defer timeTrack(time.Now(), "get public networks")
	resp, err := c.Cloudbreak.Networks.GetNetworksAccount(&networks.GetNetworksAccountParams{})
	if err != nil {
		logErrorAndExit(c.GetPublicNetworks, err.Error())
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteNetwork(name string) error {
	defer timeTrack(time.Now(), "delete network")
	log.Infof("[DeleteNetwork] delete network: %s", name)
	return c.Cloudbreak.Networks.DeleteNetworksAccountName(&networks.DeleteNetworksAccountNameParams{Name: name})
}
