package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/templates"
	"github.com/sequenceiq/hdc-cli/models"
	"strconv"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateTemplate(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {

	masterTemplateName := "mtempl" + strconv.FormatInt(time.Now().UnixNano(), 10)
	workerTemplateName := "wtempl" + strconv.FormatInt(time.Now().UnixNano(), 10)

	volumeType := "gp2"
	volumeSize := int32(100)

	masterTemplateReqBody := models.TemplateRequest{
		Name:          masterTemplateName,
		CloudPlatform: "AWS",
		InstanceType:  skeleton.MasterInstanceType,
		VolumeType:    &volumeType,
		VolumeSize:    &volumeSize,
		VolumeCount:   1,
		Parameters:    make(map[string]interface{}),
	}

	workerTemplateReqBody := models.TemplateRequest{
		Name:          workerTemplateName,
		CloudPlatform: "AWS",
		InstanceType:  skeleton.WorkerInstanceType,
		VolumeType:    &volumeType,
		VolumeSize:    &volumeSize,
		VolumeCount:   1,
		Parameters:    make(map[string]interface{}),
	}

	log.Infof("[CreateTemplate] sending master template create request with name: %s", masterTemplateName)
	resp, err := c.Cloudbreak.Templates.PostTemplatesAccount(&templates.PostTemplatesAccountParams{&masterTemplateReqBody})

	if err != nil {
		log.Errorf("[CreateTemplate] %s", err.Error())
		newExitReturnError()
	}

	log.Infof("[CreateTemplate] master template created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID

	log.Infof("[CreateTemplate] sending worker template create request with name: %s", masterTemplateName)
	resp, err = c.Cloudbreak.Templates.PostTemplatesAccount(&templates.PostTemplatesAccountParams{&workerTemplateReqBody})

	if err != nil {
		log.Errorf("[CreateTemplate] %s", err.Error())
		newExitReturnError()
	}

	log.Infof("[CreateTemplate] worker template created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID

	wg.Done()
}
