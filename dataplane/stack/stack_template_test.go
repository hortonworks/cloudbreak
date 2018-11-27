package stack

import (
	"io/ioutil"
	"testing"

	"github.com/hortonworks/cb-cli/dataplane/cloud"
)

func TestGetNodesByBlueprint(t *testing.T) {
	t.Parallel()

	bp, _ := ioutil.ReadFile("../testdata/blueprint.json")

	nodes := getNodesByBlueprint(bp)

	expectedNodes := []cloud.Node{
		{Name: "master", GroupType: "GATEWAY", Count: 1},
		{Name: "slave_1", GroupType: "CORE", Count: 3},
		{Name: "slave_2", GroupType: "CORE", Count: 3},
		{Name: "slave_3", GroupType: "CORE", Count: 1},
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
