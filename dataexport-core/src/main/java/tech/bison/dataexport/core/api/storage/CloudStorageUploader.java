package tech.bison.dataexport.core.api.storage;

public interface CloudStorageUploader {
    void upload(byte[] data);
}
