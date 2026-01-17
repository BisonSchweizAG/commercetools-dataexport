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
package tech.bison.dataexport.core.api.configuration;

import com.commercetools.api.client.ProjectApiRoot;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import tech.bison.dataexport.core.api.command.ExportableResourceType;

public interface Configuration {

  ProjectApiRoot getApiRoot();

  CommercetoolsProperties getApiProperties();

  Clock getClock();
}
