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
package tech.bison.dataexport.core.api;

import tech.bison.dataexport.core.api.configuration.Configuration;
import tech.bison.dataexport.core.api.configuration.FluentConfiguration;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.Context;
import tech.bison.dataexport.core.api.executor.DataExportResult;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;
import tech.bison.dataexport.core.internal.exector.DataExportExecutor;
import tech.bison.dataexport.core.internal.storage.gcp.GcpCloudStorageUploader;

/**
 * Entry point for a data cleanup run.
 */
public class DataExport {
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
     * This is your starting point. This creates a configuration which can be customized to your needs before being loaded into a new DataExport instance using the load() method.
     * <p>
     * In its simplest form, this is how you configure DataExport with all defaults to get started:
     * <pre>DataExport dataCleanup = DataExport.configure().withApiUrl(..).load();</pre>
     * <p>
     * After that you have a fully-configured DataExport instance and you can call execute()
     *
     * @return A new configuration from which DataExport can be loaded.
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
            return dataExportExecutor.execute(context);
        } catch (Exception ex) {
            throw new DataExportException("Error while executing data export.", ex);
        }
    }
}
