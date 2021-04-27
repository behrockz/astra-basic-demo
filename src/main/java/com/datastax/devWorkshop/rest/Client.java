package com.datastax.devWorkshop.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Client {
    private final ObjectMapper mapper;
    private final HttpClient client;

    private final String ASTRA_DB_ID="";
    private final String ASTRA_DB_REGION="";
    private final String ASTRA_DB_KEYSPACE="";
    private final String ASTRA_DB_APPLICATION_TOKEN = "";
    private final String restEndpointTemplate = "https://%s-%s.apps.astra.datastax.com/api/rest/v2/keyspaces/%s";
    private final String restEndPointURI;

    public Client() {
        mapper = new ObjectMapper();
        client = HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        restEndPointURI = String.format(restEndpointTemplate, ASTRA_DB_ID, ASTRA_DB_REGION, ASTRA_DB_KEYSPACE);
    }

    public CompletableFuture<JsonNode> get(String tableName, int docId) throws URISyntaxException {
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(new URI(String.format("%s/%s/%s", restEndPointURI, tableName, docId)))
                .setHeader(HttpHeaders.ACCEPT, "*/*")
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setHeader("x-cassandra-request-id", UUID.randomUUID().toString())
                .setHeader("x-cassandra-token", ASTRA_DB_APPLICATION_TOKEN)
                .GET()
                .build();
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> getJsonNode(response.body()));
    }

    public CompletableFuture<Boolean> insert(String data, String tableName) throws URISyntaxException, JsonProcessingException {
        JsonNode actualObj = mapper.readTree(data);
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(new URI(String.format("%s/%s", restEndPointURI, tableName)))
                .setHeader(HttpHeaders.ACCEPT, "*/*")
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setHeader("x-cassandra-request-id", UUID.randomUUID().toString())
                .setHeader("x-cassandra-token", ASTRA_DB_APPLICATION_TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(actualObj.toString()))
                .build();
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() < 300);
    }

    private JsonNode getJsonNode(String body) {
        try {
            return mapper.readTree(body);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
