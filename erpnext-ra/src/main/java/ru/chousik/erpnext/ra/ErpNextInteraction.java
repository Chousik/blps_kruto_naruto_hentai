package ru.chousik.erpnext.ra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.Record;
import jakarta.resource.cci.ResourceWarning;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ErpNextInteraction implements Interaction {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final List<String> INSERT_STRIP_KEYS = List.of(
        "name",
        "owner",
        "creation",
        "modified",
        "modified_by",
        "parent",
        "parenttype",
        "parentfield"
    );

    private final ErpNextConnectionImpl connection;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean closed;

    public ErpNextInteraction(ErpNextConnectionImpl connection, String baseUrl, String apiKey, String apiSecret) {
        this.connection = connection;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
        ensureOpen();
        if (!(input instanceof MappedRecord inputRecord)) {
            throw new ResourceException("ERPNext interaction expects a MappedRecord input");
        }

        String operation = stringValue(inputRecord, "operation");
        if (operation == null || operation.isBlank()) {
            throw new ResourceException("ERPNext interaction input does not contain operation");
        }

        return switch (operation) {
            case "ensureCustomer" -> ensureCustomer(inputRecord);
            case "createQuotation" -> createQuotation(inputRecord);
            case "createSalesOrderFromQuotation" -> createSalesOrderFromQuotation(inputRecord);
            case "createSalesInvoiceFromSalesOrder" -> createSalesInvoiceFromSalesOrder(inputRecord);
            default -> throw new ResourceException("Unsupported ERPNext JCA operation '" + operation + "'");
        };
    }

    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {
        Record result = execute(ispec, input);
        if (!(result instanceof MappedRecord resultRecord)) {
            throw new ResourceException("ERPNext interaction returned unsupported record type");
        }
        if (!(output instanceof MappedRecord outputRecord)) {
            throw new ResourceException("ERPNext interaction expects a MappedRecord output");
        }

        outputRecord.clear();
        outputRecord.putAll(resultRecord);
        outputRecord.setRecordName(resultRecord.getRecordName());
        outputRecord.setRecordShortDescription(resultRecord.getRecordShortDescription());
        return true;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public ResourceWarning getWarnings() {
        return null;
    }

    @Override
    public void clearWarnings() {
    }

    private void ensureOpen() throws ResourceException {
        if (closed) {
            throw new ResourceException("ERPNext interaction is closed");
        }
    }

    private Record ensureCustomer(MappedRecord input) throws ResourceException {
        String customerName = requiredStringRecord(input, "customerName");
        Map<String, Object> existing = findCustomerByName(customerName);
        if (existing != null) {
            return documentRecord(existing);
        }

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("customer_name", customerName);
        payload.put("customer_type", stringValue(input, "customerType") == null ? "Individual" : stringValue(input, "customerType"));
        payload.put("customer_group", requiredStringRecord(input, "customerGroup"));
        payload.put("territory", requiredStringRecord(input, "territory"));
        payload.putAll(mapValue(input, "additionalFields"));
        return documentRecord(createResource("Customer", payload, false));
    }

    private Record createQuotation(MappedRecord input) throws ResourceException {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("quotation_to", defaultString(input, "quotationTo", "Customer"));
        payload.put("party_name", requiredStringRecord(input, "partyName"));
        payload.put("company", requiredStringRecord(input, "company"));
        payload.put("transaction_date", requiredStringRecord(input, "transactionDate"));
        payload.put("order_type", defaultString(input, "orderType", "Sales"));

        List<Object> items = new ArrayList<>();
        for (Object item : listValue(input, "items")) {
            Map<String, Object> itemMap = asStringKeyMap(item);
            LinkedHashMap<String, Object> mappedItem = new LinkedHashMap<>();
            mappedItem.put("item_code", requiredStringMap(itemMap, "itemCode"));
            mappedItem.put("qty", itemMap.get("qty"));
            mappedItem.put("rate", itemMap.get("rate"));
            if (itemMap.get("description") != null && !itemMap.get("description").toString().isBlank()) {
                mappedItem.put("description", itemMap.get("description"));
            }
            items.add(mappedItem);
        }
        payload.put("items", items);

        String validTill = stringValue(input, "validTill");
        if (validTill != null && !validTill.isBlank()) {
            payload.put("valid_till", validTill);
        }
        payload.putAll(mapValue(input, "additionalFields"));
        return documentRecord(createResource("Quotation", payload, true));
    }

    private Record createSalesOrderFromQuotation(MappedRecord input) throws ResourceException {
        String quotationName = requiredStringRecord(input, "quotationName");
        String deliveryDate = requiredStringRecord(input, "deliveryDate");
        Map<String, Object> draft = callMethod(
            "erpnext.selling.doctype.quotation.quotation.make_sales_order",
            Map.of("source_name", quotationName)
        );
        Map<String, Object> payload = new LinkedHashMap<>(sanitizeDocumentForInsert(draft));
        payload.put("delivery_date", deliveryDate);
        Object itemsValue = payload.get("items");
        if (itemsValue instanceof List<?> items) {
            List<Object> updatedItems = new ArrayList<>();
            for (Object item : items) {
                Map<String, Object> itemMap = new LinkedHashMap<>(asStringKeyMap(item));
                itemMap.put("delivery_date", deliveryDate);
                updatedItems.add(itemMap);
            }
            payload.put("items", updatedItems);
        }
        return documentRecord(createResource("Sales Order", payload, true));
    }

    private Record createSalesInvoiceFromSalesOrder(MappedRecord input) throws ResourceException {
        String salesOrderName = requiredStringRecord(input, "salesOrderName");
        Map<String, Object> draft = callMethod(
            "erpnext.selling.doctype.sales_order.sales_order.make_sales_invoice",
            Map.of("source_name", salesOrderName)
        );
        return documentRecord(createResource("Sales Invoice", sanitizeDocumentForInsert(draft), true));
    }

    private Map<String, Object> findCustomerByName(String customerName) throws ResourceException {
        try {
            String fields = objectMapper.writeValueAsString(List.of("name", "doctype", "docstatus"));
            String filters = objectMapper.writeValueAsString(List.of(List.of("customer_name", "=", customerName)));
            JsonNode response = executeJson(
                "search Customer by customer_name",
                "GET",
                buildUri(
                    "/api/resource/Customer",
                    Map.of(
                        "fields", fields,
                        "filters", filters,
                        "limit_page_length", "1"
                    )
                ),
                null
            );
            JsonNode data = requirePayload(response, "data");
            if (!data.isArray() || data.isEmpty()) {
                return null;
            }
            return nodeToMap(data.get(0));
        } catch (JsonProcessingException ex) {
            throw new ResourceException("Failed to serialize ERPNext query", ex);
        }
    }

    private Map<String, Object> createResource(String doctype, Map<String, Object> payload, boolean submit) throws ResourceException {
        JsonNode created = executeJson(
            "create " + doctype,
            "POST",
            buildUri("/api/resource/" + encodePathSegment(doctype), Map.of()),
            payload
        );
        JsonNode createdDoc = requirePayload(created, "data");
        String name = createdDoc.path("name").asText();
        if (name == null || name.isBlank()) {
            throw new ResourceException("ERPNext create " + doctype + " returned blank document name");
        }
        if (!submit) {
            return nodeToMap(createdDoc);
        }
        submitResource(doctype, name);
        return getDocument(doctype, name);
    }

    private Map<String, Object> getDocument(String doctype, String name) throws ResourceException {
        JsonNode node = executeJson(
            "read " + doctype + "/" + name,
            "GET",
            buildUri("/api/resource/" + encodePathSegment(doctype) + "/" + encodePathSegment(name), Map.of()),
            null
        );
        return nodeToMap(requirePayload(node, "data"));
    }

    private void submitResource(String doctype, String name) throws ResourceException {
        executeJson(
            "submit " + doctype + "/" + name,
            "POST",
            buildUri("/api/resource/" + encodePathSegment(doctype) + "/" + encodePathSegment(name), Map.of()),
            Map.of("run_method", "submit")
        );
    }

    private Map<String, Object> callMethod(String methodPath, Map<String, Object> params) throws ResourceException {
        Map<String, String> query = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                query.put(entry.getKey(), entry.getValue().toString());
            }
        }
        JsonNode response = executeJson(
            "call " + methodPath,
            "GET",
            buildUri("/api/method/" + methodPath, query),
            null
        );
        return nodeToMap(requirePayload(response, "message"));
    }

    private JsonNode executeJson(String operation, String method, URI uri, Map<String, Object> payload) throws ResourceException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .header("Authorization", "token " + apiKey + ":" + apiSecret);

            if ("POST".equals(method)) {
                builder.header("Content-Type", "application/json");
                String body = payload == null ? "{}" : objectMapper.writeValueAsString(payload);
                builder.POST(HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.GET();
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = "ERPNext " + operation + " failed with HTTP " + response.statusCode();
                if (response.body() != null && !response.body().isBlank()) {
                    message += ": " + response.body();
                }
                throw new ResourceException(message);
            }
            if (response.body() == null || response.body().isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new ResourceException("ERPNext " + operation + " failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResourceException("ERPNext " + operation + " was interrupted", ex);
        }
    }

    private URI buildUri(String path, Map<String, String> queryParams) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        String uri = baseUrl + path + (query.isEmpty() ? "" : "?" + query);
        return URI.create(uri);
    }

    @SuppressWarnings("unchecked")
    private MappedRecord documentRecord(Map<String, Object> document) {
        SimpleMappedRecord record = new SimpleMappedRecord("erpnextDocument");
        record.put("doctype", document.getOrDefault("doctype", "").toString());
        record.put("name", document.getOrDefault("name", "").toString());
        Object docstatus = document.get("docstatus");
        if (docstatus instanceof Number number) {
            record.put("docstatus", number.intValue());
        } else if (docstatus instanceof String value && !value.isBlank()) {
            record.put("docstatus", Integer.parseInt(value));
        }
        record.put("payload", document);
        return record;
    }

    private Map<String, Object> sanitizeDocumentForInsert(Map<String, Object> document) throws ResourceException {
        Object sanitized = sanitizeValue(document);
        if (!(sanitized instanceof Map<?, ?> sanitizedMap)) {
            throw new ResourceException("ERPNext mapped document has unexpected structure");
        }

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : sanitizedMap.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? null : entry.getKey().toString();
                if (key == null || key.startsWith("__") || INSERT_STRIP_KEYS.contains(key)) {
                    continue;
                }
                sanitized.put(key, sanitizeValue(entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : list) {
                sanitized.add(sanitizeValue(item));
            }
            return sanitized;
        }
        return value;
    }

    private JsonNode requirePayload(JsonNode root, String key) throws ResourceException {
        JsonNode payload = root.path(key);
        if (payload.isMissingNode() || payload.isNull()) {
            throw new ResourceException("ERPNext response does not contain '" + key + "'");
        }
        return payload;
    }

    private Map<String, Object> nodeToMap(JsonNode node) {
        return objectMapper.convertValue(node, MAP_TYPE);
    }

    private Map<String, Object> mapValue(MappedRecord record, String key) throws ResourceException {
        Object value = record.get(key);
        if (value == null) {
            return Map.of();
        }
        return asStringKeyMap(value);
    }

    private List<?> listValue(MappedRecord record, String key) throws ResourceException {
        Object value = record.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        throw new ResourceException("ERPNext interaction value '" + key + "' must be a list");
    }

    private Map<String, Object> asStringKeyMap(Object value) throws ResourceException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new ResourceException("ERPNext interaction expected nested map value");
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    private String requiredStringRecord(MappedRecord record, String key) throws ResourceException {
        String value = stringValue(record, key);
        if (value == null || value.isBlank()) {
            throw new ResourceException("ERPNext interaction value '" + key + "' is required");
        }
        return value;
    }

    private String requiredStringMap(Map<String, Object> record, String key) throws ResourceException {
        Object value = record.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ResourceException("ERPNext interaction value '" + key + "' is required");
        }
        return value.toString();
    }

    private String defaultString(MappedRecord record, String key, String defaultValue) {
        String value = stringValue(record, key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String stringValue(MappedRecord record, String key) {
        Object value = record.get(key);
        return value == null ? null : value.toString();
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
