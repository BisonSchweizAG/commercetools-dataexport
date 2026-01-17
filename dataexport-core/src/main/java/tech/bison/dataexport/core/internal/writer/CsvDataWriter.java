package tech.bison.dataexport.core.internal.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.executor.DataWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvDataWriter implements DataWriter {

    private final CSVPrinter csvPrinter;
    private final DataExportProperties dataExportProperties;
    private final ObjectMapper mapper = new ObjectMapper();

    public CsvDataWriter(CSVPrinter csvPrinter, DataExportProperties dataExportProperties) {
        this.csvPrinter = csvPrinter;
        this.dataExportProperties = dataExportProperties;
    }

    @Override
    public void writeRow(Object source) throws IOException {
        JsonNode node = mapper.valueToTree(source);
        List<String> values = new ArrayList<>();
        for (String field : dataExportProperties.fields()) {
            String pointer = "/" + field.replace(".", "/");
            JsonNode value = node.at(pointer);
            values.add(value.isMissingNode() ? "" : value.asText());
        }
        csvPrinter.printRecord(values);
    }
}
