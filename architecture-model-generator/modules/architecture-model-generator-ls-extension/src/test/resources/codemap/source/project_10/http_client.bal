import ballerina/http;
import ballerina/io;
import ballerina/mime;
import ballerina/constraint;

// Type

type MusicAlbum record {
    @constraint:String {
        maxLength: 5,
        minLength: 1
    }
    string title;
    string artist;
};

// Client helper
final http:Client albumClient = check new ("localhost:9090");

// 1. GET /albums  (plain array binding)

function getAlbums() returns MusicAlbum[]|error {
    MusicAlbum[] albums = check albumClient->/albums;
    io:println("GET /albums: " + albums.toJsonString());
    io:println("First artist: " + albums[0].artist);
    return albums;
}

// 2. GET /albums?artist=  (query parameter)

function getAlbumsByArtistQuery(string artist) returns MusicAlbum[]|error {
    MusicAlbum[] albums = check albumClient->/albums(artist = artist);
    io:println("GET /albums?artist=" + artist + ": " + albums.toJsonString());
    return albums;
}

// 3. GET /albums/[artist]  (path parameter)

function getAlbumByPathParam(string artist) returns MusicAlbum|error {
    MusicAlbum album = check albumClient->/albums/[artist];
    io:println("GET /albums/[" + artist + "]: " + album.toJsonString());
    return album;
}

// 4. GET /albums  (with Accept header)

function getAlbumsWithAcceptHeader() returns MusicAlbum[]|error {
    MusicAlbum[] albums = check albumClient->/albums({
        Accept: mime:APPLICATION_JSON
    });
    io:println("GET /albums (Accept: JSON): " + albums.toJsonString());
    return albums;
}

// 5. POST /albums  (plain post)

function postAlbum() returns MusicAlbum|error {
    MusicAlbum album = check albumClient->/albums.post({
        title: "Sarah Vaughan and Clifford Brown",
        artist: "Sarah Vaughan"
    });
    io:println("POST /albums: " + album.toJsonString());
    return album;
}

// 6. POST /albums  (with constraint validation)

function postAlbumWithConstraint() returns MusicAlbum|error {
    MusicAlbum album = check albumClient->/albums.post({
        title: "Blue Train",   // exceeds maxLength:5 on response binding
        artist: "John Coltrane"
    });
    io:println("POST /albums (constrained): " + album.toJsonString());
    return album;
}

// Entry point

public function main(string artist = "Blue Train") returns error? {
    _ = check getAlbums();
    _ = check getAlbumsByArtistQuery("John Coltrane");
    _ = check getAlbumByPathParam(artist);
    _ = check getAlbumsWithAcceptHeader();
    _ = check postAlbum();
    _ = check postAlbumWithConstraint();
}
