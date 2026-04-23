import ballerina/http;
import ballerina/mime;

// Types
type Album readonly & record {|
    string title;
    string artist;
|};

// Subtype of http:Conflict carrying a descriptive body
type AlbumConflict record {|
    *http:Conflict;
    record {
        string message;
    } body;
|};

// Data
table<Album> key(title) albums = table [
    {title: "Blue Train", artist: "John Coltrane"},
    {title: "Jeru",       artist: "Gerry Mulligan"}
];

// Service
service / on new http:Listener(9090) {

    // GET /albums
    // Requires Accept: application/json; returns 406 otherwise.
    resource function get albums(@http:Header string accept)
            returns Album[]|http:NotAcceptable {

        if !string:equalsIgnoreCaseAscii(accept, mime:APPLICATION_JSON) {
            return http:NOT_ACCEPTABLE;
        }
        return albums.toArray();
    }

    // POST /albums
    // Returns 409 Conflict if the album already exists.
    resource function post albums(Album album)
            returns Album|AlbumConflict {

        if albums.hasKey(album.title) {
            return {body: {message: "album already exists"}};
        }
        albums.add(album);
        return album;
    }
}
