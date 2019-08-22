package common

var CloudResourceHeader = []string{"Name", "Description", "CloudPlatform"}

type CloudResourceOut struct {
	Name          string `json:"name" yaml:"name"`
	Description   string `json:"description" yaml:"description"`
	CloudPlatform string `json:"cloudPlatform" yaml:"cloudPlatform"`
}

func (c *CloudResourceOut) DataAsStringArray() []string {
	return []string{c.Name, c.Description, c.CloudPlatform}
}
