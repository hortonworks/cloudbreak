package cli

import (
	"strconv"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/templates"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func (c *Cloudbreak) CreateTemplate(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defer timeTrack(time.Now(), "create template")
	defer wg.Done()

	createTemplateImpl(skeleton, channel, c.Cloudbreak.Templates.PostPublicTemplate)
}

func createTemplateImpl(skeleton ClusterSkeleton, channel chan int64,
	postTemplate func(*templates.PostPublicTemplateParams) (*templates.PostPublicTemplateOK, error)) {
	masterTemplateReqBody := createMasterTemplateRequest(skeleton)
	workerTemplateReqBody := createWorkerTemplateRequest(skeleton)
	computeTemplateReqBody := createComputeTemplateRequest(skeleton)

	log.Infof("[CreateTemplate] sending master template create request with name: %s", masterTemplateReqBody.Name)
	resp, err := postTemplate(templates.NewPostPublicTemplateParams().WithBody(masterTemplateReqBody))

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateTemplate] master template created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID

	log.Infof("[CreateTemplate] sending worker template create request with name: %s", workerTemplateReqBody.Name)
	resp, err = postTemplate(templates.NewPostPublicTemplateParams().WithBody(workerTemplateReqBody))

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateTemplate] worker template created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID

	log.Infof("[CreateTemplate] sending compute template create request with name: %s", computeTemplateReqBody.Name)
	resp, err = postTemplate(templates.NewPostPublicTemplateParams().WithBody(computeTemplateReqBody))

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateTemplate] compute template created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID
}

func createMasterTemplateRequest(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest {
	masterTemplateName := "mtempl" + strconv.FormatInt(time.Now().UnixNano(), 10)

	parameters := make(map[string]interface{})
	encrypt := skeleton.Master.Encrypted
	if encrypt != nil {
		parameters[ENCRYPTED] = *encrypt
	}

	masterTemplateReqBody := models_cloudbreak.TemplateRequest{
		Name:          &masterTemplateName,
		CloudPlatform: &(&stringWrapper{"AWS"}).s,
		InstanceType:  &skeleton.Master.InstanceType,
		VolumeType:    skeleton.Master.VolumeType,
		VolumeSize:    *skeleton.Master.VolumeSize,
		VolumeCount:   *skeleton.Master.VolumeCount,
		Parameters:    parameters,
	}

	return &masterTemplateReqBody
}

func createWorkerTemplateRequest(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest {
	workerTemplateName := "wtempl" + strconv.FormatInt(time.Now().UnixNano(), 10)

	parameters := make(map[string]interface{})
	encrypt := skeleton.Worker.Encrypted
	if encrypt != nil {
		parameters[ENCRYPTED] = *encrypt
	}

	workerTemplateReqBody := models_cloudbreak.TemplateRequest{
		Name:          &workerTemplateName,
		CloudPlatform: &(&stringWrapper{"AWS"}).s,
		InstanceType:  &skeleton.Worker.InstanceType,
		VolumeType:    skeleton.Worker.VolumeType,
		VolumeSize:    *skeleton.Worker.VolumeSize,
		VolumeCount:   *skeleton.Worker.VolumeCount,
		Parameters:    parameters,
	}

	return &workerTemplateReqBody
}

func createComputeTemplateRequest(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest {
	computeTemplateName := "ctempl" + strconv.FormatInt(time.Now().UnixNano(), 10)

	parameters := make(map[string]interface{})
	if skeleton.Compute.SpotPrice != "" {
		floatPrice, _ := strconv.ParseFloat(skeleton.Compute.SpotPrice, 64)
		parameters["spotPrice"] = floatPrice
	}
	encrypt := skeleton.Compute.Encrypted
	if encrypt != nil {
		parameters[ENCRYPTED] = *encrypt
	}

	computeTemplateReqBody := models_cloudbreak.TemplateRequest{
		Name:          &computeTemplateName,
		CloudPlatform: &(&stringWrapper{"AWS"}).s,
		InstanceType:  &skeleton.Compute.InstanceType,
		VolumeType:    skeleton.Compute.VolumeType,
		VolumeSize:    *skeleton.Compute.VolumeSize,
		VolumeCount:   *skeleton.Compute.VolumeCount,
		Parameters:    parameters,
	}

	return &computeTemplateReqBody
}

func (c *Cloudbreak) GetPublicTemplates() []*models_cloudbreak.TemplateResponse {
	defer timeTrack(time.Now(), "get public templates")
	resp, err := c.Cloudbreak.Templates.GetPublicsTemplate(templates.NewGetPublicsTemplateParams())
	if err != nil {
		logErrorAndExit(err)
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteTemplate(name string) error {
	defer timeTrack(time.Now(), "delete template")
	log.Infof("[DeleteTemplate] delete template: %s", name)
	return c.Cloudbreak.Templates.DeletePublicTemplate(templates.NewDeletePublicTemplateParams().WithName(name))
}
