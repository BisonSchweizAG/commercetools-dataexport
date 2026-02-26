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
package tech.bison.dataexport.core.api.configuration;

import com.commercetools.api.client.ProjectApiRoot;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.DataExport;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.DataExportExecution;
import tech.bison.dataexport.core.api.executor.DataExporter;
import tech.bison.dataexport.core.api.executor.DataWriterProvider;
import tech.bison.dataexport.core.api.executor.ExportableResourceType;
import tech.bison.dataexport.core.api.upload.ExportDataUploader;
import tech.bison.dataexport.core.internal.exporter.customers.CustomerDataCsvWriter;
import tech.bison.dataexport.core.internal.exporter.customers.CustomerDataExporter;
import tech.bison.dataexport.core.internal.exporter.orders.OrderDataCsvWriter;
import tech.bison.dataexport.core.internal.exporter.orders.OrderDataExporter;
import tech.bison.dataexport.core.internal.storage.gcp.GcpFileUploader;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;


public class FluentConfiguration implements Configuration {

    private CommercetoolsProperties apiProperties;
    private ProjectApiRoot projectApiRoot;
    private Clock clock;
    private String outputFileExtension = "csv";
    private Integer maxRecordsPerUpload;
    private final List<ExportDataUploader> uploaderList = new ArrayList<>();
    private final Map<String, DataExportExecution> dataExportExecutionMap = new HashMap<>();

    /**
     * @return The new fully-configured DataExport instance.
     */
    public DataExport load() {
        validateConfiguration();
        return new DataExport(this);
    }

    private void validateConfiguration() {
        if (projectApiRoot == null && apiProperties == null) {
            throw new DataExportException(
                    "Missing commercetools api configuration. Either use withApiProperties() or withApiRoot().");
        }
        if (dataExportExecutionMap.isEmpty()) {
            throw new DataExportException("At least one export type must be configured.");
        }
        if (dataExportExecutionMap.entrySet().stream()
                .anyMatch(entry -> !isCustomExport(entry.getKey()) && entry.getValue().dataExportProperties().fields().isEmpty())) {
            throw new DataExportException("At least one export type has no fields configured.");
        }
        if (uploaderList.isEmpty()) {
            throw new DataExportException("No data export uploader configured.");
        }
        if (maxRecordsPerUpload != null && maxRecordsPerUpload < 1) {
            throw new DataExportException("maxRecordsPerUpload must be greater than 0.");
        }
    }

    private boolean isCustomExport(String key) {
        return Arrays.stream(ExportableResourceType.values())
                .map(ExportableResourceType::getPluralName)
                .noneMatch(name -> name.equals(key));
    }

    /**
     * Configure the commercetools api with properties.
     */
    public FluentConfiguration withApiProperties(CommercetoolsProperties apiProperties) {
        this.apiProperties = apiProperties;
        return this;
    }

    /**
     * Configure the commercetools api with the given api root.
     */
    public FluentConfiguration withApiRoot(ProjectApiRoot projectApiRoot) {
        this.projectApiRoot = projectApiRoot;
        return this;
    }

    /**
     * Configure an uploader for the exported data.
     */
    public FluentConfiguration withUploader(ExportDataUploader exportDataUploader) {
        Objects.requireNonNull(exportDataUploader, "exportDataUploader must not be null.");
        uploaderList.add(exportDataUploader);
        return this;
    }

    /**
     * Configure GCP Cloud Storage uploader with the given properties.
     */
    public FluentConfiguration withGcpCloudStorageProperties(GcpCloudStorageProperties gcpCloudStorageProperties) {
        Objects.requireNonNull(gcpCloudStorageProperties, "gcpCloudStorageProperties must not be null.");
        withUploader(new GcpFileUploader(gcpCloudStorageProperties));
        return this;
    }

    /**
     * Configures the fields to be exported for the given resource types.
     */
    public FluentConfiguration withExportFields(ExportableResourceType resourceType, List<String> exportFields) {
        Objects.requireNonNull(resourceType, "resourceType must not be null.");
        registerDataExportExecution(resourceType.getPluralName(), new DataExportProperties(exportFields),
                createDataExporter(resourceType),
                createDataWriterProvider(resourceType));
        return this;
    }

    /**
     * Configures a custom exporter implementation and writer provider for the given export key.
     */
    public FluentConfiguration withCustomExporter(String exportKey, DataExporter dataExporter, DataWriterProvider dataWriterProvider) {
        String normalizedExportKey = normalizeExportKey(exportKey);
        if (!isCustomExport(normalizedExportKey)) {
            throw new DataExportException(
                    String.format("Custom exporter key '%s' is reserved for built-in exports.", normalizedExportKey));
        }
        registerDataExportExecution(normalizedExportKey, new DataExportProperties(List.of()), dataExporter,
                dataWriterProvider);
        return this;
    }

    private DataExporter createDataExporter(ExportableResourceType resourceType) {
        return switch (resourceType) {
            case ORDER -> new OrderDataExporter();
            case CUSTOMER -> new CustomerDataExporter();
        };
    }

    private DataWriterProvider createDataWriterProvider(ExportableResourceType resourceType) {
        return (fields, outputStream) -> {
            try {
                var csvPrinter = new CSVPrinter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                        CSVFormat.DEFAULT.builder().setHeader(fields.toArray(new String[0]))
                                .get());
                var objectMapper = JsonUtils.createObjectMapper();
                return switch (resourceType) {
                    case ORDER -> new OrderDataCsvWriter(csvPrinter, fields, objectMapper);
                    case CUSTOMER -> new CustomerDataCsvWriter(csvPrinter, fields, objectMapper);
                };
            } catch (IOException ex) {
                throw new DataExportException("Error creating CSVPrinter.", ex);
            }
        };
    }

    private void registerDataExportExecution(String exportKey, DataExportProperties dataExportProperties,
                                             DataExporter dataExporter,
                                             DataWriterProvider dataWriterProvider) {
        String normalizedExportKey = normalizeExportKey(exportKey);
        Objects.requireNonNull(dataExportProperties, "dataExportProperties must not be null.");
        Objects.requireNonNull(dataExporter, "dataExporter must not be null.");
        Objects.requireNonNull(dataWriterProvider, "dataWriterProvider must not be null.");
        this.dataExportExecutionMap.put(normalizedExportKey, new DataExportExecution(dataExportProperties,
                dataExporter, dataWriterProvider));
    }

    private String normalizeExportKey(String exportKey) {
        String normalizedExportKey = Objects.requireNonNull(exportKey, "exportKey must not be null.").trim();
        if (normalizedExportKey.isEmpty()) {
            throw new IllegalArgumentException("exportKey must not be blank.");
        }
        return normalizedExportKey;
    }


    public FluentConfiguration withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public FluentConfiguration withOutputFileExtension(String outputFileExtension) {
        this.outputFileExtension = Objects.requireNonNull(outputFileExtension, "outputFileExtension must not be null.");
        return this;
    }

    public FluentConfiguration withMaxRecordsPerUpload(Integer maxRecordsPerUpload) {
        this.maxRecordsPerUpload = maxRecordsPerUpload;
        return this;
    }


    @Override
    public CommercetoolsProperties getApiProperties() {
        return apiProperties;
    }

    @Override
    public ProjectApiRoot getApiRoot() {
        return projectApiRoot;
    }


    @Override
    public Clock getClock() {
        return clock;
    }

    @Override
    public String getOutputFileExtension() {
        return outputFileExtension;
    }

    @Override
    public Integer getMaxRecordsPerUpload() {
        return maxRecordsPerUpload;
    }

    @Override
    public List<ExportDataUploader> getExportDataUploaders() {
        return uploaderList;
    }

    @Override
    public Map<String, DataExportExecution> getDataExportExecutions() {
        return dataExportExecutionMap;
    }
}
