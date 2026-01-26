import ballerina/http;

// HTTP listener on port 8080
listener http:Listener httpListener = check new (8080);

// Sample record types
type User record {|
    int id;
    string name;
    string email;
|};

type CreateUserRequest record {|
    string name;
    string email;
|};

type ErrorResponse record {|
    string message;
|};

// In-memory storage for demo
map<User> users = {};
int nextId = 1;

// REST API service
service /api on httpListener {

    // GET /api/users - Get all users
    resource function get users() returns User[]|error {
        return users.toArray();
    }

    // GET /api/users/{id} - Get user by ID
    resource function get users/[int id]() returns User|http:NotFound|error {
        User? user = users[id.toString()];
        if user is () {
            return <http:NotFound>{
                body: {
                    message: string `User with id ${id} not found`
                }
            };
        }
        return user;
    }

    // POST /api/users - Create a new user
    resource function post users(CreateUserRequest payload) returns User|error {
        User newUser = {
            id: nextId,
            name: payload.name,
            email: payload.email
        };
        users[nextId.toString()] = newUser;
        nextId = nextId + 1;
        return newUser;
    }

    // GET /api/search - Search users by name (query parameter example)
    resource function get search(@http:Query string name) returns User[]|error {
        User[] results = from User user in users
            where user.name.toLowerAscii().includes(name.toLowerAscii())
            select user;
        return results;
    }

    // GET /api/health - Health check endpoint
    resource function get health() returns string {
        return "Service is running";
    }
}
