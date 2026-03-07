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
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.bison.dataexport.core.api.configuration.GcpCloudStorageProperties;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GcpFileUploaderTest {

    @Test
    void upload_usesInjectedStorage() {
        var storage = mock(Storage.class);
        var blob = mock(Blob.class);
        when(storage.create(any(), eq("data".getBytes()))).thenReturn(blob);

        var uploader = new GcpFileUploader("bucket-name", storage);

        uploader.upload("export.csv", "data".getBytes());

        verify(storage).create(any(), eq("data".getBytes()));
    }

    @Test
    void createStorage_loadsCredentialsFromPath(@TempDir Path tempDir)
            throws IOException, NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException {
        var credentialPath = writeServiceAccountCredentials(tempDir);
        var uploader = new GcpFileUploader(new GcpCloudStorageProperties("project-id", "bucket-name", credentialPath.toString()));

        var storage = extractStorage(uploader);

        assertThat(storage).isNotNull();
        assertThat(storage.getOptions().getProjectId()).isEqualTo("project-id");
        assertThat(storage.getOptions().getCredentials()).isInstanceOf(ServiceAccountCredentials.class);
    }

    private Path writeServiceAccountCredentials(Path tempDir) throws IOException, NoSuchAlgorithmException {
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var privateKey = keyPairGenerator.generateKeyPair().getPrivate();
        var privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        var credentialsJson = """
                {
                  "type": "service_account",
                  "project_id": "project-id",
                  "private_key_id": "private-key-id",
                  "private_key": "%s",
                  "client_email": "test-service-account@project-id.iam.gserviceaccount.com",
                  "client_id": "123456789012345678901",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(privateKeyPem.replace("\n", "\\n"));

        var credentialPath = tempDir.resolve("service-account.json");
        Files.writeString(credentialPath, credentialsJson);
        return credentialPath;
    }

    private Storage extractStorage(GcpFileUploader uploader)
            throws NoSuchFieldException, IllegalAccessException {
        Field storageField = GcpFileUploader.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        return (Storage) storageField.get(uploader);
    }
}
