package main

import (
	"bytes"
	"flag"
	"go/ast"
	"go/format"
	"go/parser"
	"go/token"
	"io/ioutil"
	"strconv"
	"strings"

	"github.com/hortonworks/cb-cli/cli/utils"
	"golang.org/x/tools/go/ast/astutil"
)

var src string
var operation string
var exp string
var file *ast.File

func init() {
	flag.StringVar(&src, "src", "", "location of original source file")
	flag.StringVar(&operation, "operation", "", "type of operation [remove-statement]")
	flag.StringVar(&exp, "exp", "", "expression of operation")
	flag.Parse()
}

func main() {
	content, err := ioutil.ReadFile(src)
	if err != nil {
		panic(err)
	}

	fset := token.NewFileSet()
	file, err = parser.ParseFile(fset, "src.go", content, 0)
	if err != nil {
		panic(err)
	}

	switch operation {
	case "remove-statement":
		removeStatement()
	default:
		panic("operation not allowed")
	}

	var buf bytes.Buffer
	if err = format.Node(&buf, fset, file); err != nil {
		panic(err)
	}
	if err = ioutil.WriteFile(src, buf.Bytes(), 0644); err != nil {
		panic(err)
	}
}

func removeStatement() {
	exps := strings.Split(exp, ":")
	target := getFunction(exps[0])
	nodes := getNodes(target, strings.Split(exps[1], ","), make([]*ast.Node, 0))
	parent := nodes[len(nodes)-2]
	last := *nodes[len(nodes)-1]
	found := 0
	block := getBlockStmt(parent)
	for i, n := range block.List {
		if last == n {
			found = i
			break
		}
	}
	block.List = append(block.List[:found], block.List[found+1:]...)
}

func getFunction(name string) (resp *ast.FuncDecl) {
	ast.Inspect(file, func(n ast.Node) bool {
		if fd, ok := n.(*ast.FuncDecl); ok && fd.Name.String() == name {
			resp = fd
			return false
		}
		return true
	})
	return
}

func getNodes(target ast.Node, stmts []string, collect []*ast.Node) []*ast.Node {
	if len(stmts) != 0 && target != nil {
		exp := strings.Split(stmts[0], "-")
		what := exp[0]
		where, err := strconv.Atoi(exp[1])
		if err != nil {
			utils.LogErrorMessageAndExit("Unable to parse as number: " + exp[1])
			panic(4)
		}
		node := findNode(&getBlockStmt(&target).List, what, where)
		collect = getNodes(node, stmts[1:], append(collect, &node))
	}
	return collect
}

func getBlockStmt(target *ast.Node) *ast.BlockStmt {
	tmp := *target
	switch t := tmp.(type) {
	case *ast.FuncDecl:
		return t.Body
	case *ast.RangeStmt:
		return t.Body
	case *ast.ForStmt:
		return t.Body
	case *ast.IfStmt:
		return t.Body
	}
	return nil
}

func findNode(stmts *[]ast.Stmt, what string, where int) (node ast.Node) {
	found := 0
	for _, s := range *stmts {
		sameType := strings.Index(astutil.NodeDescription(s), what) == 0
		if sameType {
			if found == where {
				node = s
			}
			found++
		}
	}
	return
}
