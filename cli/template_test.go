package cli

import (
	"regexp"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client_cloudbreak/templates"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func TestCreateTemplateImpl(t *testing.T) {
	skeleton := ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			Master: InstanceConfig{
				InstanceType: "master",
				VolumeType:   "master-volume",
				VolumeSize:   &(&int32Wrapper{32}).i,
				VolumeCount:  &(&int32Wrapper{1}).i,
			},
			Worker: InstanceConfig{
				InstanceType: "worker",
				VolumeType:   "worker-volume",
				VolumeSize:   &(&int32Wrapper{64}).i,
				VolumeCount:  &(&int32Wrapper{2}).i,
			},
			Compute: SpotInstanceConfig{
				InstanceConfig: InstanceConfig{
					InstanceType: "compute",
					VolumeType:   "compute-volume",
					VolumeSize:   &(&int32Wrapper{64}).i,
					VolumeCount:  &(&int32Wrapper{2}).i,
				},
			},
		},
	}
	c := make(chan int64, 3)
	expectedMasterId := int64(1)
	expectedWorkerId := int64(2)
	expectedComputeId := int64(3)
	var actualMasterTemplate *models_cloudbreak.TemplateRequest
	var actualWorkerTemplate *models_cloudbreak.TemplateRequest
	var actualComputeTemplate *models_cloudbreak.TemplateRequest

	postTemplate := func(params *templates.PostTemplatesAccountParams) (*templates.PostTemplatesAccountOK, error) {
		var id int64
		if strings.Contains(params.Body.Name, "mtempl") {
			id = expectedMasterId
			actualMasterTemplate = params.Body
		} else if strings.Contains(params.Body.Name, "wtempl") {
			id = expectedWorkerId
			actualWorkerTemplate = params.Body
		} else {
			id = expectedComputeId
			actualComputeTemplate = params.Body
		}
		resp := templates.PostTemplatesAccountOK{
			Payload: &models_cloudbreak.TemplateResponse{ID: &id},
		}
		return &resp, nil
	}

	createTemplateImpl(skeleton, c, postTemplate)

	validateTemplate("master", skeleton.Master, expectedMasterId, actualMasterTemplate, c, t)
	validateTemplate("worker", skeleton.Worker, expectedWorkerId, actualWorkerTemplate, c, t)
	//validateTemplate("compute", skeleton.Compute, expectedComputeId, actualComputeTemplate, c, t)
}

func validateTemplate(kind string, config InstanceConfig, expId int64, actual *models_cloudbreak.TemplateRequest, c chan int64, t *testing.T) {
	id := <-c
	if id != expId {
		t.Errorf(kind+" id not match %d == %d", expId, id)
	}
	if kind == "master" {
		if m, _ := regexp.MatchString("mtempl([0-9]{10,20})", actual.Name); m == false {
			t.Errorf("name not match mtempl([0-9]{10,20}) == %s", actual.Name)
		}
	}
	if kind == "worker" {
		if m, _ := regexp.MatchString("wtempl([0-9]{10,20})", actual.Name); m == false {
			t.Errorf("name not match wtempl([0-9]{10,20}) == %s", actual.Name)
		}
	}
	if kind == "compute" {
		if m, _ := regexp.MatchString("ctempl([0-9]{10,20})", actual.Name); m == false {
			t.Errorf("name not match ctempl([0-9]{10,20}) == %s", actual.Name)
		}
	}
	if actual.CloudPlatform != "AWS" {
		t.Errorf(kind+" cloud platform not match AWS == %s", actual.CloudPlatform)
	}
	if actual.InstanceType != config.InstanceType {
		t.Errorf(kind+" instance type not match %s == %s", config.InstanceType, actual.CloudPlatform)
	}
	if *actual.VolumeType != config.VolumeType {
		t.Errorf(kind+" volume type not match %s == %s", config.VolumeType, *actual.VolumeType)
	}
	if actual.VolumeSize != config.VolumeSize {
		t.Errorf(kind+" volume size not match %d == %d", config.VolumeSize, actual.VolumeSize)
	}
	if actual.VolumeCount != config.VolumeCount {
		t.Errorf(kind+" volume count not match %d == %d", config.VolumeCount, actual.VolumeCount)
	}
}
