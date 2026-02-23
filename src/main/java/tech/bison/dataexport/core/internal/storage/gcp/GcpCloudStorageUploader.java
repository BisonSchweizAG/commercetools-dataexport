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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.configuration.GcpCloudStorageProperties;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;

import java.io.FileInputStream;
import java.io.IOException;

public class GcpCloudStorageUploader implements CloudStorageUploader {

    private static final Logger LOG = LoggerFactory.getLogger(GcpCloudStorageUploader.class);
    private final GcpCloudStorageProperties gcpCloudStorageProperties;

    public GcpCloudStorageUploader(GcpCloudStorageProperties gcpCloudStorageProperties) {
        this.gcpCloudStorageProperties = gcpCloudStorageProperties;
    }

    @Override
    public void upload(String name, byte[] data) {
        try {
            var storage = getStorage();
            BlobId blobId = BlobId.of(gcpCloudStorageProperties.bucketName(), name);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/csv").build();
            Blob blob = storage.create(blobInfo, data);
            LOG.info("Created blob '{}' in bucket '{}'", name, gcpCloudStorageProperties.bucketName());
            LOG.debug("The hash of the created blob is {}", blob.getMd5ToHexString());
        } catch (IOException e) {
            throw new DataExportException(
                    String.format("Error while uploading blob data with name '%s' to google cloud storage.", name), e);
        }
    }

    private Storage getStorage() throws IOException {
        var storageBuilder = StorageOptions.newBuilder().setProjectId(gcpCloudStorageProperties.projectId());
        if (gcpCloudStorageProperties.credentialPath() != null && !gcpCloudStorageProperties.credentialPath().isEmpty()) {
            var credentials = GoogleCredentials.fromStream(new FileInputStream(gcpCloudStorageProperties.credentialPath()))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            credentials.refreshIfExpired();
            storageBuilder.setCredentials(credentials);
        }
        return storageBuilder.build().getService();
    }
}
