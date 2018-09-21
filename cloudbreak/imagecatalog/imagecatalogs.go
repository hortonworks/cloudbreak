package imagecatalog

import (
	"fmt"
	"github.com/hortonworks/cb-cli/cloudbreak/oauth"
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v3_workspace_id_imagecatalogs"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/cloud"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/urfave/cli"
)

var imagecatalogHeader = []string{"Name", "Default", "URL"}

type imagecatalogOut struct {
	Name    string `json:"Name" yaml:"Name"`
	Default bool   `json:"Default" yaml:"Default"`
	URL     string `json:"URL" yaml:"URL"`
}

type imagecatalogOutDescribe struct {
	*imagecatalogOut
	ID string `json:"ID" yaml:"ID"`
}

func (r *imagecatalogOut) DataAsStringArray() []string {
	return []string{r.Name, strconv.FormatBool(r.Default), r.URL}
}

func (b *imagecatalogOutDescribe) DataAsStringArray() []string {
	return append(b.imagecatalogOut.DataAsStringArray(), b.ID)
}

var imageHeader = []string{"Date", "Description", "Version", "ImageID"}

type imageOut struct {
	Date        string `json:"Date" yaml:"Date"`
	Description string `json:"Description" yaml:"Description"`
	Version     string `json:"Version" yaml:"Version"`
	ImageID     string `json:"ImageID" yaml:"ImageID"`
}

func (r *imageOut) DataAsStringArray() []string {
	return []string{r.Date, r.Description, r.Version, r.ImageID}
}

var imageDetailsHeader = []string{"Date", "Description", "Ambari Version", "ImageID", "OS", "OS Type", "Images", "Package Versions"}

type imageDetailsOut struct {
	Date            string                       `json:"Date" yaml:"Date"`
	Description     string                       `json:"Description" yaml:"Description"`
	Version         string                       `json:"Version" yaml:"Version"`
	ImageID         string                       `json:"ImageID" yaml:"ImageID"`
	Os              string                       `json:"OS" yaml:"OS"`
	OsType          string                       `json:"OSType" yaml:"OSType"`
	Images          map[string]map[string]string `json:"Images" yaml:"Images"`
	PackageVersions map[string]string            `json:"PackageVersions" yaml:"PackageVersions"`
}

func (r *imageDetailsOut) DataAsStringArray() []string {
	var images string
	for prov, imgs := range r.Images {
		images += fmt.Sprintf("%s:\n", prov)
		for region, image := range imgs {
			images += fmt.Sprintf("  %s: %s\n", region, image)
		}
	}

	var packageVersions string
	for pkg, ver := range r.PackageVersions {
		packageVersions += fmt.Sprintf("%s: %s\n", pkg, ver)
	}

	return []string{r.Date, r.Description, r.Version, r.ImageID, r.Os, r.OsType, images, packageVersions}
}

func CreateImagecatalogFromUrl(c *cli.Context) {

	log.Infof("[CreateImagecatalogFromUrl] creating imagecatalog from a URL")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createImagecatalogImpl(
		cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlURL.Name))
}

type imagecatalogClient interface {
	CreateImageCatalogInWorkspace(params *v3_workspace_id_imagecatalogs.CreateImageCatalogInWorkspaceParams) (*v3_workspace_id_imagecatalogs.CreateImageCatalogInWorkspaceOK, error)
}

func createImagecatalogImpl(client imagecatalogClient, workspaceID int64, name string, imagecatalogURL string) {
	defer utils.TimeTrack(time.Now(), "create imagecatalog")
	imagecatalogRequest := &model.ImageCatalogRequest{
		Name: &name,
		URL:  &imagecatalogURL,
	}
	var ic *model.ImageCatalogResponse
	log.Infof("[createImagecatalogImpl] sending create imagecatalog request")
	resp, err := client.CreateImageCatalogInWorkspace(v3_workspace_id_imagecatalogs.NewCreateImageCatalogInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(imagecatalogRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	ic = resp.Payload
	log.Infof("[createImagecatalogImpl] imagecatalog created: %s (id: %d)", *ic.Name, ic.ID)
}

func ListImagecatalogs(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list imagecatalogs")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	listImagecatalogsImpl(cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs, workspaceID, output.WriteList)
}

type listImageCatalogsByWorkspaceClient interface {
	ListImageCatalogsByWorkspace(*v3_workspace_id_imagecatalogs.ListImageCatalogsByWorkspaceParams) (*v3_workspace_id_imagecatalogs.ListImageCatalogsByWorkspaceOK, error)
}

func listImagecatalogsImpl(client listImageCatalogsByWorkspaceClient, workspaceID int64, writer func([]string, []utils.Row)) {
	log.Infof("[listImagecatalogsImpl] sending imagecatalog list request")
	imagecatalogResp, err := client.ListImageCatalogsByWorkspace(v3_workspace_id_imagecatalogs.NewListImageCatalogsByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, ic := range imagecatalogResp.Payload {
		tableRows = append(tableRows, &imagecatalogOut{*ic.Name, ic.UsedAsDefault, *ic.URL})
	}

	writer(imagecatalogHeader, tableRows)
}

func DeleteImagecatalog(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete imagecatalog")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	name := c.String(fl.FlName.Name)
	log.Infof("[DeleteImagecatalog] sending delete imagecatalog request with name: %s", name)
	if _, err := cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs.DeleteImageCatalogInWorkspace(v3_workspace_id_imagecatalogs.NewDeleteImageCatalogInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteImagecatalog] imagecatalog deleted, name: %s", name)
}

func SetDefaultImagecatalog(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "set default imagecatalog")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	name := c.String(fl.FlName.Name)
	log.Infof("[SetDefautlImagecatalog] sending set default imagecatalog request with name: %s", name)

	if _, err := cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs.PutSetDefaultImageCatalogByNameInWorkspace(v3_workspace_id_imagecatalogs.NewPutSetDefaultImageCatalogByNameInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[SetDefaultImagecatalog] imagecatalog is set as default, name: %s", name)
}

func DescribeImagecatalog(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe imagecatalog")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs.GetImageCatalogInWorkspace(v3_workspace_id_imagecatalogs.NewGetImageCatalogInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(c.String(fl.FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	imgc := resp.Payload
	if imgc.ID == nil {
		output.Write(imagecatalogHeader, &imagecatalogOut{*imgc.Name, imgc.UsedAsDefault, *imgc.URL})
	} else {
		output.Write(append(imagecatalogHeader, "ID"), &imagecatalogOutDescribe{&imagecatalogOut{*imgc.Name, imgc.UsedAsDefault, *imgc.URL}, strconv.FormatInt(*imgc.ID, 10)})
	}
}

func ListAwsImages(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	listImages(c)
}

func ListAzureImages(c *cli.Context) {
	cloud.SetProviderType(cloud.AZURE)
	listImages(c)
}

func ListGcpImages(c *cli.Context) {
	cloud.SetProviderType(cloud.GCP)
	listImages(c)
}

func ListOpenstackImages(c *cli.Context) {
	cloud.SetProviderType(cloud.OPENSTACK)
	listImages(c)
}

func listImages(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list available images")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	listImagesImpl(cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs, output.WriteList, c.Int64(fl.FlWorkspaceOptional.Name), c.String(fl.FlImageCatalog.Name))
}

func ListImagesValidForUpgrade(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list images valid for stack upgrade")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	clusterName := c.String(fl.FlClusterToUpgrade.Name)
	imageCatalogName := c.String(fl.FlImageCatalogOptional.Name)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	if imageCatalogName != "" {
		log.Infof("[ListImagesValidForUpgrade] sending list images request, stack: %s, catalog: %s", clusterName, imageCatalogName)
		imageResp, err := cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs.GetImagesByStackNameAndCustomImageCatalogInWorkspace(v3_workspace_id_imagecatalogs.NewGetImagesByStackNameAndCustomImageCatalogInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(imageCatalogName).WithStackName(clusterName))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		writeImageListInformation(output.WriteList, toImageResponseList(imageResp.Payload))
	} else {
		log.Infof("[ListImagesValidForUpgrade] sending list images request using the default catalog, stack: %s", clusterName)
		imageResp, err := cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs.GetImagesByStackNameAndDefaultImageCatalogInWorkspace(v3_workspace_id_imagecatalogs.NewGetImagesByStackNameAndDefaultImageCatalogInWorkspaceParams().WithWorkspaceID(workspaceID).WithStackName(clusterName))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		writeImageListInformation(output.WriteList, toImageResponseList(imageResp.Payload))
	}
}

func toImageResponseList(images *model.ImagesResponse) []*model.ImageResponse {
	var imagesList = make([]*model.ImageResponse, 0, len(images.BaseImages)+len(images.HdfImages)+len(images.HdpImages))
	for _, i := range images.BaseImages {
		imagesList = append(imagesList, toImageResponse(i))
	}
	for _, i := range images.HdfImages {
		imagesList = append(imagesList, i)
	}
	for _, i := range images.HdpImages {
		imagesList = append(imagesList, i)
	}

	return imagesList
}

func DescribeAwsImage(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	describeImage(c)
}

func DescribeAzureImage(c *cli.Context) {
	cloud.SetProviderType(cloud.AZURE)
	describeImage(c)
}

func DescribeGcpImage(c *cli.Context) {
	cloud.SetProviderType(cloud.GCP)
	describeImage(c)
}

func DescribeOpenstackImage(c *cli.Context) {
	cloud.SetProviderType(cloud.OPENSTACK)
	describeImage(c)
}

func describeImage(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe image")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	describeImageImpl(cbClient.Cloudbreak.V3WorkspaceIDImagecatalogs, output.WriteList, c.Int64(fl.FlWorkspaceOptional.Name), c.String(fl.FlImageCatalog.Name), c.String(fl.FlImageId.Name))
}

func describeImageImpl(client getPublicImagesClient, writer func([]string, []utils.Row), workspaceID int64, imagecatalog string, imageid string) {
	log.Infof("[listImagesImpl] sending list images request")
	provider := cloud.GetProvider().GetName()
	imageResp, err := client.GetImagesByProviderAndCustomImageCatalogInWorkspace(v3_workspace_id_imagecatalogs.NewGetImagesByProviderAndCustomImageCatalogInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(imagecatalog).WithPlatform(*provider))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	image := findImageByUUID(imageResp.Payload, imageid)
	if image == nil {
		utils.LogErrorMessage(fmt.Sprintf("Image not found by id: %s for cloud: %s", imageid, *provider))
	}

	writeImageInformation(writer, image)
}

func findImageByUUID(imageResponse *model.ImagesResponse, imageID string) *model.ImageResponse {
	image := findBaseImage(imageResponse.BaseImages, imageID)
	if image == nil {
		warmupImage := findWarmupImage(imageResponse.HdpImages, imageID)
		if warmupImage == nil {
			warmupImage = findWarmupImage(imageResponse.HdfImages, imageID)
		}
		return warmupImage
	}
	return toImageResponse(image)
}

func toImageResponse(image *model.BaseImageResponse) *model.ImageResponse {
	return &model.ImageResponse{
		Date:            image.Date,
		Description:     image.Description,
		Version:         "",
		UUID:            image.UUID,
		Os:              image.Os,
		OsType:          image.OsType,
		Images:          image.Images,
		PackageVersions: image.PackageVersions,
	}
}

func findBaseImage(images []*model.BaseImageResponse, imageID string) *model.BaseImageResponse {
	for _, i := range images {
		if i.UUID == imageID {
			return i
		}
	}
	return nil
}

func findWarmupImage(images []*model.ImageResponse, imageID string) *model.ImageResponse {
	for _, i := range images {
		if i.UUID == imageID {
			return i
		}
	}
	return nil
}

func writeImageInformation(writer func([]string, []utils.Row), image *model.ImageResponse) {
	tableRows := []utils.Row{}
	tableRows = append(tableRows, &imageDetailsOut{image.Date, image.Description, image.Version, image.UUID, image.Os, image.OsType, image.Images, image.PackageVersions})
	writer(imageDetailsHeader, tableRows)
}

func writeImageListInformation(writer func([]string, []utils.Row), payload []*model.ImageResponse) {
	tableRows := []utils.Row{}

	for _, i := range payload {
		tableRows = append(tableRows, &imageDetailsOut{i.Date, i.Description, i.Version, i.UUID, i.Os, i.OsType, i.Images, i.PackageVersions})
	}
	writer(imageDetailsHeader, tableRows)
}

type getPublicImagesClient interface {
	GetImagesByProviderAndCustomImageCatalogInWorkspace(*v3_workspace_id_imagecatalogs.GetImagesByProviderAndCustomImageCatalogInWorkspaceParams) (*v3_workspace_id_imagecatalogs.GetImagesByProviderAndCustomImageCatalogInWorkspaceOK, error)
}

func listImagesImpl(client getPublicImagesClient, writer func([]string, []utils.Row), workspaceID int64, imagecatalog string) {
	log.Infof("[listImagesImpl] sending list images request")
	provider := cloud.GetProvider().GetName()
	imageResp, err := client.GetImagesByProviderAndCustomImageCatalogInWorkspace(v3_workspace_id_imagecatalogs.NewGetImagesByProviderAndCustomImageCatalogInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(imagecatalog).WithPlatform(*provider))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, i := range imageResp.Payload.BaseImages {
		tableRows = append(tableRows, &imageOut{i.Date, i.Description, i.Version, i.UUID})
	}

	writer(imageHeader, tableRows)

}
