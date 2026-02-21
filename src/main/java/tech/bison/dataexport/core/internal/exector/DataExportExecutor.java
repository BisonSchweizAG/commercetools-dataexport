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
package tech.bison.dataexport.core.internal.exector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.executor.*;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static tech.bison.dataexport.core.api.ResourceExportResult.FAILED;
import static tech.bison.dataexport.core.api.ResourceExportResult.SUCCESS;

public class DataExportExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(DataExportExecutor.class);
    private final CloudStorageUploader cloudStorageUploader;
    private final DataExporterProvider dataExporterProvider;
    private final DataWriterProvider dataWriterProvider;


    public DataExportExecutor(CloudStorageUploader cloudStorageUploader) {
        this(cloudStorageUploader, DataExporter::from, DataWriter::csv);
    }

    public DataExportExecutor(CloudStorageUploader cloudStorageUploader, DataExporterProvider dataExporterProvider,
                              DataWriterProvider dataWriterProvider) {
        this.cloudStorageUploader = cloudStorageUploader;
        this.dataExporterProvider = dataExporterProvider;
        this.dataWriterProvider = dataWriterProvider;
    }

    public DataExportResult execute(Context context) {
        DataExportResult dataExportResult = DataExportResult.empty();
        var resourceExportProperties = context.getResourceExportProperties();
        for (var entry : resourceExportProperties.entrySet()) {
            var resourceType = entry.getKey();
            LOG.info("Running data export for resource '{}'.", resourceType.getName());
            try {
                DataExporter dataExporter = dataExporterProvider.apply(entry.getKey());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataWriter dataWriter = dataWriterProvider.create(entry.getValue(), byteArrayOutputStream);
                dataExporter.export(context, dataWriter);
                dataWriter.flush();
                cloudStorageUploader.upload(getBlobName(resourceType, context.getClock()), byteArrayOutputStream.toByteArray());
                dataExportResult.addResult(resourceType, SUCCESS);
                LOG.info("Data export finished successfully for resource '{}'.", resourceType.getName());
            } catch (Exception ex) {
                dataExportResult.addResult(resourceType, FAILED);
                LOG.error("Error while executing data export for resource '{}'. Continue with next resource type.",
                        resourceType.getName(), ex);
            }
        }
        return dataExportResult;
    }

    private String getBlobName(ExportableResourceType resourceType, Clock clock) {
        return String.format("%ss/%ss_%s.csv", resourceType.getName(), resourceType.getName(),
                LocalDateTime.now(clock).format(
                        DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")));
    }
}
