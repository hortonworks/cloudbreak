package utils

import (
	"reflect"
	"testing"
)

func TestRandStr(t *testing.T) {
	res1 := RandStr(10)
	res2 := RandStr(10)
	if len(res1) != 10 || len(res2) != 10 {
		t.Errorf("Wrong length of random strings")
	}
	if res1 == res2 {
		t.Errorf("Random strings are equals")
	}
}

func TestSafeInt32ConvertIsNil(t *testing.T) {
	expected := int32(0)
	resp := SafeInt32Convert(nil)
	if resp != expected {
		t.Errorf("expected %d != %d", expected, resp)
	}
}

func TestSafeInt32ConvertIsNotNil(t *testing.T) {
	expected := int32(0)
	resp := SafeInt32Convert(&expected)
	if resp != expected {
		t.Errorf("expected %d != %d", expected, resp)
	}
}

func TestSafeStringConvertIsNil(t *testing.T) {
	resp := SafeStringConvert(nil)
	if resp != "" {
		t.Errorf("expected empty != %s", resp)
	}
}

func TestSafeStringConvertIsNotNil(t *testing.T) {
	expected := "content"
	resp := SafeStringConvert(&expected)
	if resp != expected {
		t.Errorf("expected %s != %s", expected, resp)
	}
}

func TestEscapeStringToJson(t *testing.T) {
	password := EscapeStringToJson("§±!@#$%^&*()_+-=[]{};'\\:\"/.,?><`~")
	expectedPassword := "§±!@#$%^&*()_+-=[]{};'\\\\:\\\"/.,?><`~"
	if password != expectedPassword {
		t.Errorf("expected %s != %s", expectedPassword, password)
	}
}

func TestReadFile(t *testing.T) {
	content := ReadFile("testdata/file")

	if "content\n" != string(content) {
		t.Errorf("content not match content == %s", string(content))
	}
}

func TestSemicolonDelimiterConvert(t *testing.T) {
	result := DelimitedStringToArray("simple;test", ";")

	expected := []string{"simple", "test"}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("content not match content == %s", result)
	}
}

func TestSemicolonDelimiterConvertToInvalidString(t *testing.T) {
	result := DelimitedStringToArray("simple,test", ";")

	expected := []string{"simple,test"}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("content not match content == %s", result)
	}
}

func TestSemicolonDelimiterConvertToEmptyString(t *testing.T) {
	result := DelimitedStringToArray("", ";")

	expected := make([]string, 0)
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("content not match content == %s", result)
	}
}
