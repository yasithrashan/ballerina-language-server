# test_with_semantic_error Codebase Summary

---

## File Path : main.bal

```ballerina
import ballerina/http [L:1 - L:1]
```

```ballerina
table<$CompilationError$> key(title) albums [L:8 - L:11]
```

```ballerina
type Albumm record [L:3 - L:6]
```

```ballerina
service / on new http:Listener(9090) { [L:13 - L:23]
    resource function get albums() returns Album[] [L:15 - L:17]
    resource function post albums(Album album) returns Album [L:19 - L:22]
}
```
