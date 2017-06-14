package cli

import (
	"strconv"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/networks"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var NetworkHeader []string = []string{"ID", "Name"}

type NetworkList struct {
	Id   int64  `json:"Id" yaml:"Id"`
	Name string `json:"Name" yaml:"Name"`
}

func (n *NetworkList) DataAsStringArray() []string {
	return []string{strconv.FormatInt(n.Id, 10), n.Name}
}

func (c *Cloudbreak) CreateNetwork(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defer timeTrack(time.Now(), "create network")
	defer wg.Done()

	createNetworkImpl(skeleton, channel, c.Cloudbreak.Networks.PostPublicNetwork, c.GetNetwork)
}

func createNetworkImpl(skeleton ClusterSkeleton, channel chan int64,
	postNetwork func(*networks.PostPublicNetworkParams) (*networks.PostPublicNetworkOK, error),
	getNetwork func(string) models_cloudbreak.NetworkResponse) {

	network := createNetworkRequest(skeleton, getNetwork)

	log.Infof("[CreateNetwork] sending network create request with name: %s", network.Name)
	resp, err := postNetwork(&networks.PostPublicNetworkParams{Body: network})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateNetwork] network created, id: %d", resp.Payload.ID)
	channel <- *resp.Payload.ID
}

func createNetworkRequest(skeleton ClusterSkeleton, getNetwork func(string) models_cloudbreak.NetworkResponse) *models_cloudbreak.NetworkRequest {
	networkName := "net" + strconv.FormatInt(time.Now().UnixNano(), 10)

	vpc := skeleton.Network
	var vpcParams = make(map[string]interface{})
	if vpc != nil && len(vpc.VpcId) > 0 && len(vpc.SubnetId) > 0 {
		vpcParams["vpcId"] = vpc.VpcId
		vpcParams["subnetId"] = vpc.SubnetId
	} else {
		defaultNetwork := getNetwork("aws-network")
		vpcParams["vpcId"] = defaultNetwork.Parameters["vpcId"]
		vpcParams["internetGatewayId"] = defaultNetwork.Parameters["internetGatewayId"]
	}

	network := models_cloudbreak.NetworkRequest{
		Name:          networkName,
		CloudPlatform: "AWS",
		Parameters:    vpcParams,
	}

	return &network
}

func CreateNetworkCommand(c *cli.Context) error {
	checkRequiredFlags(c)
	defer timeTrack(time.Now(), "create network")

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	return createNetworkCommandImpl(c.String, oAuth2Client.Cloudbreak.Networks.PostPublicNetwork)
}

func createNetworkCommandImpl(finder func(string) string, postNetwork func(*networks.PostPublicNetworkParams) (*networks.PostPublicNetworkOK, error)) error {
	networkName := finder(FlNetworkName.Name)
	log.Infof("[CreateNetworkCommand] create network with name: %s", networkName)

	var vpcParams = make(map[string]interface{})
	vpcParams["vpcId"] = finder(FlVPC.Name)
	igw := finder(FlIGW.Name)
	if len(igw) > 0 {
		vpcParams["internetGatewayId"] = igw
	}
	subnet := finder(FlSubnet.Name)
	if len(subnet) > 0 {
		vpcParams["subnetId"] = subnet
	}
	network := models_cloudbreak.NetworkRequest{
		Name:          networkName,
		CloudPlatform: "AWS",
		Parameters:    vpcParams,
	}
	subnetCidr := finder(FlSubnetCidr.Name)
	if len(subnetCidr) > 0 {
		network.SubnetCIDR = &subnetCidr
	}

	resp, err := postNetwork(&networks.PostPublicNetworkParams{Body: &network})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateNetworkCommand] network created, id: %d", resp.Payload.ID)
	return nil
}

func (c *Cloudbreak) GetNetwork(name string) models_cloudbreak.NetworkResponse {
	log.Infof("[GetNetwork] sending get request to find network with name: %s", name)
	resp, err := c.Cloudbreak.Networks.GetPublicNetwork(&networks.GetPublicNetworkParams{Name: name})

	if err != nil {
		logErrorAndExit(err)
	}

	defaultNetwork := *resp.Payload
	log.Infof("[GetNetwork] found network, name: %s", defaultNetwork.Name)
	return defaultNetwork
}

func (c *Cloudbreak) GetNetworkById(id int64) *models_cloudbreak.NetworkResponse {
	log.Infof("[GetNetwork] sending get request to find network with id: %d", id)
	resp, err := c.Cloudbreak.Networks.GetNetwork(&networks.GetNetworkParams{ID: id})

	if err != nil {
		logErrorAndExit(err)
	}

	network := resp.Payload
	log.Infof("[GetNetwork] found network, name: %s", network.Name)
	return network
}

func DeleteNetwork(c *cli.Context) error {
	checkRequiredFlags(c)
	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	if err := oAuth2Client.DeleteNetwork(c.String(FlNetworkName.Name)); err != nil {
		logErrorAndExit(err)
	}
	return nil
}

func (c *Cloudbreak) GetPublicNetworks() []*models_cloudbreak.NetworkResponse {
	defer timeTrack(time.Now(), "get public networks")
	resp, err := c.Cloudbreak.Networks.GetPublicsNetwork(&networks.GetPublicsNetworkParams{})
	if err != nil {
		logErrorAndExit(err)
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteNetwork(name string) error {
	defer timeTrack(time.Now(), "delete network")
	log.Infof("[DeleteNetwork] delete network: %s", name)
	return c.Cloudbreak.Networks.DeletePublicNetwork(&networks.DeletePublicNetworkParams{Name: name})
}

func ListPrivateNetworks(c *cli.Context) error {
	checkRequiredFlags(c)
	defer timeTrack(time.Now(), "list the private networks")

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	output := Output{Format: c.String(FlOutput.Name)}
	return listPrivateNetworksImpl(oAuth2Client.GetPrivateNetworks, output.WriteList)
}

func listPrivateNetworksImpl(getNetworks func() []*models_cloudbreak.NetworkResponse, writer func([]string, []Row)) error {
	networkResp := getNetworks()

	var tableRows []Row
	for _, net := range networkResp {
		row := &NetworkList{Id: *net.ID, Name: net.Name}
		tableRows = append(tableRows, row)
	}

	writer(NetworkHeader, tableRows)
	return nil
}

func (c *Cloudbreak) GetPrivateNetworks() []*models_cloudbreak.NetworkResponse {
	defer timeTrack(time.Now(), "get private networks")
	resp, err := c.Cloudbreak.Networks.GetPrivatesNetwork(&networks.GetPrivatesNetworkParams{})
	if err != nil {
		logErrorAndExit(err)
	}
	return resp.Payload
}
