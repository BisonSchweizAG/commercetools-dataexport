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
package tech.bison.dataexport.core.internal.exporter.customers;

import com.commercetools.api.models.common.BaseResource;
import com.commercetools.api.models.customer.Customer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.DataWriter;
import tech.bison.dataexport.core.internal.exporter.common.CsvWriterSupport;

import java.io.IOException;
import java.util.List;

public class CustomerDataCsvWriter implements DataWriter {

    private static final String ADDRESS_PREFIX = "addresses.";
    private final CSVPrinter csvPrinter;
    private final DataExportProperties dataExportProperties;
    private final ObjectMapper objectMapper;

    public CustomerDataCsvWriter(CSVPrinter csvPrinter, DataExportProperties dataExportProperties, ObjectMapper objectMapper) {
        this.csvPrinter = csvPrinter;
        this.dataExportProperties = dataExportProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void writeRow(BaseResource source) {
        Customer customer = (Customer) source;
        JsonNode customerNode = objectMapper.valueToTree(source);

        var customerFields = CsvWriterSupport.topLevelFields(dataExportProperties.fields(), ADDRESS_PREFIX);
        var addressFields = CsvWriterSupport.childItemFields(dataExportProperties.fields(), ADDRESS_PREFIX);
        if (!addressFields.isEmpty()) {
            writeRecordsWithAddresses(customer, customerNode, customerFields, addressFields);
        } else {
            List<String> values = dataExportProperties.fields().stream()
                    .map(field -> CsvWriterSupport.extractValue(customerNode, field))
                    .toList();
            writeRecord(customer, values);
        }
    }

    private void writeRecordsWithAddresses(Customer customer, JsonNode customerNode, List<String> customerFields,
                                           List<String> addressFields) {
        var customerRecord = CsvWriterSupport.createParentRecord(customerNode, customerFields, addressFields.size());
        writeRecord(customer, customerRecord);

        for (var address : customer.getAddresses()) {
            JsonNode addressNode = objectMapper.valueToTree(address);
            var addressRecord = CsvWriterSupport.createChildRecord(customerFields.size(), addressFields,
                    field -> CsvWriterSupport.extractValue(addressNode, field.replace(ADDRESS_PREFIX, "")));
            writeRecord(customer, addressRecord);
        }
    }

    private void writeRecord(Customer customer, List<String> values) {
        try {
            csvPrinter.printRecord(values);
        } catch (IOException e) {
            throw new DataExportException(String.format("Could not write customer '%s'", customer.getId()), e);
        }
    }

    @Override
    public void flush() {
        try {
            csvPrinter.flush();
        } catch (IOException e) {
            throw new DataExportException("Could not flush customer csv writer.", e);
        }
    }
}
