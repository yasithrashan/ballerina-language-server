# test Codebase Summary

---

## File Path : main.bal

```ballerina
import ballerina/test [L:1 - L:1]
```

```ballerina
@test:Config()
function testAssertEquals() [L:3 - L:11]
@test:Config()
function testAssertNotEquals() [L:13 - L:19]
@test:Config()
function testAssertTrue() [L:21 - L:26]
@test:Config()
function testAssertFalse() [L:28 - L:33]
@test:Config()
function testAssertFail() [L:35 - L:43]
@test:Config()
function testAssertExactEquals() [L:53 - L:60]
@test:Config()
# Verifies that two `Person` objects are not exactly equal (not the same reference in memory).
function testAssertNotExactEquals() [L:62 - L:70]
```

```ballerina
class Person { [L:45 - L:51]
    public string name [L:46 - L:46]
    public int age [L:47 - L:47]
    public Person? parent [L:48 - L:48]
    private string email [L:49 - L:49]
    string address [L:50 - L:50]
}
```
