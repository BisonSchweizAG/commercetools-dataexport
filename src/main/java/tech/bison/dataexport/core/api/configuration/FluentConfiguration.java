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
import tech.bison.dataexport.core.api.DataExport;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.DataWriter;
import tech.bison.dataexport.core.api.executor.DataWriterProvider;
import tech.bison.dataexport.core.api.executor.ExportableResourceType;
import tech.bison.dataexport.core.api.upload.ExportDataUploader;
import tech.bison.dataexport.core.internal.storage.gcp.GcpFileUploader;

import java.time.Clock;
import java.util.*;


public class FluentConfiguration implements Configuration {

    private CommercetoolsProperties apiProperties;
    private ProjectApiRoot projectApiRoot;
    private Clock clock;
    private final DataWriterProvider dataWriterProvider = DataWriter::csv;
    private String outputFileExtension = "csv";
    private Integer maxRecordsPerUpload;
    private final List<ExportDataUploader> uploaderList = new ArrayList<>();
    private final Map<ExportableResourceType, DataExportProperties> exportFieldsMap = new EnumMap<>(
            ExportableResourceType.class);

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
        if (exportFieldsMap.isEmpty()) {
            throw new DataExportException("At least one export type must be configured.");
        }
        if (exportFieldsMap.values().stream().anyMatch(fields -> fields.fields().isEmpty())) {
            throw new DataExportException("At least one export type has no fields configured.");
        }
        if (uploaderList.isEmpty()) {
            throw new DataExportException("No data export uploader configured.");
        }
        if (maxRecordsPerUpload != null && maxRecordsPerUpload < 1) {
            throw new DataExportException("maxRecordsPerUpload must be greater than 0.");
        }
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
        this.exportFieldsMap.put(resourceType, new DataExportProperties(resourceType, exportFields));
        return this;
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
    public DataWriterProvider getDataWriterProvider() {
        return dataWriterProvider;
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
    public Map<ExportableResourceType, DataExportProperties> getResourceExportProperties() {
        return exportFieldsMap;
    }
}
