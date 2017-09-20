package cloud

type CloudType int

const (
	AWS = CloudType(iota)
)

type Network struct {
	VpcId    string `json:"VpcId" yaml:"VpcId"`
	SubnetId string `json:"SubnetId" yaml:"SubnetId"`
}

var CurrentCloud CloudType = AWS
var CloudProviders map[CloudType]CloudProvider = make(map[CloudType]CloudProvider)

type CloudProvider interface {
	ValidateNetwork(*Network) []error
	ValidateTags(map[string]string) []error
}

func GetProvider() CloudProvider {
	return CloudProviders[CurrentCloud]
}
