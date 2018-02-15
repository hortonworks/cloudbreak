package cli

import (
	"strings"
	"testing"

	"github.com/hortonworks/cb-cli/cli/cloud"
	_ "github.com/hortonworks/cb-cli/cli/cloud/aws"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1imagecatalogs"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type mockGetPublicsImagecatalogsClient struct {
}

func (*mockGetPublicsImagecatalogsClient) GetPublicsImageCatalogs(params *v1imagecatalogs.GetPublicsImageCatalogsParams) (*v1imagecatalogs.GetPublicsImageCatalogsOK, error) {
	resp := []*models_cloudbreak.ImageCatalogResponse{
		&models_cloudbreak.ImageCatalogResponse{
			Name:          &(&types.S{S: "test"}).S,
			UsedAsDefault: (&types.B{B: true}).B,
			URL:           &(&types.S{S: "testurl"}).S,
		},
	}
	return &v1imagecatalogs.GetPublicsImageCatalogsOK{Payload: resp}, nil
}

func TestListImagecatalogsImpl(t *testing.T) {
	var rows []utils.Row
	listImagecatalogsImpl(new(mockGetPublicsImagecatalogsClient), func(h []string, r []utils.Row) { rows = r })
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "test true testurl"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}

type mockGetPublicImagesClient struct {
}

func (*mockGetPublicImagesClient) GetPublicImagesByProviderAndCustomImageCatalog(params *v1imagecatalogs.GetPublicImagesByProviderAndCustomImageCatalogParams) (*v1imagecatalogs.GetPublicImagesByProviderAndCustomImageCatalogOK, error) {
	resp := &models_cloudbreak.ImagesResponse{
		BaseImages: []*models_cloudbreak.BaseImageResponse{
			&models_cloudbreak.BaseImageResponse{
				Date:        "1111-11-11",
				Version:     "1.1.1",
				Description: "images",
				UUID:        "uuid",
			},
		},
	}
	return &v1imagecatalogs.GetPublicImagesByProviderAndCustomImageCatalogOK{Payload: resp}, nil
}

func TestListImagesImpl(t *testing.T) {
	var rows []utils.Row
	cloud.SetProviderType(cloud.AWS)
	listImagesImpl(new(mockGetPublicImagesClient), func(h []string, r []utils.Row) { rows = r }, "catalog")
	if len(rows) != 1 {
		t.Fatalf("row number doesn't match 1 == %d", len(rows))
	}
	for _, r := range rows {
		expected := "1111-11-11 images 1.1.1 uuid"
		if strings.Join(r.DataAsStringArray(), " ") != expected {
			t.Errorf("row data not match %s == %s", expected, strings.Join(r.DataAsStringArray(), " "))
		}
	}
}
