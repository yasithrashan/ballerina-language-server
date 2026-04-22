# test Codebase Summary

---

## File Path : function.bal

```ballerina
import ballerina/http [L:1 - L:1]
import ballerina/io [L:2 - L:2]
```

```ballerina
int defaultA [L:17 - L:17]
int defaultB [L:18 - L:18]
string[] names [L:20 - L:20]
IntFilter isEvenFn [L:74 - L:74]
function (int) returns int increment [L:76 - L:76]
function (string value) returns string[] appendName [L:78 - L:81]
```

```ballerina
type Options record [L:4 - L:7]
type Configuration record [L:9 - L:13]
type IntFilter function (int num) returns boolean [L:15 - L:15]
```

```ballerina
function add(int x, int y) returns int [L:22 - L:25]
function printSum(int x, int y, int z) [L:27 - L:29]
function calculateWeight(decimal mass, decimal gForce = 9.8) returns decimal [L:31 - L:33]
function extractSubstring(string str, int 'start = 0, int end = str.length()) returns string [L:35 - L:37]
function processFile(string inputFile, *Options options) [L:39 - L:39]
function printStrings(int n, string... s) [L:41 - L:41]
function printSumAll(int first, int second, int... others) [L:43 - L:49]
function createHttpClient(string url, decimal timeout, http:HttpVersion httpVersion) [L:51 - L:59]
function applyWithDefaults(function (int a = defaultA, int b = defaultB) returns int func) returns int [L:61 - L:63]
function applyBinaryIntFunc(function (int, int) returns int func, int v1, int v2) returns int [L:65 - L:67]
function isEven(int n) returns boolean [L:69 - L:71]
```
