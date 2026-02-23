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

import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.customer.Customer;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.executor.ExportableResourceType;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerDataCsvWriterTest {

    @Captor
    private ArgumentCaptor<List<String>> rowCaptor;

    @Test
    void writeRow_simpleTopLevelFields_printCsvRecord() throws IOException {
        var csvPrinter = mock(CSVPrinter.class);
        var properties = new DataExportProperties(ExportableResourceType.CUSTOMER, List.of("id", "email", "customerNumber"));
        var csvDataWriter = new CustomerDataCsvWriter(csvPrinter, properties, JsonUtils.createObjectMapper());

        var customer = Customer.builder()
                .id("customer-id")
                .email("john.doe@example.com")
                .customerNumber("1001")
                .addresses()
                .billingAddressIds()
                .shippingAddressIds()
                .buildUnchecked();

        csvDataWriter.writeRow(customer);

        verify(csvPrinter).printRecord(List.of("customer-id", "john.doe@example.com", "1001"));
    }

    @Test
    void writeRow_withAddressesFields_printParentAndAddressChildRecords() throws IOException {
        var csvPrinter = mock(CSVPrinter.class);
        var properties = new DataExportProperties(ExportableResourceType.CUSTOMER,
                List.of("id", "email", "addresses.postalCode", "addresses.city"));
        var csvDataWriter = new CustomerDataCsvWriter(csvPrinter, properties, JsonUtils.createObjectMapper());

        var customer = Customer.builder()
                .id("customer-id")
                .email("john.doe@example.com")
                .addresses(
                        Address.builder().postalCode("8000").city("Zurich").buildUnchecked(),
                        Address.builder().postalCode("3000").city("Bern").buildUnchecked())
                .billingAddressIds()
                .shippingAddressIds()
                .buildUnchecked();

        doNothing().when(csvPrinter).printRecord(rowCaptor.capture());

        csvDataWriter.writeRow(customer);

        assertThat(rowCaptor.getAllValues().get(0)).isEqualTo(List.of("customer-id", "john.doe@example.com", "", ""));
        assertThat(rowCaptor.getAllValues().get(1)).isEqualTo(List.of("", "", "8000", "Zurich"));
        assertThat(rowCaptor.getAllValues().get(2)).isEqualTo(List.of("", "", "3000", "Bern"));
    }
}
