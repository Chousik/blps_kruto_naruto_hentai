package ru.chousik.erpnext.ra;

import jakarta.resource.cci.IndexedRecord;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.RecordFactory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SimpleRecordFactory implements RecordFactory {
    @Override
    public MappedRecord createMappedRecord(String recordName) {
        return new SimpleMappedRecord(recordName);
    }

    @Override
    public IndexedRecord createIndexedRecord(String recordName) {
        return new SimpleIndexedRecord(recordName);
    }
}
