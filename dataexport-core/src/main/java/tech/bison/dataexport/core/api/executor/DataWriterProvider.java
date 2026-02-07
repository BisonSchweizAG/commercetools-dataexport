package tech.bison.dataexport.core.api.executor;

import org.apache.commons.csv.CSVPrinter;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;

import java.util.function.BiFunction;

public interface DataWriterProvider extends BiFunction<DataExportProperties, CSVPrinter, DataWriter> {
}
