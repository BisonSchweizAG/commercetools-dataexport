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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.executor.*;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static tech.bison.dataexport.core.api.ResourceExportResult.FAILED;
import static tech.bison.dataexport.core.api.ResourceExportResult.SUCCESS;

public class DataExportExecutor {

    private final static Logger LOG = LoggerFactory.getLogger(DataExportExecutor.class);
    private final CloudStorageUploader cloudStorageUploader;
    private final DataExporterProvider dataExporterProvider;
    private final DataWriterProvider dataWriterProvider;

    public DataExportExecutor(CloudStorageUploader cloudStorageUploader) {
        this(cloudStorageUploader, DataExporter::from, DataWriter::csv);
    }

    public DataExportExecutor(CloudStorageUploader cloudStorageUploader, DataExporterProvider dataExporterProvider, DataWriterProvider dataWriterProvider) {
        this.cloudStorageUploader = cloudStorageUploader;
        this.dataExporterProvider = dataExporterProvider;
        this.dataWriterProvider = dataWriterProvider;
    }

    public DataExportResult execute(Context context) {
        DataExportResult dataExportResult = DataExportResult.empty();
        var resourceExportProperties = context.getResourceExportProperties();
        for (var entry : resourceExportProperties.entrySet()) {
            var resourceType = entry.getKey();
            LOG.info("Running data export for resource '{}'.", entry.getKey().getName());
            try {
                DataExporter dataExporter = dataExporterProvider.apply(entry.getValue());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8), CSVFormat.DEFAULT.builder().setHeader("").get());
                DataWriter dataWriter = dataWriterProvider.apply(entry.getValue(), printer);
                dataExporter.export(context, dataWriter);
                cloudStorageUploader.upload(byteArrayOutputStream.toByteArray());
                dataExportResult.addResult(resourceType, SUCCESS);
            } catch (Exception ex) {
                dataExportResult.addResult(resourceType, FAILED);
                LOG.error("Error while executing data export for resource '{}'. Continue with next resource type.", resourceType.getName(), ex);
            }
        }
        return dataExportResult;
    }
}
