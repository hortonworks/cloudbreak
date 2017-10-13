package cli

var cloudResourceHeader []string = []string{"Name", "Description", "CloudPlatform"}

type cloudResourceOut struct {
	Name          string `json:"Name" yaml:"Name"`
	Description   string `json:"Description" yaml:"Description"`
	CloudPlatform string `json:"CloudPlatform" yaml:"CloudPlatform"`
}

func (c *cloudResourceOut) DataAsStringArray() []string {
	return []string{c.Name, c.Description, c.CloudPlatform}
}
