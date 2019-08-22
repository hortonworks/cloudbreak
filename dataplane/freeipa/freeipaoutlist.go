package freeipa

var listHeader = []string{"Name", "Crn", "EnvironmentCrn", "Status"}

type freeIpaDetails struct {
	Name           string `json:"name" yaml:"name"`
	CRN            string `json:"crn" yaml:"crn"`
	EnvironmentCrn string `json:"environmentCrn" yaml:"environmentCrn"`
	Status         string `json:"status" yaml:"status"`
}

func (ipa *freeIpaDetails) DataAsStringArray() []string {
	return []string{ipa.Name, ipa.CRN, ipa.EnvironmentCrn, ipa.Status}
}
