package cloud

type CloudType string

const (
	AWS = CloudType("AWS")
)

type Network struct {
	VpcId    string `json:"VpcId" yaml:"VpcId"`
	SubnetId string `json:"SubnetId" yaml:"SubnetId"`
}

var CurrentCloud CloudType = AWS
var CloudProviders map[CloudType]CloudProvider = make(map[CloudType]CloudProvider)

type CloudProvider interface {
	GetName() *string
	CreateCredentialParameters(func(string) string, func(string) bool) map[string]interface{}
	ValidateNetwork(*Network) []error
	ValidateTags(map[string]string) []error
}

func GetProvider() CloudProvider {
	return CloudProviders[CurrentCloud]
}
