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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.command.ExportCommand;
import tech.bison.dataexport.core.api.command.ExportResult;
import tech.bison.dataexport.core.api.command.ResourceExportSummary;

public class DataExportExecutor {

  private final static Logger LOG = LoggerFactory.getLogger(DataExportExecutor.class);

  public ExportResult execute(Context context, List<ExportCommand> exportCommands) {
    ExportResult cleanupResult = ExportResult.empty();
    for (var cleanupCommand : exportCommands) {
      LOG.info("Running data export for resource '{}'.", cleanupCommand.getResourceType().getName());
      ResourceExportSummary resourceExportSummary = cleanupCommand.execute(context);
      cleanupResult.addResult(cleanupCommand.getResourceType(), resourceExportSummary);
    }
    return cleanupResult;
  }
}
