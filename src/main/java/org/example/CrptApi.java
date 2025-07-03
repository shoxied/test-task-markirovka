package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final String token;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final Semaphore semaphore;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public CrptApi(TimeUnit timeUnit, int requestLimit, String token){
        this.timeUnit = timeUnit;
        this.semaphore = new Semaphore(requestLimit);
        this.token = token;
    }

    public String createDocument(Object document, String signature) throws InterruptedException, IOException {

        semaphore.acquire();

        executorService.schedule(() -> semaphore.release(), 1, timeUnit);

        RequestBody requestBody = new RequestBody();
        String product_document = Base64.getEncoder().encodeToString(mapper.writeValueAsString(document).getBytes());
        requestBody.setProduct_document(product_document);
        requestBody.setSignature(signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200){
            throw new IOException("Failed to create document, " + response.body());
        }

        return response.body();
    }

    public static class RequestBody{
        private String document_format = "MANUAL";
        private String product_document;
        private String signature;
        private String type = "LP_INTRODUCE_GOODS";

        public String getDocument_format() {
            return document_format;
        }

        public void setDocument_format(String document_format) {
            this.document_format = document_format;
        }

        public String getProduct_document() {
            return product_document;
        }

        public void setProduct_document(String product_document) {
            this.product_document = product_document;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
