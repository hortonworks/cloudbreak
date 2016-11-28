package cli

import (
	"reflect"
	"regexp"
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client/networks"
	"github.com/hortonworks/hdc-cli/models"
)

func TestCreateNetworkImplCustomNetwork(t *testing.T) {
	skeleton := ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			Network: &Network{
				VpcId:    "vpcid",
				SubnetId: "subnetid",
			},
		},
	}
	c := make(chan int64, 1)
	expectedId := int64(1)
	var actual *models.NetworkRequest
	postNetwork := func(params *networks.PostNetworksAccountParams) (*networks.PostNetworksAccountOK, error) {
		actual = params.Body
		return &networks.PostNetworksAccountOK{Payload: &models.NetworkResponse{ID: &expectedId}}, nil
	}

	createNetworkImpl(skeleton, c, postNetwork, nil)

	actualId := <-c
	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
	if m, _ := regexp.MatchString("net([0-9]{10,20})", actual.Name); m == false {
		t.Errorf("name not match net([0-9]{10,20}) == %s", actual.Name)
	}
	if actual.CloudPlatform != "AWS" {
		t.Errorf("cloud platform not match AWS == %s", actual.CloudPlatform)
	}
	expectedParams := make(map[string]interface{})
	expectedParams["vpcId"] = skeleton.Network.VpcId
	expectedParams["subnetId"] = skeleton.Network.SubnetId
	if !reflect.DeepEqual(actual.Parameters, expectedParams) {
		t.Errorf("params not match %s == %s", expectedParams, actual.Parameters)
	}
}

func TestCreateNetworkImplDefaultNetwork(t *testing.T) {
	skeleton := ClusterSkeleton{}
	c := make(chan int64, 1)
	var actual *models.NetworkRequest
	postNetwork := func(params *networks.PostNetworksAccountParams) (*networks.PostNetworksAccountOK, error) {
		actual = params.Body
		id := int64(1)
		return &networks.PostNetworksAccountOK{Payload: &models.NetworkResponse{ID: &id}}, nil
	}
	expectedParams := make(map[string]interface{})
	expectedParams["vpcId"] = "vpcid"
	expectedParams["internetGatewayId"] = "internetGatewayId"
	var actualName string
	getNetwork := func(name string) models.NetworkResponse {
		actualName = name
		return models.NetworkResponse{Parameters: expectedParams}
	}

	createNetworkImpl(skeleton, c, postNetwork, getNetwork)

	expectedName := "aws-network"
	if actualName != expectedName {
		t.Errorf("name not match %s == %s", expectedName, actualName)
	}
	if !reflect.DeepEqual(actual.Parameters, expectedParams) {
		t.Errorf("params not match %s == %s", expectedParams, actual.Parameters)
	}
}

func TestCreateNetworkCommandImpl(t *testing.T) {
	finder := func(in string) string {
		switch in {
		case FlNetworkName.Name:
			return "name"
		case FlVPC.Name:
			return "vpc"
		case FlIGW.Name:
			return "igw"
		case FlSubnet.Name:
			return "subnet"
		default:
			return ""
		}
	}
	var actual *models.NetworkRequest
	postNetwork := func(params *networks.PostNetworksAccountParams) (*networks.PostNetworksAccountOK, error) {
		actual = params.Body
		id := int64(1)
		return &networks.PostNetworksAccountOK{Payload: &models.NetworkResponse{ID: &id}}, nil
	}

	createNetworkCommandImpl(finder, postNetwork)

	if actual.Name != finder(FlNetworkName.Name) {
		t.Errorf("name not match %s == %s", finder(FlNetworkName.Name), actual.Name)
	}
	if actual.CloudPlatform != "AWS" {
		t.Errorf("cloud platform not match AWS == %s", actual.CloudPlatform)
	}
	expectedParams := make(map[string]interface{})
	expectedParams["vpcId"] = finder(FlVPC.Name)
	expectedParams["internetGatewayId"] = finder(FlIGW.Name)
	if !reflect.DeepEqual(actual.Parameters, expectedParams) {
		t.Errorf("params not match %s == %s", expectedParams, actual.Parameters)
	}
	if *actual.SubnetCIDR != finder(FlSubnet.Name) {
		t.Errorf("subnet not match %s == %s", finder(FlSubnet.Name), *actual.SubnetCIDR)
	}
}

func TestListPrivateNetworksImpl(t *testing.T) {
	netwroks := make([]*models.NetworkResponse, 0)
	for i := 0; i < 3; i++ {
		id := int64(i)
		netwroks = append(netwroks, &models.NetworkResponse{
			ID:   &id,
			Name: "name" + strconv.Itoa(i),
		})
	}
	var rows []Row

	listPrivateNetworksImpl(func() []*models.NetworkResponse { return netwroks }, func(h []string, r []Row) { rows = r })

	if len(rows) != len(netwroks) {
		t.Fatalf("row number not match %d == %d", len(netwroks), len(rows))
	}

	for i, r := range rows {
		expected := []string{strconv.Itoa(i), "name" + strconv.Itoa(i)}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}
