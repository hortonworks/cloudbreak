package cli

import (
	"strconv"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/templates"
	"github.com/hortonworks/hdc-cli/models"
)

func (c *Cloudbreak) CreateTemplate(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defer timeTrack(time.Now(), "create template")
	defer wg.Done()

	createTemplateImpl(skeleton, channel, c.Cloudbreak.Templates.PostTemplatesAccount)
}

func createTemplateImpl(skeleton ClusterSkeleton, channel chan int64, postTemplate func(*templates.PostTemplatesAccountParams) (*templates.PostTemplatesAccountOK, error)) {
	masterTemplateName := "mtempl" + strconv.FormatInt(time.Now().UnixNano(), 10)
	workerTemplateName := "wtempl" + strconv.FormatInt(time.Now().UnixNano(), 10)
	computeTemplateName := "ctempl" + strconv.FormatInt(time.Now().UnixNano(), 10)

	masterTemplateReqBody := models.TemplateRequest{
		Name:          masterTemplateName,
		CloudPlatform: "AWS",
		InstanceType:  skeleton.Master.InstanceType,
		VolumeType:    &skeleton.Master.VolumeType,
		VolumeSize:    skeleton.Master.VolumeSize,
		VolumeCount:   skeleton.Master.VolumeCount,
		Parameters:    make(map[string]interface{}),
	}

	workerTemplateReqBody := models.TemplateRequest{
		Name:          workerTemplateName,
		CloudPlatform: "AWS",
		InstanceType:  skeleton.Worker.InstanceType,
		VolumeType:    &skeleton.Worker.VolumeType,
		VolumeSize:    skeleton.Worker.VolumeSize,
		VolumeCount:   skeleton.Worker.VolumeCount,
		Parameters:    make(map[string]interface{}),
	}

	computeParameters := make(map[string]interface{})
	if skeleton.Compute.SpotPrice != "" {
		computeParameters["spotPrice"] = skeleton.Compute.SpotPrice
	}

	computeTemplateReqBody := models.TemplateRequest{
		Name:          computeTemplateName,
		CloudPlatform: "AWS",
		InstanceType:  skeleton.Compute.InstanceType,
		VolumeType:    &skeleton.Compute.VolumeType,
		VolumeSize:    skeleton.Compute.VolumeSize,
		VolumeCount:   skeleton.Compute.VolumeCount,
		Parameters:    computeParameters,
	}

	log.Infof("[CreateTemplate] sending master template create request with name: %s", masterTemplateName)
	resp, err := postTemplate(&templates.PostTemplatesAccountParams{Body: &masterTemplateReqBody})

	if err != nil {
		logErrorAndExit(createTemplateImpl, err.Error())
	}

	log.Infof("[CreateTemplate] master template created, id: %d", resp.Payload.ID)
	channel <- *resp.Payload.ID

	log.Infof("[CreateTemplate] sending worker template create request with name: %s", workerTemplateName)
	resp, err = postTemplate(&templates.PostTemplatesAccountParams{Body: &workerTemplateReqBody})

	if err != nil {
		logErrorAndExit(createTemplateImpl, err.Error())
	}

	log.Infof("[CreateTemplate] worker template created, id: %d", resp.Payload.ID)
	channel <- *resp.Payload.ID

	log.Infof("[CreateTemplate] sending compute template create request with name: %s", computeTemplateName)
	resp, err = postTemplate(&templates.PostTemplatesAccountParams{Body: &computeTemplateReqBody})

	if err != nil {
		logErrorAndExit(createTemplateImpl, err.Error())
	}

	log.Infof("[CreateTemplate] compute template created, id: %d", resp.Payload.ID)
	channel <- *resp.Payload.ID
}

func (c *Cloudbreak) GetPublicTemplates() []*models.TemplateResponse {
	defer timeTrack(time.Now(), "get public templates")
	resp, err := c.Cloudbreak.Templates.GetTemplatesAccount(&templates.GetTemplatesAccountParams{})
	if err != nil {
		logErrorAndExit(c.GetPublicTemplates, err.Error())
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteTemplate(name string) error {
	defer timeTrack(time.Now(), "delete template")
	log.Infof("[DeleteTemplate] delete template: %s", name)
	return c.Cloudbreak.Templates.DeleteTemplatesAccountName(&templates.DeleteTemplatesAccountNameParams{Name: name})
}
