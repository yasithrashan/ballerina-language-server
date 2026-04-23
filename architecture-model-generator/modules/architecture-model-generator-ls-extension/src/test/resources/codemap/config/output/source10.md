# test Codebase Summary

---

## File Path : rest_service.bal

```ballerina
import ballerina/http [L:1 - L:1]
import ballerina/mime [L:2 - L:2]
```

```ballerina
table<Album> key(title) albums [L:19 - L:22]
```

```ballerina
type Album record [L:5 - L:8]
type AlbumConflict record [L:11 - L:16]
```

```ballerina
service / on new http:Listener(9090) { [L:25 - L:49]
    resource function get albums(@http:Header string accept) returns Album[]|http:NotAcceptable [L:29 - L:36]
    resource function post albums(Album album) returns Album|AlbumConflict [L:40 - L:48]
}
```
