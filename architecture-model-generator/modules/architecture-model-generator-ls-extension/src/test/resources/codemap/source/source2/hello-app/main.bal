import ballerina/io;
import wso2/utils;

public function main() {
    string result = utils:hello("sample data !");
    io:println(result);
}