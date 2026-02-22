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
package tech.bison.dataexport.core.internal.exporter.common;

import com.commercetools.api.models.common.CentPrecisionMoney;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class CsvWriterSupport {

    private CsvWriterSupport() {
    }

    public static String extractValue(JsonNode node, String field) {
        String pointer = "/" + field.replace(".", "/");
        JsonNode value = node.at(pointer);
        return extractNodeValue(value);
    }

    public static String extractNodeValue(JsonNode value) {
        if (value.get("type") != null && CentPrecisionMoney.CENT_PRECISION.equals(value.get("type").asText())) {
            double amount = value.get("centAmount").asInt() / 100d;
            return String.valueOf(amount);
        } else {
            return value.asText("");
        }
    }

    public static List<String> topLevelFields(List<String> fields, String childItemPrefix) {
        return fields.stream()
                .filter(field -> !field.startsWith(childItemPrefix))
                .toList();
    }

    public static List<String> childItemFields(List<String> fields, String childItemPrefix) {
        return fields.stream()
                .filter(field -> field.startsWith(childItemPrefix))
                .toList();
    }

    public static List<String> createParentRecord(JsonNode parentNode, List<String> parentFields, int childFieldCount) {
        return Stream.concat(
                parentFields.stream().map(f -> extractValue(parentNode, f)),
                Collections.nCopies(childFieldCount, "").stream()
        ).toList();
    }

    public static List<String> createChildRecord(int parentFieldCount, List<String> childFields, Function<String, String> childValueExtractor) {
        return Stream.concat(
                Collections.nCopies(parentFieldCount, "").stream(),
                childFields.stream().map(childValueExtractor)
        ).toList();
    }
}
