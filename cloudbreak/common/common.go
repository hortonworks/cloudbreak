package common

var CloudResourceHeader []string = []string{"Name", "Description", "CloudPlatform"}

type CloudResourceOut struct {
	Name          string `json:"Name" yaml:"Name"`
	Description   string `json:"Description" yaml:"Description"`
	CloudPlatform string `json:"CloudPlatform" yaml:"CloudPlatform"`
}

func (c *CloudResourceOut) DataAsStringArray() []string {
	return []string{c.Name, c.Description, c.CloudPlatform}
}
