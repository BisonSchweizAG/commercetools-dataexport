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
import com.commercetools.api.models.common.CentPrecisionMoney;
import com.commercetools.api.models.order.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.DataWriter;

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

    var orderFields = dataExportProperties.fields().stream().filter(field -> !field.startsWith(LINE_ITEM_PREFIX))
        .toList();
    var lineItemFields = dataExportProperties.fields().stream().filter(field -> field.startsWith(LINE_ITEM_PREFIX))
        .toList();
    if (!lineItemFields.isEmpty()) {
      writeRecordsWithLineItems(order, node, orderFields, lineItemFields);
    } else {
      List<String> values = new ArrayList<>();
      for (String field : dataExportProperties.fields()) {
        values.add(extractValue(node, field));
      }
      writeRecord(order, values);
    }
  }

  private void writeRecordsWithLineItems(Order order, JsonNode orderNode, List<String> orderFields,
      List<String> lineItemFields) {
    var orderRecord = Stream.concat(
        orderFields.stream().map(f -> extractValue(orderNode, f)),
        Collections.nCopies(lineItemFields.size(), "").stream()
    ).toList();
    writeRecord(order, orderRecord);

    for (var lineItem : order.getLineItems()) {
      JsonNode lineItemNode = objectMapper.valueToTree(lineItem);
      var lineItemRecord = Stream.concat(
          Collections.nCopies(orderFields.size(), "").stream(),
          lineItemFields.stream().map(f -> extractLineItemValue(lineItemNode, f.replace(LINE_ITEM_PREFIX, "")))
      ).toList();
      writeRecord(order, lineItemRecord);
    }
  }

  private String extractLineItemValue(JsonNode lineItemNode, String field) {
    if (!field.startsWith(VARIANT_ATTRIBUTES_PREFIX)) {
      return extractValue(lineItemNode, field);
    }

    String attributePath = field.substring(VARIANT_ATTRIBUTES_PREFIX.length());
    if (attributePath.isBlank()) {
      return "";
    }

    String attributeName = attributePath;
    String nestedPath = null;
    int nestedPathSeparatorIdx = attributePath.indexOf('.');
    if (nestedPathSeparatorIdx >= 0) {
      attributeName = attributePath.substring(0, nestedPathSeparatorIdx);
      nestedPath = attributePath.substring(nestedPathSeparatorIdx + 1);
    }

    JsonNode attributes = lineItemNode.at("/variant/attributes");

    for (JsonNode attribute : attributes) {
      if (!attributeName.equals(attribute.path("name").asText())) {
        continue;
      }

      JsonNode attributeValue = attribute.path("value");
      if (nestedPath == null) {
        return extractNodeValue(attributeValue);
      }
      return extractValue(attributeValue, nestedPath);
    }
    return "";
  }

  private void writeRecord(Order order, List<String> values) {
    try {
      csvPrinter.printRecord(values);
    } catch (IOException e) {
      throw new DataExportException(String.format("Could not write order '%s'", order.getId()), e);
    }
  }

  private String extractValue(JsonNode node, String field) {
    String pointer = "/" + field.replace(".", "/");
    JsonNode value = node.at(pointer);
    return extractNodeValue(value);
  }

  private String extractNodeValue(JsonNode value) {
    if (value.get("type") != null && CentPrecisionMoney.CENT_PRECISION.equals(value.get("type").asText())) {
      double amount = value.get("centAmount").asInt() / 100d;
      return String.valueOf(amount);
    } else {
      return value.asText("");
    }
  }
}
