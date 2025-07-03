package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi implements Closeable {

    private final TimeUnit timeUnit;
    private final String token;

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final Semaphore semaphore;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public CrptApi(TimeUnit timeUnit, int requestLimit, String token){
        this.timeUnit = timeUnit;
        this.semaphore = new Semaphore(requestLimit);
        this.token = token;
    }

    /**
     *   В конструкторе класса указывается TimeUnit timeUnit, int requestLimit, String token,
     * на вход метода createDocument подается Object document, String signature.
     *   Ограничение на количество запросов было реализовано с помощью Semaphore и ScheduledExecutorService.
     *   Количество доступов к ресурсу определяется requestLimit, который подается в конструктор Semaphore,
     * в executorService.schedule используется semaphore.release() после 1 timeUnit.
     */
    public String createDocument(Object document, String signature) throws InterruptedException, IOException {

        semaphore.acquire();
        executorService.schedule(() -> semaphore.release(), 1, timeUnit);

        RequestBody requestBody = new RequestBody();
        String productDocument = Base64.getEncoder().encodeToString(mapper.writeValueAsString(document).getBytes());
        requestBody.setProductDocument(productDocument);
        requestBody.setSignature(signature);
        String responseBody;

        HttpPost httpPost = new HttpPost(API_URL);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + token);

        StringEntity entity = new StringEntity(mapper.writeValueAsString(requestBody), "UTF-8");
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {

            if(response!=null && response.getEntity()!=null) {
                responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (response.getStatusLine().getStatusCode() != 200){
                    throw new IOException("Failed to create document, " + responseBody);
                } else return responseBody;
            }
            else throw new IOException("Failed to create document, response is null");
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
        executorService.shutdown();
    }

    public static class RequestBody{
        @JsonProperty("document_format")
        private String documentFormat = "MANUAL";
        @JsonProperty("product_document")
        private String productDocument;
        @JsonProperty("signature")
        private String signature;
        @JsonProperty("type")
        private String type = "LP_INTRODUCE_GOODS";

        public String getDocumentFormat() {
            return documentFormat;
        }

        public void setDocumentFormat(String documentFormat) {
            this.documentFormat = documentFormat;
        }

        public String getProductDocument() {
            return productDocument;
        }

        public void setProductDocument(String productDocument) {
            this.productDocument = productDocument;
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
