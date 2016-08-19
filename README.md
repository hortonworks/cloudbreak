# Hortonworks Data Cloud - Command Line Interface

## Dependency management

This project uses [Godep](https://github.com/tools/godep) for dependency management. You can install it and download the configured dependency versions with:
```
go get github.com/tools/godep
godep restore
```

If you changed the external dependencies then you can save it with:
```
godep save
```