import ballerina/i0;

function add(int x, int y) returns int {
    int sum = x + y;
    return sum;
}

function calculateWeight(decimal mass, decimal gForce = 9.8) returns decimal {
    return mass * gForce;
}

function print(anydata data) {
    io:println(data);
}

public function main() {
    int sum = add(5, 11);
    print(sum);

    print(calculateWeight(5));

    print(add(x = 5, y = 6));

    _ = calculateWeight(mass = 5, gForce = 10);
}