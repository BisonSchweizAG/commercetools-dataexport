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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;

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
}
