package cli

import (
	"github.com/hortonworks/cb-cli/cli/cloud"
	"io/ioutil"
	"testing"
)

func TestGetNodesByBlueprint(t *testing.T) {
	bp, _ := ioutil.ReadFile("testdata/blueprint.json")

	nodes := getNodesByBlueprint(bp)

	expectedNodes := []cloud.Node{
		{"master", "GATEWAY", 1},
		{"slave_1", "CORE", 3},
		{"slave_2", "CORE", 3},
		{"slave_3", "CORE", 1},
	}
	for i, n := range expectedNodes {
		node := nodes[i]
		if node.Name != n.Name {
			t.Errorf("node[%d] name not match %s == %s", i, n.Name, node.Name)
		} else if node.GroupType != n.GroupType {
			t.Errorf("node[%d] group type not match %s == %s", i, n.GroupType, node.GroupType)
		} else if node.Count != n.Count {
			t.Errorf("node[%d] count not match %d == %d", i, n.Count, node.Count)
		}
	}
}
