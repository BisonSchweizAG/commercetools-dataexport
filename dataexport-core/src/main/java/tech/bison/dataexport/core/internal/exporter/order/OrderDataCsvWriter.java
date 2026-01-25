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
package tech.bison.dataexport.core.internal.exporter.order;

import com.commercetools.api.models.common.CentPrecisionMoney;
import com.commercetools.api.models.order.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.executor.DataWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrderDataCsvWriter implements DataWriter<Order> {

    private final CSVPrinter csvPrinter;
    private final DataExportProperties dataExportProperties;
    private final ObjectMapper objectMapper;

    public OrderDataCsvWriter(CSVPrinter csvPrinter, DataExportProperties dataExportProperties) {
        this.csvPrinter = csvPrinter;
        this.dataExportProperties = dataExportProperties;
        this.objectMapper = JsonUtils.createObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void writeRow(Order source) throws IOException {
        JsonNode node = objectMapper.valueToTree(source);
        List<String> values = new ArrayList<>();
        for (String field : dataExportProperties.fields()) {
            String pointer = "/" + field.replace(".", "/");
            JsonNode value = node.at(pointer);
            if (value.get("type") != null && CentPrecisionMoney.CENT_PRECISION.equals(value.get("type").asText())) {
                double amount = value.get("centAmount").asInt() / 100d;
                values.add(String.valueOf(amount));
            } else {
                values.add(value.asText(""));
            }

        }
        csvPrinter.printRecord(values);
    }
}
