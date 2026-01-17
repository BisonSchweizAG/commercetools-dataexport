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
package tech.bison.dataexport.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.command.DataExportResult;
import tech.bison.dataexport.core.api.configuration.Configuration;
import tech.bison.dataexport.core.api.configuration.FluentConfiguration;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.Context;
import tech.bison.dataexport.core.api.executor.DataExportExecutor;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;
import tech.bison.dataexport.core.internal.storage.gcp.GcpCloudStorageUploader;

import java.util.List;

/**
 * Entry point for a data cleanup run.
 */
public class DataExport {

    private static final Logger LOG = LoggerFactory.getLogger(DataExport.class);

    private final Configuration configuration;
    private final DataExportExecutor dataExportExecutor;

    public DataExport(Configuration configuration) {
        this.configuration = configuration;
        dataExportExecutor = new DataExportExecutor(createCloudStorageUploader(configuration));
    }

    private CloudStorageUploader createCloudStorageUploader(Configuration configuration) {
        if (configuration.getGcpCloudStorageProperties() != null) {
            return new GcpCloudStorageUploader(configuration.getGcpCloudStorageProperties());
        }
        throw new DataExportException("No cloud storage configuration found.");
    }

    /**
     * This is your starting point. This creates a configuration which can be customized to your needs before being loaded into a new DataCleanup instance using the load() method.
     * <p>
     * In its simplest form, this is how you configure DataCleanup with all defaults to get started:
     * <pre>DataCleanup dataCleanup = DataCleanup.configure().withApiUrl(..).load();</pre>
     * <p>
     * After that you have a fully-configured DataCleanup instance and you can call migrate()
     *
     * @return A new configuration from which DataCleanup can be loaded.
     */
    public static FluentConfiguration configure() {
        return new FluentConfiguration();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Executes the configured data exports.
     *
     * @return the export result
     * @throws DataExportException in case of any exception thrown during execution.
     */
    public DataExportResult execute() {
        try {
            var context = new Context(configuration);
            return dataExportExecutor.execute(context, List.of());
        } catch (Exception ex) {
            throw new DataExportException("Error while executing data export.", ex);
        }
    }
}
