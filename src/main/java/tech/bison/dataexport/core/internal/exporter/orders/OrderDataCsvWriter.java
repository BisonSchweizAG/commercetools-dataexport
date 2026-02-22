/*
 * Copyright (C) 2000 - 2026 Bison Schweiz AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.bison.dataexport.core.internal.exporter.orders;

import com.commercetools.api.models.common.BaseResource;
import com.commercetools.api.models.order.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.DataWriter;
import tech.bison.dataexport.core.internal.exporter.common.CsvWriterSupport;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

public class OrderDataCsvWriter implements DataWriter {

    private static final String LINE_ITEM_PREFIX = "lineItems.";
    private static final String VARIANT_ATTRIBUTES_PREFIX = "variant.attributes.";
    private final CSVPrinter csvPrinter;
    private final DataExportProperties dataExportProperties;
    private final ObjectMapper objectMapper;

    public OrderDataCsvWriter(CSVPrinter csvPrinter, DataExportProperties dataExportProperties,
                              ObjectMapper objectMapper) {
        this.csvPrinter = csvPrinter;
        this.dataExportProperties = dataExportProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void writeRow(BaseResource source) {
        Order order = (Order) source;
        JsonNode node = objectMapper.valueToTree(source);

        var orderFields = CsvWriterSupport.topLevelFields(dataExportProperties.fields(), LINE_ITEM_PREFIX);
        var lineItemFields = CsvWriterSupport.childItemFields(dataExportProperties.fields(), LINE_ITEM_PREFIX);
        if (!lineItemFields.isEmpty()) {
            writeRecordsWithLineItems(order, node, orderFields, lineItemFields);
        } else {
            List<String> values = dataExportProperties.fields().stream().map(field -> CsvWriterSupport.extractValue(node, field))
                    .toList();
            writeRecord(order, values);
        }
    }

    private void writeRecordsWithLineItems(Order order, JsonNode orderNode, List<String> orderFields,
                                           List<String> lineItemFields) {
        var orderRecord = CsvWriterSupport.createParentRecord(orderNode, orderFields, lineItemFields.size());
        writeRecord(order, orderRecord);

        for (var lineItem : order.getLineItems()) {
            JsonNode lineItemNode = objectMapper.valueToTree(lineItem);
            var lineItemRecord = CsvWriterSupport.createChildRecord(orderFields.size(), lineItemFields,
                    field -> extractLineItemValue(lineItemNode, field.replace(LINE_ITEM_PREFIX, "")));
            writeRecord(order, lineItemRecord);
        }
    }

    private String extractLineItemValue(JsonNode lineItemNode, String field) {
        if (!field.startsWith(VARIANT_ATTRIBUTES_PREFIX)) {
            return CsvWriterSupport.extractValue(lineItemNode, field);
        }

        String attributePath = field.substring(VARIANT_ATTRIBUTES_PREFIX.length());
        String attributeName = attributePath;
        String nestedPath = null;
        int nestedPathSeparatorIdx = attributePath.indexOf('.');
        if (nestedPathSeparatorIdx >= 0) {
            attributeName = attributePath.substring(0, nestedPathSeparatorIdx);
            nestedPath = attributePath.substring(nestedPathSeparatorIdx + 1);
        }
        final String targetAttributeName = attributeName;
        final String targetNestedPath = nestedPath;

        JsonNode attributes = lineItemNode.at("/variant/attributes");
        return StreamSupport.stream(attributes.spliterator(), false)
                .filter(attribute -> targetAttributeName.equals(attribute.path("name").asText()))
                .findFirst()
                .map(attribute -> {
                    JsonNode attributeValue = attribute.path("value");
                    if (targetNestedPath == null) {
                        return CsvWriterSupport.extractNodeValue(attributeValue);
                    }
                    return CsvWriterSupport.extractValue(attributeValue, targetNestedPath);
                })
                .orElse("");
    }

    private void writeRecord(Order order, List<String> values) {
        try {
            csvPrinter.printRecord(values);
        } catch (IOException e) {
            throw new DataExportException(String.format("Could not write order '%s'", order.getId()), e);
        }
    }

    @Override
    public void flush() {
        try {
            csvPrinter.flush();
        } catch (IOException e) {
            throw new DataExportException("Could not flush order csv writer.", e);
        }
    }
}
