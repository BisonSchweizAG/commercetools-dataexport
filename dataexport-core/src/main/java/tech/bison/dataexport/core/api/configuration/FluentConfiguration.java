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
package tech.bison.dataexport.core.api.configuration;

import com.commercetools.api.client.ProjectApiRoot;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import tech.bison.dataexport.core.api.DataExport;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.ExportableResourceType;


public class FluentConfiguration implements Configuration {

  private CommercetoolsProperties apiProperties;
  private ProjectApiRoot projectApiRoot;
  private Clock clock;
  private GcpCloudStorageProperties gcpCloudStorageProperties;
  private final Map<ExportableResourceType, DataExportProperties> exportFieldsMap = new EnumMap<>(
      ExportableResourceType.class);

  /**
   * @return The new fully-configured DataExport instance.
   */
  public DataExport load() {
    validateConfiguration();
    return new DataExport(this);
  }

  private void validateConfiguration() {
    if (projectApiRoot == null && apiProperties == null) {
      throw new DataExportException(
          "Missing commercetools api configuration. Either use withApiProperties() or withApiRoot().");
    }
    if (exportFieldsMap.isEmpty()) {
      throw new DataExportException("At least one export type must be configured.");
    }
    if (!exportFieldsMap.values().stream().allMatch(fields -> !fields.fields().isEmpty())) {
      throw new DataExportException("At least one export type has no fields configured.");
    }
    if (gcpCloudStorageProperties == null) {
      throw new DataExportException("GCP cloud storage configuration is missing.");
    }
  }

  /**
   * Configure the commercetools api with properties.
   */
  public FluentConfiguration withApiProperties(CommercetoolsProperties apiProperties) {
    this.apiProperties = apiProperties;
    return this;
  }

  /**
   * Configure the commercetools api with the given api root.
   */
  public FluentConfiguration withApiRoot(ProjectApiRoot projectApiRoot) {
    this.projectApiRoot = projectApiRoot;
    return this;
  }

  /**
   * Configure the GCP cloud storage.
   */
  public FluentConfiguration withGcpCloudStorageProperties(GcpCloudStorageProperties gcpCloudStorageProperties) {
    this.gcpCloudStorageProperties = gcpCloudStorageProperties;
    return this;
  }

  /**
   * Configures the fields to be exported for the given resource types.
   */
  public FluentConfiguration withExportFields(Map<ExportableResourceType, DataExportProperties> exportFieldsMap) {
    this.exportFieldsMap.putAll(exportFieldsMap);
    return this;
  }


  public FluentConfiguration withClock(Clock clock) {
    this.clock = clock;
    return this;
  }


  @Override
  public CommercetoolsProperties getApiProperties() {
    return apiProperties;
  }

  @Override
  public ProjectApiRoot getApiRoot() {
    return projectApiRoot;
  }


  @Override
  public Clock getClock() {
    return clock;
  }

  public GcpCloudStorageProperties getGcpCloudStorageProperties() {
    return gcpCloudStorageProperties;
  }

  @Override
  public Map<ExportableResourceType, DataExportProperties> getResourceExportProperties() {
    return exportFieldsMap;
  }
}
