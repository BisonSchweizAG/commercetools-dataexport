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
package tech.bison.dataexport.core.internal.storage.gcp;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.configuration.GcpCloudStorageProperties;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.upload.ExportDataUploader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;


public class GcpFileUploader implements ExportDataUploader {

    private static final Logger LOG = LoggerFactory.getLogger(GcpFileUploader.class);
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private final String bucketName;
    private final Storage storage;

    public GcpFileUploader(GcpCloudStorageProperties gcpCloudStorageProperties) {
        this(gcpCloudStorageProperties.bucketName(), createStorage(gcpCloudStorageProperties));
    }

    public GcpFileUploader(String bucketName, Storage storage) {
        this.bucketName = Objects.requireNonNull(bucketName, "bucketName must not be null.");
        this.storage = Objects.requireNonNull(storage, "storage must not be null.");
    }

    @Override
    public void upload(String name, byte[] data) {
        BlobId blobId = BlobId.of(bucketName, name);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/csv").build();
        Blob blob = storage.create(blobInfo, data);
        LOG.info("Created blob '{}' in bucket '{}'", name, bucketName);
        LOG.debug("The hash of the created blob is {}", blob.getMd5ToHexString());
    }

    private static Storage createStorage(GcpCloudStorageProperties gcpCloudStorageProperties) {
        var storageBuilder = StorageOptions.newBuilder().setProjectId(gcpCloudStorageProperties.projectId());
        try {
            if (gcpCloudStorageProperties.credentialPath() != null && !gcpCloudStorageProperties.credentialPath().isEmpty()) {
                storageBuilder.setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(gcpCloudStorageProperties.credentialPath()))
                        .createScoped(CLOUD_PLATFORM_SCOPE));
            }
        } catch (IOException e) {
            throw new DataExportException("Error while creating google cloud storage client.", e);
        }
        return storageBuilder.build().getService();
    }
}
