/*
 * Copyright (C) 2024 Bison Schweiz AG
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

import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.internal.exporter.CustomerDataExporter;
import tech.bison.dataexport.core.internal.exporter.OrderDataExporter;

/**
 * Interface for exporting data.
 */
public interface DataExporter {

    void export(Context context, DataWriter dataWriter);

    static DataExporter from(DataExportProperties dataExportProperties) {
        return switch (dataExportProperties.resourceType()) {
            case ORDER -> new OrderDataExporter();
            case CUSTOMER -> new CustomerDataExporter();
            default ->
                    throw new IllegalArgumentException("Unsupported resource type: " + dataExportProperties.resourceType());
        };
    }
}
