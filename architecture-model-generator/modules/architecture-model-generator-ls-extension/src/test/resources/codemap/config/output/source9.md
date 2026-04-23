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

---

## File Path : main.bal

```ballerina
import ballerina/io [L:1 - L:1]
```

```ballerina
public function main() [L:3 - L:33]
```

---

## File Path : object.bal

```ballerina
import ballerina/io [L:1 - L:1]
import ballerina/http [L:2 - L:2]
import ballerina/lang.runtime [L:3 - L:3]
```

```ballerina
http:Listener httpListener [L:98 - L:98]
```

```ballerina
type Hashable object {function hash() returns int;} [L:35 - L:37]
type Cloneable object {function clone() returns Cloneable;} [L:52 - L:54]
type Persons object {string name; function getName() returns string; function clone() returns Cloneable;} [L:56 - L:60]
type Album record [L:77 - L:80]
```

```ballerina
function makeHashable() returns any [L:39 - L:49]
```

```ballerina
public function main() returns error? [L:116 - L:196]
```

```ballerina
class EngineerWithFinal { [L:6 - L:20]
    final string name [L:7 - L:7]
    int age [L:8 - L:8]
    function init(string name, int age) [L:10 - L:13]
    function getName() returns string [L:14 - L:16]
    function getAge() returns int [L:17 - L:19]
}
class EngineerWithDefault { [L:23 - L:32]
    string name [L:24 - L:24]
    function init(string name = "Null") [L:26 - L:28]
    function getName() returns string [L:29 - L:31]
}
class Engineer { [L:62 - L:74]
    function init(string name) [L:65 - L:67]
    function clone() returns Engineer [L:68 - L:70]
    function getName() returns string [L:71 - L:73]
}
public client class AlbumClient { [L:82 - L:95]
    private final http:Client httpClient [L:83 - L:83]
    function init(string url) returns error? [L:85 - L:87]
    resource function get [string path]() returns Album[]|error [L:88 - L:90]
    resource function post [string path](Album album) returns error? [L:91 - L:94]
}
public service class AlbumService { [L:100 - L:113]
    table<Album> key(title) albums [L:102 - L:105]
    resource function get albums() returns Album[] [L:107 - L:109]
    resource function post albums(Album album) [L:110 - L:112]
}
```

---

## File Path : type.bal

```ballerina
anydata x1 [L:48 - L:48]
anydata x2 [L:49 - L:49]
any number1 [L:51 - L:51]
typedesc<record {|anydata...;|}> t [L:53 - L:53]
int[] iv [L:55 - L:55]
any[] av [L:56 - L:56]
string str [L:58 - L:58]
string:Char ch [L:59 - L:59]
int cp [L:60 - L:60]
string|int s1 [L:62 - L:62]
Pair p [L:64 - L:68]
```

```ballerina
type StructuredName record [L:3 - L:6]
type Person record [L:8 - L:11]
type R record [L:13 - L:16]
type Pair record [L:18 - L:21]
type PairRest record [L:23 - L:26]
type Name StructuredName|string [L:30 - L:30]
type MapArray map<string>[] [L:32 - L:32]
type SwitchStatus "on"|"off" [L:34 - L:34]
```

```ballerina
function demo(anydata v) returns float|error [L:72 - L:74]
```

```ballerina
class EvenNumberGenerator { [L:38 - L:44]
    int i [L:39 - L:39]
    public isolated function next() returns record {|int value;|}|error? [L:40 - L:43]
}
```
