### Migrate the databases
Create the database schema or migrate it to the latest version:

```
cbd startdb
cbd migrate cbdb up
```

Verify that all scripts have been applied:
```
cbd migrate cbdb status
```


```
cbd generate
cbd migrate cbdb up
```

