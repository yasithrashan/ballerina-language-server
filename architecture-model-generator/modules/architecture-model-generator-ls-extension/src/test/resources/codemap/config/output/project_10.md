# test Codebase Summary

---

## File Path : graphql_service.bal

```ballerina
import ballerina/graphql [L:1 - L:1]
import ballerina/http [L:2 - L:2]
```

```ballerina
isolated function validateScope(graphql:Context context, string[] allowedScopes) returns error? [L:51 - L:60]
isolated function contextInit(http:RequestContext requestContext, http:Request request) returns graphql:Context|error [L:62 - L:73]
```

```ballerina
@graphql:ServiceConfig()
service /graphql on new graphql:Listener(9090) { [L:4 - L:23]
    private final Profile profile [L:10 - L:10]
    function init() [L:12 - L:15]
    resource function get profile(graphql:Context context) returns Profile|error [L:18 - L:22]
}
```

```ballerina
service class Profile { [L:26 - L:49]
    private final string name [L:27 - L:27]
    private final int age [L:28 - L:28]
    private final float salary [L:29 - L:29]
    function init(string name, int age, float salary) [L:31 - L:35]
    resource function get name() returns string [L:37 - L:37]
    resource function get age() returns int [L:39 - L:39]
    resource function get salary(graphql:Context context) returns float|error [L:44 - L:48]
}
```

---

## File Path : http_client.bal

```ballerina
import ballerina/http [L:1 - L:1]
import ballerina/io [L:2 - L:2]
import ballerina/mime [L:3 - L:3]
import ballerina/constraint [L:4 - L:4]
```

```ballerina
final http:Client albumClient [L:18 - L:18]
```

```ballerina
type MusicAlbum record [L:8 - L:15]
```

```ballerina
function getAlbums() returns MusicAlbum[]|error [L:22 - L:27]
function getAlbumsByArtistQuery(string artist) returns MusicAlbum[]|error [L:31 - L:35]
function getAlbumByPathParam(string artist) returns MusicAlbum|error [L:39 - L:43]
function getAlbumsWithAcceptHeader() returns MusicAlbum[]|error [L:47 - L:53]
function postAlbum() returns MusicAlbum|error [L:57 - L:64]
function postAlbumWithConstraint() returns MusicAlbum|error [L:68 - L:75]
```

```ballerina
public function main(string artist = "Blue Train") returns error? [L:79 - L:86]
```

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

---

## File Path : ssl.bal

```ballerina
import ballerina/http [L:1 - L:1]
import ballerina/io [L:2 - L:2]
```

```ballerina
type Album record [L:4 - L:7]
```

```ballerina
public function main() returns error? [L:9 - L:22]
```

---

## File Path : websocket.bal

```ballerina
import ballerina/websocket [L:1 - L:1]
```

```ballerina
listener chatListener : websocket:Listener [L:8 - L:15]
```

```ballerina
service /chat on chatListener { [L:17 - L:22]
    resource function get .() returns websocket:Service [L:19 - L:21]
}
```

```ballerina
service class ChatService { [L:24 - L:30]
    remote function onMessage(websocket:Caller caller, string chatMessage) returns error? [L:27 - L:29]
}
```
