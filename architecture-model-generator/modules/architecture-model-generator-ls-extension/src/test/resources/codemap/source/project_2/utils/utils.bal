public function hello(string? name) returns string {
    if name !is () {
        return string `Hello, ${name}`;
    }
    return "Hello, World!";
}
