package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/networks"
	"github.com/sequenceiq/hdc-cli/models"
	"strconv"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateNetwork(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	networkName := "net" + strconv.FormatInt(time.Now().UnixNano(), 10)

	network := models.NetworkJSON{
		Name:          networkName,
		SubnetCIDR:    "10.0.0.0/16",
		CloudPlatform: "AWS",
	}

	log.Infof("[CreateNetwork] sending network create request with name: %s", networkName)
	resp, err := c.Cloudbreak.Networks.PostNetworksAccount(&networks.PostNetworksAccountParams{&network})

	if err != nil {
		log.Errorf("[CreateNetwork] %s", err.Error())
		newExitReturnError()
	}

	log.Infof("[CreateNetwork] network created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID

	wg.Done()
}
