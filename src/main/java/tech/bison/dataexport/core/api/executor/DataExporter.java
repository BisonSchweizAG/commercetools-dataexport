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
package tech.bison.dataexport.core.api.executor;

/**
 * Interface for exporting data.
 */
public interface DataExporter {
    /**
     * Performs the data export.
     *
     * @param context         the context of the export.
     * @param deltaLoadFilter if ExportMode.DELTA the deltaLoadFilter contains a where clause to apply when loading data otherwise null.
     * @param dataWriter      the writer to write the exported data to.
     */
    void export(Context context, String deltaLoadFilter, DataWriter dataWriter);
}
