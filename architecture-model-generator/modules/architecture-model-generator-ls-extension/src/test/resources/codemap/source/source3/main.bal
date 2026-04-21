import ballerina/io;
import ballerina/test;
import ballerina/http;
import ballerina/constraint;

// This is a top-level comment.

# Description.
#
# + value - Parameter description
# + return - Return value description
function getValue(int value) returns int {
    return value;
}

// Function annotations are aligned with the starting position of the function.
@test:Config {
    before: beforeFunc,
    after: afterFunc
}
function testFunction() {
    io:println("I'm in test function!");
    test:assertTrue(true, msg = "Failed!");
}

// Empty annotation
@test:Config
function emptyAnnotationExample() {
    io:println("Empty annotation example");
}

// Parameter and return annotations
annotation validated on parameter, return;

// Parameter annotation.
public function secureFunction1(@validated string secureInName, int secureInId, string insecureIn) {
    // This is a block-level comment.
    int x = 10;
}

public function secureFunction2(string secureInName,
    @validated int secureInId, string insecureIn) {
    if true {
        if true {
            // This is a nested if block-level comment.
            string a = "hello";
        }
    }
}

// Return type annotation.
public function taintedReturn1() returns @validated string {
    return "secure";
}

public function taintedReturn2() returns
    @validated string {
    return "secure";
}

// Inline comment example
type People record {}; // Inline comment

function inlineCommentExample() {
    int a = 0; // Inline comment
}

// Record documentation
# Represents a user.
type User record {|
    # Id of the user
    int id;
    # Name of the user
    string name;
    # Whether the user is a member
    boolean isMember;
|};

// Record with annotations and spacing
# Represents a user with metadata.
type UserWithMetadata record {|
    # Id of the user
    @constraint:Int {
        maxDigits: 10
    }
    int id;

    # Name of the user
    @constraint:String {
        minLength: 5,
        maxLength: 20
    }
    string name;

    # Address of the user
    @constraint:String {
        minLength: 20,
        maxLength: 100
    }
    string address;
|};

// Service documentation
# Description.
service / on new http:Listener(8080) {

    # Description.
    # Test description.
    # + caller - Parameter description.
    # + request - Parameter description.
    resource function get greeting(http:Caller caller, http:Request request) {
        // Inline response
        http:Response res = new;
        res.setTextPayload("Hello, World!");
        http:ListenerError? respondResult = caller->respond(res);
        if respondResult is error {
            io:println("Failed to send response: ", respondResult.message());
        }
    }
}

// Test lifecycle functions
function beforeFunc() {
    io:println("Before test");
}

function afterFunc() {
    io:println("After test");
}