# test_with_syntax_error Codebase Summary

---

## File Path : main.bal

```ballerina
import ballerina/i0 [L:1 - L:1]
```

```ballerina
function add(int x, int y) returns int [L:3 - L:6]
function calculateWeight(decimal mass, decimal gForce = 9.8) returns decimal [L:8 - L:10]
function print(anydata data) [L:12 - L:14]
```

```ballerina
public function main() [L:16 - L:25]
```

---

## File Path : service.bal

```ballerina
// [Parser Error] missing open brace pipe token [L:4 - L:4]
string title;
```

```ballerina
import ballerina/http [L:1 - L:1]
```

```ballerina
table<Album> key(title) albums [L:8 - L:11]
```

```ballerina
service / on new http:Listener(9090) { [L:13 - L:23]
    resource function get albums() returns Album[] [L:15 - L:17]
    resource function post albums(Album album) returns Album [L:19 - L:22]
}
```
