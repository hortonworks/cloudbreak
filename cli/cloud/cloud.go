package cloud

type CloudType string

const (
	AWS       = CloudType("AWS")
	AZURE     = CloudType("AZURE")
	GCP       = CloudType("GCP")
	OPENSTACK = CloudType("OPENSTACK")
)

type NetworkMode int

const (
	NEW_NETWORK_NEW_SUBNET NetworkMode = iota
	EXISTING_NETWORK_NEW_SUBNET
	EXISTING_NETWORK_EXISTING_SUBNET
	LEGACY_NETWORK
)

var currentCloud CloudType

func SetProviderType(ct CloudType) {
	currentCloud = ct
}

var CloudProviders map[CloudType]CloudProvider = make(map[CloudType]CloudProvider)

type CloudProvider interface {
	GetName() *string
	CreateCredentialParameters(func(string) string, func(string) bool) (map[string]interface{}, error)
	GetNetworkParamatersTemplate(NetworkMode) map[string]interface{}
	// ValidateNetwork(*Network) []error
	// ValidateTags(map[string]string) []error
}

func GetProvider() CloudProvider {
	return CloudProviders[currentCloud]
}
