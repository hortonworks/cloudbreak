package cloud

type CloudType string

const (
	AWS       = CloudType("AWS")
	GCP       = CloudType("GCP")
	OPENSTACK = CloudType("OPENSTACK")
)

type Network struct {
	VpcId    string `json:"VpcId" yaml:"VpcId"`
	SubnetId string `json:"SubnetId" yaml:"SubnetId"`
}

var CurrentCloud CloudType
var CloudProviders map[CloudType]CloudProvider = make(map[CloudType]CloudProvider)

type CloudProvider interface {
	GetName() *string
	CreateCredentialParameters(func(string) string, func(string) bool) (map[string]interface{}, error)
	// ValidateNetwork(*Network) []error
	// ValidateTags(map[string]string) []error
}

func GetProvider() CloudProvider {
	return CloudProviders[CurrentCloud]
}
