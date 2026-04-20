# test Codebase Summary

---

## File Path : main.bal

```ballerina
import ballerina/io [L:1 - L:1]
import ballerina/test [L:2 - L:2]
import ballerina/http [L:3 - L:3]
import ballerina/constraint [L:4 - L:4]
```

```ballerina
type People record [L:62 - L:62]
// Represents a user.
type User record [L:69 - L:77]
// Represents a user with metadata.
type UserWithMetadata record [L:80 - L:101]
```

```ballerina
// Description.
//
//
// # + value - Parameter description
//
// # + return - Return value description
function getValue(int : value) returns int [L:8 - L:14]
function testFunction() [L:17 - L:24]
function emptyAnnotationExample() [L:27 - L:30]
public function secureFunction1(string : secureInName, int : secureInId, string : insecureIn) [L:36 - L:39]
public function secureFunction2(string : secureInName, int : secureInId, string : insecureIn) [L:41 - L:49]
public function taintedReturn1() returns string [L:52 - L:54]
public function taintedReturn2() returns string [L:56 - L:59]
function inlineCommentExample() [L:64 - L:66]
function beforeFunc() [L:123 - L:125]
function afterFunc() [L:127 - L:129]
```

```ballerina
// Description.
service / on new http:Listener(8080) { [L:104 - L:120]
    // Description.
    //
    //
    // # + caller - Parameter description.
    //
    // # + request - Parameter description.
    resource function get greeting(http:Caller : caller, http:Request : request) [L:107 - L:119]
}
```
