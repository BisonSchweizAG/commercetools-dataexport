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
package tech.bison.dataexport.core.api.upload;

import tech.bison.dataexport.core.api.configuration.GcpCloudStorageProperties;
import tech.bison.dataexport.core.internal.storage.gcp.GcpFileUploader;

import java.util.Objects;

public final class DataUploaders {

    /**
     * Prevents instantiation of this utility class.
     */
    private DataUploaders() {
    }

    /**
     * Creates an ExportDataUploader configured to upload files to Google Cloud Storage.
     *
     * @param gcpCloudStorageProperties configuration properties for GCP Cloud Storage
     * @return an ExportDataUploader that uploads files to GCP Cloud Storage
     * @throws NullPointerException if gcpCloudStorageProperties is null
     */
    public static ExportDataUploader gcpCloudStorage(GcpCloudStorageProperties gcpCloudStorageProperties) {
        Objects.requireNonNull(gcpCloudStorageProperties, "gcpCloudStorageProperties must not be null.");
        return new GcpFileUploader(gcpCloudStorageProperties);
    }
}
