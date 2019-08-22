package freeipa

import (
	"fmt"
)

var syncStatusHeader = []string{"ID", "Status", "SyncType", "Success", "Failure", "Error", "StartTime", "EndTime"}

type freeIpaOutSyncOperation struct {
	ID        string          `json:"id" yaml:"id"`
	Status    string          `json:"status" yaml:"status"`
	SyncType  string          `json:"syncType" yaml:"syncType"`
	Success   []successDetail `json:"success" yaml:"success"`
	Failure   []failureDetail `json:"failure" yaml:"failure"`
	Error     string          `json:"error,omitempty" yaml:"error,omitempty"`
	StartTime string          `json:"startTime" yaml:"startTime"`
	EndTime   string          `json:"endTime,omitempty" yaml:"endTime,omitempty"`
}

type successDetail struct {
	Environment string `json:"environment" yaml:"environment"`
}

type failureDetail struct {
	Environment string `json:"environment" yaml:"environment"`
	Details     string `json:"details" yaml:"details"`
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
