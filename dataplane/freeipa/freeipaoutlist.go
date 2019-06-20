package freeipa

var listHeader = []string{"Name", "Crn", "EnvironmentCrn", "Status"}

type freeIpaDetails struct {
	Name           string `json:"Name" yaml:"Name"`
	CRN            string `json:"CRN" yaml:"CRN"`
	EnvironmentCrn string `json:"EnvironmentCrn" yaml:"EnvironmentCrn"`
	Status         string `json:"Status" yaml:"Status"`
}

func (ipa *freeIpaDetails) DataAsStringArray() []string {
	return []string{ipa.Name, ipa.CRN, ipa.EnvironmentCrn, ipa.Status}
}
