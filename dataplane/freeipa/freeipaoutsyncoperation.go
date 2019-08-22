package freeipa

import (
	"fmt"
)

var syncStatusHeader = []string{"ID", "Status", "SyncType", "Success", "Failure", "Error", "StartTime", "EndTime"}

type freeIpaOutSyncOperation struct {
	ID        string          `json:"ID" yaml:"ID"`
	Status    string          `json:"Status" yaml:"Status"`
	SyncType  string          `json:"SyncType" yaml:"SyncType"`
	Success   []successDetail `json:"Success" yaml:"Success"`
	Failure   []failureDetail `json:"Failure" yaml:"Failure"`
	Error     string          `json:"Error,omitempty" yaml:"Error,omitempty"`
	StartTime string          `json:"StartTime" yaml:"StartTime"`
	EndTime   string          `json:"EndTime,omitempty" yaml:"EndTime,omitempty"`
}

type successDetail struct {
	Environment string `json:"Environment" yaml:"Environment"`
}

type failureDetail struct {
	Environment string `json:"Environment" yaml:"Environment"`
	Details     string `json:"Details" yaml:"Details"`
}

func (f *freeIpaOutSyncOperation) DataAsStringArray() []string {
	var successString string
	for _, success := range f.Success {
		successString += fmt.Sprintf("%s\n", success.Environment)
	}
	var failureString string
	for _, failure := range f.Failure {
		failureString += fmt.Sprintf("Environment: %s\n", failure.Environment)
		failureString += fmt.Sprintf("Details: %s\n\n", failure.Details)
	}
	return []string{f.ID, f.Status, f.SyncType, successString, failureString, f.Error, f.StartTime, f.EndTime}
}
