package io.quarkus.devui.tests;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DevUIJsonRPCTest {

    protected static final Logger log = Logger.getLogger(DevUIJsonRPCTest.class);

    private final URI websocketUri;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();
    private final Random random = new Random();

    private final String namespace;

    private final HttpClient httpClient;

    public DevUIJsonRPCTest(String namespace) {
        this(namespace, ConfigProvider.getConfig().getValue("test.url", String.class));
    }

    public DevUIJsonRPCTest(String namespace, String testUrl) {
        this.namespace = namespace;
        String nonApplicationRoot = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.http.non-application-root-path", String.class).orElse("q");
        if (!nonApplicationRoot.startsWith("/")) {
            nonApplicationRoot = "/" + nonApplicationRoot;
        }
        websocketUri = URI.create("ws" + testUrl.substring(4) + nonApplicationRoot + "/dev-ui/json-rpc-ws");

        // we used to rely on the Vert.x HTTP client here but this is a problem for dev mode tests
        // as we ended up attempting to load the Vert.x classes from the root app class loader
        // while they were already loaded by one of the child QuarkusClassLoader
        // while imperfect, relying on the JDK HttpClient is safer
        this.httpClient = HttpClient.newHttpClient();
    }

    public <T> T executeJsonRPCMethod(TypeReference typeReference, String methodName) throws Exception {
        return executeJsonRPCMethod(typeReference, methodName, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T executeJsonRPCMethod(TypeReference typeReference, String methodName, Map<String, Object> params)
            throws Exception {
        int id = sendRequest(methodName, params);
        T response = getJsonRPCResponse(typeReference, id);
        log.debug("response = " + response);
        return response;
    }

    public JsonNode executeJsonRPCMethod(String methodName) throws Exception {
        return executeJsonRPCMethod(methodName, null);
    }

    public JsonNode executeJsonRPCMethod(String methodName, Map<String, Object> params) throws Exception {
        return executeJsonRPCMethod(JsonNode.class, methodName, params);
    }

    public <T> T executeJsonRPCMethod(Class<T> classType, String methodName) throws Exception {
        return executeJsonRPCMethod(classType, methodName, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T executeJsonRPCMethod(Class<T> classType, String methodName, Map<String, Object> params) throws Exception {

        int id = sendRequest(methodName, params);
        T response = getJsonRPCResponse(classType, id);
        log.debug("response = " + response);

        return response;
    }

    protected JsonNode toJsonNode(String json) {
        try {
            JsonParser parser = factory.createParser(json);
            return mapper.readTree(parser);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private <T> T getJsonRPCResponse(TypeReference typeReference, int id) throws InterruptedException, IOException {
        return getJsonRPCResponse(typeReference, id, 0);
    }

    @SuppressWarnings("unchecked")
    private <T> T getJsonRPCResponse(TypeReference typeReference, int id, int loopCount)
            throws InterruptedException, IOException {
        JsonNode object = objectResultFromJsonRPC(id);
        if (object != null) {
            JavaType jt = mapper.getTypeFactory().constructType(typeReference);
            return (T) mapper.treeToValue(object, jt);
        }
        if (loopCount > 10)
            throw new RuntimeException("Too many recursions, message not returned for id [" + id + "]");
        return getJsonRPCResponse(typeReference, id, loopCount + 1);
    }

    private <T> T getJsonRPCResponse(Class<T> classType, int id) throws InterruptedException, IOException {
        return getJsonRPCResponse(classType, id, 0);
    }

    @SuppressWarnings("unchecked")
    private <T> T getJsonRPCResponse(Class<T> classType, int id, int loopCount) throws InterruptedException, IOException {
        JsonNode object = objectResultFromJsonRPC(id);
        if (object != null) {
            if (classType == null || classType.equals(JsonNode.class)) {
                return (T) object;
            } else if (classType.equals(String.class)) {
                return (T) object.asText();
            } else if (classType.equals(Boolean.class)) {
                return (T) Boolean.valueOf(object.asBoolean());
            } else if (classType.equals(Double.class)) {
                return (T) Double.valueOf(object.asDouble());
            } else if (classType.equals(Integer.class)) {
                return (T) Integer.valueOf(object.asInt());
            } else if (classType.equals(Long.class)) {
                return (T) Long.valueOf(object.asLong());
            } else {
                return mapper.treeToValue(object, classType);
            }
        }
        if (loopCount > 10)
            throw new RuntimeException("Too many recursions, message not returned for id [" + id + "]");
        return getJsonRPCResponse(classType, id, loopCount + 1);
    }

    private JsonNode objectResultFromJsonRPC(int id) throws InterruptedException, JsonProcessingException {
        return objectResultFromJsonRPC(id, 0);
    }

    private JsonNode objectResultFromJsonRPC(int id, int loopCount) throws InterruptedException, JsonProcessingException {
        if (RESPONSES.containsKey(id)) {
            WebSocketResponse response = RESPONSES.remove(id);
            if (response != null) {
                ObjectNode json = (ObjectNode) new ObjectMapper().readTree(response.message());
                JsonNode result = json.get("result");
                if (result != null) {
                    return result.get("object");
                }
            }
            return null;
        } else {
            if (loopCount > 10)
                throw new RuntimeException("Too many recursions, message not returned for id [" + id + "]");

            TimeUnit.SECONDS.sleep(3);
            return objectResultFromJsonRPC(id, loopCount + 1);
        }
    }

    private String createJsonRPCRequest(int id, String methodName, Map<String, Object> params) throws IOException {

        ObjectNode request = mapper.createObjectNode();

        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", this.namespace + "." + methodName);
        ObjectNode jsonParams = mapper.createObjectNode();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> p : params.entrySet()) {
                JsonNode convertValue = mapper.convertValue(p.getValue(), JsonNode.class);
                jsonParams.putIfAbsent(p.getKey(), convertValue);
            }
        }
        request.set("params", jsonParams);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
    }

    private int sendRequest(String methodName, Map<String, Object> params) throws IOException {
        int id = random.nextInt(Integer.MAX_VALUE);
        String request = createJsonRPCRequest(id, methodName, params);
        log.debugf("WebSocket request = %s", request);

        var listener = new WebSocket.Listener() {

            // this is not exactly safe but we don't expect concurrency in our scenario
            private StringBuffer buffer = new StringBuffer();

            @Override
            public CompletionStage<Void> onText(WebSocket webSocket, CharSequence data, boolean last) {
                buffer.append(data);

                if (last) {
                    String fullResponse = buffer.toString();
                    buffer.setLength(0);

                    RESPONSES.put(id, new WebSocketResponse(fullResponse.toString()));
                } else {
                    webSocket.request(1);
                }

                return CompletableFuture.completedFuture(data)
                        .thenAccept(d -> log.debugf("WebSocket response: %s", data));
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                RESPONSES.put(id, new WebSocketResponse(error));

                log.debugf("WebSocket error: %s", error);
            }
        };

        WebSocket webSocket = httpClient.newWebSocketBuilder().buildAsync(this.websocketUri, listener).join();
        webSocket.sendText(request, true).join();

        return id;
    }

    private static final ConcurrentHashMap<Integer, WebSocketResponse> RESPONSES = new ConcurrentHashMap<>();

    private static class WebSocketResponse {
        private final String message;
        private final Throwable throwable;

        public WebSocketResponse(String message) {
            this.message = message;
            this.throwable = null;
        }

        public WebSocketResponse(Throwable throwable) {
            this.message = null;
            this.throwable = throwable;
        }

        String message() {
            if (throwable != null) {
                throw new IllegalStateException("Request failed: " + throwable.getMessage(), throwable);
            }
            return message;
        }
    }
}
