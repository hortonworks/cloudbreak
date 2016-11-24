package cli

import (
	"strings"
	"testing"
)

func TestGetRecipeNameWithValidUrl(t *testing.T) {
	url := "http://google.com/test-recipe.sh"

	name := getRecipeName(url)

	expected := "test-recipesh"
	if !strings.Contains(name, expected) {
		t.Errorf("expected %s != %s", expected, name)
	}
}

func TestGetRecipeNameWithValidUrl2(t *testing.T) {
	url := "http://google.com/test-recipe"

	name := getRecipeName(url)

	expected := "test-recipe"
	if !strings.Contains(name, expected) {
		t.Errorf("expected %s != %s", expected, name)
	}
}

func TestGetRecipeNameWithValidUrl3(t *testing.T) {
	url := "http://google.com/test-recipe.sh.sh"

	name := getRecipeName(url)

	expected := "test-recipeshsh"
	if !strings.Contains(name, expected) {
		t.Errorf("expected %s != %s", expected, name)
	}
}

func TestGetRecipeNameWithValidUrl4(t *testing.T) {
	url := "http://google.com/test-recipe.sh?query=true&predicate=true"

	name := getRecipeName(url)

	expected := "test-recipesh"
	if !strings.Contains(name, expected) {
		t.Errorf("expected %s != %s", expected, name)
	}
}

func TestGetRecipeNameWithInvalidUrl(t *testing.T) {
	url := "http://google.com/"

	name := getRecipeName(url)

	if !strings.Contains(name, "hrec") {
		t.Errorf("unexpected recipe name: %s", name)
	}
}

func TestGetRecipeNameWithInvalidUrl2(t *testing.T) {
	url := "http://google.com/"

	name := getRecipeName(url)

	if !strings.Contains(name, "hrec") {
		t.Errorf("unexpected recipe name: %s", name)
	}
}

func TestGetRecipeNameWithInvalidUrl3(t *testing.T) {
	url := "http://google.com/."

	name := getRecipeName(url)

	if !strings.Contains(name, "hrec") {
		t.Errorf("unexpected recipe name: %s", name)
	}
}

func TestGetRecipeNameWithInvalidUrl4(t *testing.T) {
	url := "http://google.com/.."

	name := getRecipeName(url)

	if !strings.Contains(name, "hrec") {
		t.Errorf("unexpected recipe name: %s", name)
	}
}

func TestGetRecipeNameWithInvalidUrl5(t *testing.T) {
	url := "http://google.com/?"

	name := getRecipeName(url)

	if !strings.Contains(name, "hrec") {
		t.Errorf("unexpected recipe name: %s", name)
	}
}
