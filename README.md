# MongoDB Atlas CLI

The MongoDB Atlas CLI provides a commad-line interface for common Atlas operations.


## Build
The *Atlas CLI* project use maven. To build, execute the following command:
```
mvn clean compile assembly:single
```

This will compile an executable jar file.  If you wish to build in a shell script to make it a standalone command, please execute the following:
```
cat scripts/stub.sh target/atlas-cli-{version}-SNAPSHOT.jar > atlas && chmod +x atlas
```

## Configuration
All operations of the CLI require the following information:
- Username
- API Key
- Group ID

These can be passed to each request using the *-u* and *-g* or cached for repeat operations.  To store this data for all requests, execute the following:
`atlas config`

Enter the requested data when prompted.  When all fields have been entered, the configuration will be saved locally to the `{user.home}/.atlas` file.


## Usage
* Parent Commands:
    + config
    + clusters

```
atlas config
atlas clusters
```

### Cluster Operations

#### List
##### Options
* -cn --clusterName: The name of the cluster the be returned (optional)

##### (All)
```
atlas clusters list
```

##### Cluster Name
```
atlas clusters list -cn MyCluster
```

#### Status
##### Options
* -r --refresh <interval>: Refresh the output at the given 
interval.  Defaults to 5s if no interval provided (optional)

```
atlas clusters status -r 5
```



