// Record Types

type StructuredName record {
    string firstName;
    string lastName;
};

type Person record {
    string name;
    int age;
};

type R record {
    int x;
    int y;
};

type Pair record {
    int x;
    int y;
};

type PairRest record {
    never x?;
    never y?;
};

// Type Aliases & Unions

type Name StructuredName|string;

type MapArray map<string>[];

type SwitchStatus "on"|"off";

// Classes

class EvenNumberGenerator {
    int i = 0;
    public isolated function next() returns record {|int value;|}|error? {
        self.i += 2;
        return {value: self.i};
    }
}

// Module-level Variables

anydata x1 = [1, "string", true];
anydata x2 = x1.clone();

any number1 = 1;

typedesc<record {}> t = R;

int[] iv = [1, 2, 3];
any[] av = iv;

var str = "str";
string:Char ch = "x";
int cp = ch.toCodePointInt();

string|int s1 = "Ballerina";

Pair p = {
    x: 1,
    y: 2,
    "color": "blue"
};

// Functions

function demo(anydata v) returns float|error {
    return v.ensureType(float);
}
