package tech.bison.dataexport.core.api.executor;

import java.io.IOException;

public interface DataWriter {
    void writeRow(Object object) throws IOException;
}
