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
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.executor.ExportableResourceType;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderDataCsvWriterTest {

    @Test
    void writeRow_simpleTopLevelFields_printCsvRecord() throws IOException {
        var csvPrinter = mock(CSVPrinter.class);
        var properties = new DataExportProperties(ExportableResourceType.ORDER, List.of("orderNumber", "customerId", "createdAt"));
        var csvDataWriter = new OrderDataCsvWriter(csvPrinter, properties);

        var order = Order.builder()
                .id("order-id")
                .orderNumber("12345")
                .customerId("customer-id")
                .createdAt(ZonedDateTime.of(2026, 12, 20, 10, 0, 0, 0, ZoneId.of("UTC")))
                .buildUnchecked();

        csvDataWriter.writeRow(order);

        verify(csvPrinter).printRecord(List.of("12345", "customer-id", "2026-12-20T10:00:00.000Z"));
    }

    @Test
    void writeRow_centPrecisionPriceField_printCsvRecord() throws IOException {
        var csvPrinter = mock(CSVPrinter.class);
        var properties = new DataExportProperties(ExportableResourceType.ORDER, List.of("totalPrice"));
        var csvDataWriter = new OrderDataCsvWriter(csvPrinter, properties);

        var order = Order.builder()
                .totalPrice(CentPrecisionMoney.builder().centAmount(195L).currencyCode("CHF").fractionDigits(2).buildUnchecked())
                .buildUnchecked();

        csvDataWriter.writeRow(order);

        verify(csvPrinter).printRecord(List.of("1.95"));
    }
}