import ballerina/grpc;
import ballerina/time;

listener grpc:Listener userListener = new (9091);

@grpc:Descriptor {value: USER_DESC}
service "UserService" on userListener {

    private map<UserResponse> userStore = {};

    remote function GetUser(UserRequest userRequest) returns UserResponse|error {
        string userId = userRequest.user_id;
        UserResponse? userResponse = self.userStore[userId];
        if userResponse is UserResponse {
            return userResponse;
        }
        return error("User not found with ID: " + userId);
    }

    remote function CreateUser(CreateUserRequest createRequest) returns UserResponse|error {
        string userId = "USER_" + self.userStore.length().toString();
        time:Utc currentTime = time:utcNow();
        string createdAt = time:utcToString(currentTime);

        UserResponse newUser = {
            user_id: userId,
            name: createRequest.name,
            email: createRequest.email,
            created_at: createdAt
        };

        self.userStore[userId] = newUser;
        return newUser;
    }

    remote function ListUsers(Empty emptyRequest) returns UserListResponse|error {
        UserResponse[] userList = self.userStore.toArray();
        return {users: userList};
    }
}

public type UserRequest record {|
    string user_id = "";
|};

public type CreateUserRequest record {|
    string name = "";
    string email = "";
|};

public type UserResponse record {|
    string user_id = "";
    string name = "";
    string email = "";
    string created_at = "";
|};

public type UserListResponse record {|
    UserResponse[] users = [];
|};

public type Empty record {|
|};

const string USER_DESC = "0A0A757365722E70726F746F120B75736572736572766963651A1E676F6F676C652F70726F746F6275662F77726170706572732E70726F746F22230A0B55736572526571756573741214120775736572X69641801220009520775736572496422420A1143726561746555736572526571756573741212120A6E616D651801220009520A6E616D651214120B656D61696C1802220009520B656D61696C22690A0C55736572526573706F6E73651214120775736572X69641801220009520775736572496412121210A6E616D651802220009520A6E616D651214120B656D61696C1803220009520B656D61696C121A120A63726561746564X61741804220009520A637265617465644174223F0A1055736572X4C697374526573706F6E7365122B120575736572731801220009520575736572731A1C0A0C55736572526573706F6E736512020801220009520575736572732207120A456D70747932C2010A0B55736572536572766963651243120747657455736572121255736572526571756573741A0C55736572526573706F6E7365220009124B120A43726561746555736572121143726561746555736572526571756573741A0C55736572526573706F6E7365220009123C120955736572X4C6973741205456D7074791A1055736572X4C697374526573706F6E7365220009620670726F746F33";
