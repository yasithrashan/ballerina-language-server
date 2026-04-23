import ballerina/http;
import ballerina/io;

type Options record {|
    boolean verbose = false;
    string? outputFile = ();
|};

type Configuration record {|
    string url;
    decimal timeout;
    http:HttpVersion httpVersion;
|};

type IntFilter function (int num) returns boolean;

int defaultA = 10;
int defaultB = 100;

string[] names = ["Ana", "Alice", "Bob"];

function add(int x, int y) returns int {
    int sum = x + y;
    return sum;
}

function printSum(int x, int y, int z) {
    io:println("Sum of x, y and z:", x + y + z);
}

function calculateWeight(decimal mass, decimal gForce = 9.8) returns decimal {
    return mass * gForce;
}

function extractSubstring(string str, int 'start = 0, int end = str.length()) returns string {
    return str.substring('start, end);
}

function processFile(string inputFile, *Options options) {}

function printStrings(int n, string... s) {}

function printSumAll(int first, int second, int... others) {
    int sum = first + second;
    foreach int val in others {
        sum += val;
    }
    io:println(sum);
}

function createHttpClient(string url, decimal timeout, http:HttpVersion httpVersion) {
    http:Client|error cl = new (url, {timeout, httpVersion});
    if cl is error {
        io:println("Failed to initialize an HTTP client", cl);
        return;
    }
    io:println(
        string `Initialized client with URL: ${url}, timeout: ${timeout}, HTTP version: ${httpVersion}`);
}

function applyWithDefaults(function (int a = defaultA, int b = defaultB) returns int func) returns int {
    return func();
}

function applyBinaryIntFunc(function (int, int) returns int func, int v1, int v2) returns int {
    return func(v1, v2);
}

function isEven(int n) returns boolean {
    return n % 2 == 0;
}


IntFilter isEvenFn = isEven;

function (int) returns int increment = x => x + 1;

var appendName = function(string value) returns string[] {
    names.push(value);
    return names;
};
