package freeipa

var header = []string{"FreeIPA", "ID"}

type freeIpaOutDescibe struct {
	*freeIpa
	ID string `json:"ID" yaml:"ID"`
}

type freeIpa struct {
	EnvironmentCrn string          `json:"EnvironmentID" yaml:"EnvironmentID"`
	Name           string          `json:"Name" yaml:"Name"`
	Placement      *placement      `json:"Placement" yaml:"Placement"`
	InstanceGroups []instanceGroup `json:"InstanceGroups" yaml:"InstanceGroups"`
	Authentication *authentication `json:"Authentication" yaml:"Authentication"`
	Network        *network        `json:"Network,omitempty" yaml:"Network,omitempty"`
	Image          *image          `json:"Image,omitempty" yaml:"Image,omitempty"`
	FreeIpaServer  *freeIpaServer  `json:"FreeIpaServer" yaml:"FreeIpaServer"`
}

type placement struct {
	AvailabilityZone string `json:"AvailabilityZone,omitempty" yaml:"AvailabilityZone,omitempty"`
	Region           string `json:"Region" yaml:"Region"`
}

type instanceGroup struct {
	InstanceTemplate *instanceTemplate `json:"InstanceTemplate" yaml:"InstanceTemplate"`
	SecurityGroup    *securityGroup    `json:"SecurityGroup,omitempty" yaml:"SecurityGroup,omitempty"`
	Name             string            `json:"Name" yaml:"Name"`
	NodeCount        string            `json:"NodeCount" yaml:"NodeCount"`
	MetaData         []metadata        `json:"MetaData" yaml:"MetaData"`
}

type instanceTemplate struct {
	InstanceType    string   `json:"InstanceType" yaml:"InstanceType"`
	AttachedVolumes []volume `json:"AttachedVolumes" yaml:"AttachedVolumes"`
}

type metadata struct {
	DiscoveryFQDN  string `json:"DiscoveryFQDN,omitempty" yaml:"DiscoveryFQDN,omitempty"`
	InstanceGroup  string `json:"InstanceGroup,omitempty" yaml:"InstanceGroup,omitempty"`
	InstanceID     string `json:"InstanceID,omitempty" yaml:"InstanceID,omitempty"`
	InstanceStatus string `json:"InstanceStatus,omitempty" yaml:"InstanceStatus,omitempty"`
	InstanceType   string `json:"InstanceType,omitempty" yaml:"InstanceType,omitempty"`
	PrivateIP      string `json:"PrivateIP,omitempty" yaml:"PrivateIP,omitempty"`
	PublicIP       string `json:"PublicIP,omitempty" yaml:"PublicIP,omitempty"`
	SSHPort        string `json:"SSHPort,omitempty" yaml:"SSHPort,omitempty"`
	State          string `json:"State,omitempty" yaml:"State,omitempty"`
}

type volume struct {
	Count      string `json:"Count" yaml:"Count"`
	VolumeType string `json:"VolumeType" yaml:"VolumeType"`
	Size       string `json:"Size" yaml:"Size"`
}

type securityGroup struct {
	SecurityGroupIDs []string       `json:"SecurityGroupIDs" yaml:"SecurityGroupIDs"`
	SecurityRules    []securityRule `json:"SecurityRules" yaml:"SecurityRules"`
}

type securityRule struct {
	Subnet     string   `json:"Subnet" yaml:"Subnet"`
	Ports      []string `json:"Ports" yaml:"Ports"`
	Protocol   string   `json:"Protocol" yaml:"Protocol"`
	Modifiable string   `json:"Modifiable" yaml:"Modifiable"`
}

type authentication struct {
	PublicKey     string `json:"PublicKey,omitempty" yaml:"PublicKey,omitempty"`
	PublicKeyID   string `json:"PublicKeyID,omitempty" yaml:"PublicKeyID,omitempty"`
	LoginUserName string `json:"LoginUserName,omitempty" yaml:"LoginUserName,omitempty"`
}

type network struct {
	Aws   *awsNetwork   `json:"Aws,omitempty" yaml:"Aws,omitempty"`
	Azure *azureNetwork `json:"Azure,omitempty" yaml:"Azure,omitempty"`
}

type awsNetwork struct {
	SubnetID string `json:"SubnetID,omitempty" yaml:"SubnetID,omitempty"`
	VpcID    string `json:"VpcID,omitempty" yaml:"VpcID,omitempty"`
}

type azureNetwork struct {
	NetworkID         string `json:"NetworkID,omitempty" yaml:"NetworkID,omitempty"`
	NoFirewallRules   string `json:"NoFirewallRules,omitempty" yaml:"NoFirewallRules,omitempty"`
	NoPublicIP        string `json:"NoPublicIP,omitempty" yaml:"NoPublicIP,omitempty"`
	ResourceGroupName string `json:"ResourceGroupName,omitempty" yaml:"ResourceGroupName,omitempty"`
	SubnetID          string `json:"SubnetID,omitempty" yaml:"SubnetID,omitempty"`
}

type image struct {
	Catalog string `json:"Catalog,omitempty" yaml:"Catalog,omitempty"`
	ID      string `json:"ID,omitempty" yaml:"ID,omitempty"`
	Os      string `json:"Os,omitempty" yaml:"Os,omitempty"`
}

type freeIpaServer struct {
	Domain   string `json:"Domain" yaml:"Domain"`
	Hostname string `json:"Hostname" yaml:"Hostname"`
}
