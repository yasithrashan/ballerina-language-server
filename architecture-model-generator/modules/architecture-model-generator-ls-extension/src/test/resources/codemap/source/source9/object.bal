import ballerina/io;
import ballerina/http;
import ballerina/lang.runtime;

// 1. CLASS WITH FINAL FIELD
class EngineerWithFinal {
    final string name;
    int age;

    function init(string name, int age) {
        self.name = name;
        self.age = age;
    }
    function getName() returns string {
        return self.name;
    }
    function getAge() returns int {
        return self.age;
    }
}

// 2. CLASS WITH DEFAULT PARAMETER IN INIT
class EngineerWithDefault {
    string name;

    function init(string name = "Null") {
        self.name = name;
    }
    function getName() returns string {
        return self.name;
    }
}

// 3. OBJECT TYPE (STRUCTURAL / INTERFACE)
type Hashable object {
    function hash() returns int;
};

function makeHashable() returns any {
    var obj = object {
        function hash() returns int {
            return 42;
        }
        function zero() returns int {
            return 0;
        }
    };
    return obj;
}

// 4. OBJECT TYPE INCLUSION
type Cloneable object {
    function clone() returns Cloneable;
};

type Persons object {
    *Cloneable;
    string name;
    function getName() returns string;
};

class Engineer {
    *Persons;

    function init(string name) {
        self.name = name;
    }
    function clone() returns Engineer {
        return new (self.name);
    }
    function getName() returns string {
        return self.name;
    }
}

// 5. CLIENT CLASS
public type Album readonly & record {|
    string title;
    string artist;
|};

public client class AlbumClient {
    private final http:Client httpClient;

    function init(string url) returns error? {
        self.httpClient = check new (url);
    }
    resource function get [string path]() returns Album[]|error {
        return check self.httpClient->/[path];
    }
    resource function post [string path](Album album) returns error? {
        Album updatedAlbum = check self.httpClient->/[path].post(album);
        io:println("\nPOST request: ", updatedAlbum);
    }
}

// 6. SERVICE CLASS
http:Listener httpListener = check new (9090);

public service class AlbumService {
    *http:Service;
    table<Album> key(title) albums = table [
        {title: "Blue Train", artist: "John Coltrane"},
        {title: "Jeru", artist: "Gerry Mulligan"}
    ];

    resource function get albums() returns Album[] {
        return self.albums.toArray();
    }
    resource function post albums(Album album) {
        self.albums.add(album);
    }
}

// MAIN
public function main() returns error? {

    // 1. Class with final field
    io:println("=== 1. Class with Final Field ===");
    EngineerWithFinal e1 = new EngineerWithFinal("Alice", 52);
    io:println(e1.getName());
    io:println(e1.getAge());

    // 2. Object constructor (anonymous object)
    io:println("\n=== 2. Object Constructor ===");
    var anonEngineer = object {
        string name;
        function init() {
            self.name = "";
        }
        function setName(string name) {
            self.name = name;
        }
        function getName() returns string {
            return self.name;
        }
    };
    anonEngineer.setName("Alice");
    io:println(anonEngineer.getName());

    // 3. Class with default init parameter — explicit & implicit new
    io:println("\n=== 3. Class with Default Init Parameter ===");
    EngineerWithDefault ed1 = new EngineerWithDefault();
    io:println(ed1.getName());
    EngineerWithDefault ed2 = new EngineerWithDefault("Alice");
    io:println(ed2.getName());
    EngineerWithDefault ed3 = new EngineerWithDefault(name = "Bob");
    io:println(ed3.getName());
    EngineerWithDefault ed4 = new;
    io:println(ed4.getName());
    EngineerWithDefault ed5 = new ("Alice");
    io:println(ed5.getName());
    EngineerWithDefault ed6 = new (name = "Bob");
    io:println(ed6.getName());

    // 4. Structural object type (Hashable)
    io:println("\n=== 4. Structural Object Type (Hashable) ===");
    io:println(makeHashable() is Hashable);

    // 5. Object type inclusion (Cloneable → Person → Engineer)
    io:println("\n=== 5. Object Type Inclusion ===");
    Engineer engineer = new Engineer("Alice");
    io:println(engineer.getName());
    Engineer engineerClone = engineer.clone();
    io:println(engineerClone.getName());
    io:println(engineer === engineerClone);

    // 6. Object constructor with closure
    io:println("\n=== 6. Object Constructor with Closure ===");
    string[] names = ["Ana", "Alice", "Bob"];
    var closureEngineer = object {
        string name = "";
        function setName(string name) {
            names.push(name);
            self.name = name;
        }
    };
    closureEngineer.setName("Walter");
    io:println(closureEngineer.name);
    io:println(names);

    // 7. Client class
    io:println("\n=== 7. Client Class ===");
    AlbumClient albumClient = check new ("localhost:9090");
    Album[] albums = check albumClient->/["albums"];
    io:println(albums);
    Album newAlbum = {title: "Sarah Vaughan and Clifford Brown", artist: "Sarah Vaughan"};
    check albumClient->/["albums"].post(newAlbum);

    // 8. Service class
    io:println("\n=== 8. Service Class ===");
    http:Service albumService = new AlbumService();
    check httpListener.attach(albumService);
    check httpListener.'start();
    runtime:registerListener(httpListener);
}
