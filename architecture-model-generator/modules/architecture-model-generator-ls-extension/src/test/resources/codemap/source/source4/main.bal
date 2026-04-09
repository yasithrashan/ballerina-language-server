import ballerina/io;

public function main()
    io:println("Hello, World!");
    greetUser(); // calling the new function
}

// new function
funnction  greetUser(){
    io:println("Welcome to Ballerina!");
}
