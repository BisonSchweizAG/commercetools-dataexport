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
package tech.bison.dataexport.core.api.executor;

import com.commercetools.api.models.common.BaseResource;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.internal.exporter.customers.CustomerDataCsvWriter;
import tech.bison.dataexport.core.internal.exporter.orders.OrderDataCsvWriter;

public interface DataWriter {

  void writeRow(BaseResource object);

  default void flush() {
    // default no-op for non-CSV implementations
  }

  static DataWriter csv(DataExportProperties dataExportProperties, OutputStream outputStream) {
    try {
      var csvPrinter = new CSVPrinter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
          CSVFormat.DEFAULT.builder().setHeader(dataExportProperties.fields().toArray(new String[0])).get());
      var objectMapper = JsonUtils.createObjectMapper();
      return switch (dataExportProperties.resourceType()) {
        case ORDER -> new OrderDataCsvWriter(csvPrinter, dataExportProperties, objectMapper);
        case CUSTOMER -> new CustomerDataCsvWriter(csvPrinter, dataExportProperties, objectMapper);
      };
    } catch (IOException e) {
      throw new DataExportException("Error creating CSVPrinter.", e);
    }

  }
}
