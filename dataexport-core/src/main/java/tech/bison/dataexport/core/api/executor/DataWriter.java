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
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.internal.exporter.customers.CustomerDataCsvWriter;
import tech.bison.dataexport.core.internal.exporter.orders.OrderDataCsvWriter;

public interface DataWriter {
    void writeRow(BaseResource object);

    static DataWriter csv(DataExportProperties dataExportProperties, CSVPrinter csvPrinter) {
        var objectMapper = JsonUtils.createObjectMapper();
        return switch (dataExportProperties.resourceType()) {
            case ORDER -> new OrderDataCsvWriter(csvPrinter, dataExportProperties, objectMapper);
            case CUSTOMER -> new CustomerDataCsvWriter(csvPrinter, dataExportProperties, objectMapper);
            default ->
                    throw new IllegalArgumentException("Unsupported resource type: " + dataExportProperties.resourceType());
        };
    }
}
