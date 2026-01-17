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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.command.DataExportResult;
import tech.bison.dataexport.core.api.command.DataLoader;
import tech.bison.dataexport.core.api.command.ResourceExportData;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;

import java.util.List;

import static tech.bison.dataexport.core.api.ResourceExportResult.FAILED;
import static tech.bison.dataexport.core.api.ResourceExportResult.SUCCESS;

public class DataExportExecutor {

    private final static Logger LOG = LoggerFactory.getLogger(DataExportExecutor.class);
    private final CloudStorageUploader cloudStorageUploader;

    public DataExportExecutor(CloudStorageUploader cloudStorageUploader) {
        this.cloudStorageUploader = cloudStorageUploader;
    }

    public DataExportResult execute(Context context, List<DataLoader> dataLoaders) {
        DataExportResult dataExportResult = DataExportResult.empty();
        for (var dataLoader : dataLoaders) {
            LOG.info("Running data export for resource '{}'.", dataLoader.getResourceType().getName());
            try {
                ResourceExportData exportData = dataLoader.load(context);
                cloudStorageUploader.upload(exportData);
                dataExportResult.addResult(dataLoader.getResourceType(), SUCCESS);
            } catch (Exception ex) {
                dataExportResult.addResult(dataLoader.getResourceType(), FAILED);
                LOG.error("Error while executing data export for resource '{}'. Continue with next resource type.", dataLoader.getResourceType().getName(), ex);
            }
        }
        return dataExportResult;
    }
}
