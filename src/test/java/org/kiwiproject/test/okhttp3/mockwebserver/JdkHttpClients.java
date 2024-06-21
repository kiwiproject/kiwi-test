package org.kiwiproject.test.okhttp3.mockwebserver;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

@UtilityClass
class JdkHttpClients {

    static HttpResponse<String> get(HttpClient client, String uri) {
        return get(client, URI.create(uri));
    }

    static HttpResponse<String> get(HttpClient client, URI uri) {
        return send(client, HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build());
    }

    static HttpResponse<String> post(HttpClient client, URI uri, String body) {
        return send(client, HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(body))
                .uri(uri)
                .build());
    }

    static HttpResponse<String> put(HttpClient client, URI uri, String body) {
        return send(client, HttpRequest.newBuilder()
                .PUT(BodyPublishers.ofString(body))
                .uri(uri)
                .build());
    }

    static HttpResponse<String> delete(HttpClient client, URI uri) {
        return send(client, HttpRequest.newBuilder()
                .DELETE()
                .uri(uri)
                .build());
    }

    static HttpResponse<String> send(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
