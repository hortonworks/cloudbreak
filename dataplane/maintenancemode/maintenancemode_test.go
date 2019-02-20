package maintenancemode

import (
	v4maint "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_stacks"
	"github.com/hortonworks/cb-cli/dataplane/flags"
	"testing"

	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/types"
)

func Test_resolveWorkspaceIDAndStackName(t *testing.T) {
	type args struct {
		int64Finder  func(string) int64
		stringFinder func(string) string
	}
	tests := []struct {
		name          string
		args          args
		wantWorkspace int64
		wantName      string
	}{
		{
			name: "Test resolve workspace ID and stack name from flags",
			args: args{
				int64Finder: func(argName string) int64 {
					if argName == flags.FlWorkspaceOptional.Name {
						return 1
					}
					return 0
				},
				stringFinder: func(argName string) string {
					if argName == flags.FlName.Name {
						return "test_stack"
					}
					return ""
				},
			},
			wantWorkspace: 1,
			wantName:      "test_stack",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotWorkspace, gotName := resolveWorkspaceIDAndStackName(tt.args.int64Finder, tt.args.stringFinder)
			if gotWorkspace != tt.wantWorkspace {
				t.Errorf("resolveWorkspaceIDAndStackName() got = %v, want %v", gotWorkspace, tt.wantWorkspace)
			}
			if gotName != tt.wantName {
				t.Errorf("resolveWorkspaceIDAndStackName() got1 = %v, want %v", gotName, tt.wantName)
			}
		})
	}
}

type mockMaintenanceModeClient struct {
	t                     *testing.T
	maintenanceModeStatus string
	wantStackDetails      *model.StackRepositoryV4Request
}

func (mockClient mockMaintenanceModeClient) SetClusterMaintenanceMode(params *v4maint.SetClusterMaintenanceModeParams) error {
	if params.WorkspaceID != 1 {
		mockClient.t.Errorf("SetClusterMaintenanceMode() got = %v, want %v", params.WorkspaceID, 1)
	}

	if params.Name != "test_stack" {
		mockClient.t.Errorf("SetClusterMaintenanceMode() got = %v, want %v", params.Name, "test_stack")
	}

	if params.Body.Status != mockClient.maintenanceModeStatus {
		mockClient.t.Errorf("SetClusterMaintenanceMode() got = %v, want %v", params.Body.Status, mockClient.maintenanceModeStatus)
	}

	return nil
}

func (mockClient mockMaintenanceModeClient) PutClusterV4(params *v4maint.PutClusterV4Params) error {
	if params.WorkspaceID != 1 {
		mockClient.t.Errorf("PutClusterV3() got = %v, want %v", params.WorkspaceID, 1)
	}

	if params.Name != "test_stack" {
		mockClient.t.Errorf("PutClusterV3() got = %v, want %v", params.Name, "test_stack")
	}

	if params.Body.StackRepository.Stack != mockClient.wantStackDetails.Stack {
		mockClient.t.Errorf("PutClusterV3() got = %v, want %v", params.Body.StackRepository.Stack, mockClient.wantStackDetails.Stack)
	}

	if params.Body.StackRepository.Version != mockClient.wantStackDetails.Version {
		mockClient.t.Errorf("PutClusterV3() got = %v, want %v", params.Body.StackRepository.Version, mockClient.wantStackDetails.Version)
	}

	if params.Body.StackRepository.Repository.BaseURL != mockClient.wantStackDetails.Repository.BaseURL {
		mockClient.t.Errorf("PutClusterV3() got = %v, want %v", params.Body.StackRepository.Repository.BaseURL, mockClient.wantStackDetails.Repository.BaseURL)
	}

	if params.Body.StackRepository.Repository.GpgKeyURL != mockClient.wantStackDetails.Repository.GpgKeyURL {
		mockClient.t.Errorf("PutClusterV3() got = %v, want %v", params.Body.StackRepository.Repository.GpgKeyURL, mockClient.wantStackDetails.Repository.GpgKeyURL)
	}

	return nil
}

func Test_toggleMaintenanceMode(t *testing.T) {
	type args struct {
		client            maintenanceModeClient
		workspaceId       int64
		name              string
		maintenanceModeOn string
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "Test enable maintenance mode",
			args: args{
				client: mockMaintenanceModeClient{
					t:                     t,
					maintenanceModeStatus: model.MaintenanceModeV4RequestStatusENABLED,
				},
				workspaceId:       1,
				name:              "test_stack",
				maintenanceModeOn: model.MaintenanceModeV4RequestStatusENABLED,
			},
		},
		{
			name: "Test disable maintenance mode",
			args: args{
				client: mockMaintenanceModeClient{
					t:                     t,
					maintenanceModeStatus: model.MaintenanceModeV4RequestStatusDISABLED,
				},
				workspaceId:       1,
				name:              "test_stack",
				maintenanceModeOn: model.MaintenanceModeV4RequestStatusDISABLED,
			},
		},
		{
			name: "Test validate stack in maintenance mode",
			args: args{
				client: mockMaintenanceModeClient{
					t:                     t,
					maintenanceModeStatus: model.MaintenanceModeV4RequestStatusVALIDATIONREQUESTED,
				},
				workspaceId:       1,
				name:              "test_stack",
				maintenanceModeOn: model.MaintenanceModeV4RequestStatusVALIDATIONREQUESTED,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			toggleMaintenanceMode(tt.args.client, tt.args.workspaceId, tt.args.name, tt.args.maintenanceModeOn)
		})
	}
}

func Test_unmarshallCliInput(t *testing.T) {
	gotStackDetails := model.StackRepositoryV4Request{}

	type args struct {
		stringFinder func(string) string
		stackDetails *model.StackRepositoryV4Request
	}
	tests := []struct {
		name             string
		args             args
		wantStackDetails model.StackRepositoryV4Request
	}{
		{
			name: "Test unmarshall Ambari repo config",
			args: args{
				stringFinder: func(argName string) string {
					if argName == flags.FlInputJson.Name {
						return "../testdata/ambari-repo-conf.json"
					}
					return ""
				},
				stackDetails: &gotStackDetails,
			},
			wantStackDetails: model.StackRepositoryV4Request{
				Stack:   &(&types.S{S: "AMBARI"}).S,
				Version: &(&types.S{S: "2.7.0.0"}).S,
				Repository: &model.RepositoryV4Request{
					BaseURL:   "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0",
					GpgKeyURL: "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins",
				},
			},
		},
		{
			name: "Test unmarshall HDP repo config",
			args: args{
				stringFinder: func(argName string) string {
					if argName == flags.FlInputJson.Name {
						return "../testdata/hdp-repo-conf.json"
					}
					return ""
				},
				stackDetails: &gotStackDetails,
			},
			wantStackDetails: model.StackRepositoryV4Request{
				Stack:                    &(&types.S{S: "HDP"}).S,
				Version:                  &(&types.S{S: "2.6.5.0"}).S,
				VersionDefinitionFileURL: "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0/HDP-2.6.5.0-292.xml",
			},
		},
		{
			name: "Test unmarshall HDF repo config",
			args: args{
				stringFinder: func(argName string) string {
					if argName == flags.FlInputJson.Name {
						return "../testdata/hdf-repo-conf.json"
					}
					return ""
				},
				stackDetails: &gotStackDetails,
			},
			wantStackDetails: model.StackRepositoryV4Request{
				Stack:                    &(&types.S{S: "HDF"}).S,
				Version:                  &(&types.S{S: "3.2.0.6-2"}).S,
				VersionDefinitionFileURL: "http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.2.0.6-2/HDF-3.2.0.6-2.xml",
				MpackURL:                 "http://private-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.2.0.6-2/HDF-3.2.0.6-2-centos7-rpm.tar.gz",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			unmarshallCliInput(tt.args.stringFinder, tt.args.stackDetails)
			if "AMBARI" == *tt.wantStackDetails.Stack {
				if *gotStackDetails.Version != *tt.wantStackDetails.Version {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.Version)
				}
				if gotStackDetails.Repository.BaseURL != tt.wantStackDetails.Repository.BaseURL {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.Repository.BaseURL)
				}
				if gotStackDetails.Repository.GpgKeyURL != tt.wantStackDetails.Repository.GpgKeyURL {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.Repository.GpgKeyURL)
				}
			}
			if "HDP" == *tt.wantStackDetails.Stack {
				if *gotStackDetails.Version != *tt.wantStackDetails.Version {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.Version)
				}
				if gotStackDetails.VersionDefinitionFileURL != tt.wantStackDetails.VersionDefinitionFileURL {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.VersionDefinitionFileURL)
				}
			}
			if "HDF" == *tt.wantStackDetails.Stack {
				if *gotStackDetails.Version != *tt.wantStackDetails.Version {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.Version)
				}
				if gotStackDetails.VersionDefinitionFileURL != tt.wantStackDetails.VersionDefinitionFileURL {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.VersionDefinitionFileURL)
				}
				if gotStackDetails.MpackURL != tt.wantStackDetails.MpackURL {
					t.Errorf("unmarshallCliInput() got = %v, want %v", gotStackDetails, tt.wantStackDetails.MpackURL)
				}
			}
		})
	}
}

func Test_updateStackRepoDetails(t *testing.T) {
	type args struct {
		client       maintenanceModeClient
		workspaceID  int64
		name         string
		stackDetails *model.StackRepositoryV4Request
	}
	stackDetails := model.StackRepositoryV4Request{
		Stack:   &(&types.S{S: "AMBARI"}).S,
		Version: &(&types.S{S: "2.7.0.0"}).S,
		Repository: &model.RepositoryV4Request{
			BaseURL:   "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0",
			GpgKeyURL: "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins",
		},
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "Test update Ambari repo details",
			args: args{
				client: mockMaintenanceModeClient{
					t:                t,
					wantStackDetails: &stackDetails,
				},
				workspaceID:  1,
				name:         "test_stack",
				stackDetails: &stackDetails,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			updateStackRepoDetails(tt.args.client, tt.args.workspaceID, tt.args.name, tt.args.stackDetails)
		})
	}
}
