package ru.chousik.erpnext.ra;

import jakarta.resource.cci.IndexedRecord;
import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SimpleIndexedRecord extends ArrayList implements IndexedRecord, Serializable {
    private String recordName;
    private String shortDescription;

    public SimpleIndexedRecord(String recordName) {
        this.recordName = recordName;
    }

    @Override
    public String getRecordName() {
        return recordName;
    }

    @Override
    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    @Override
    public String getRecordShortDescription() {
        return shortDescription;
    }

    @Override
    public void setRecordShortDescription(String description) {
        this.shortDescription = description;
    }

    @Override
    public Object clone() {
        SimpleIndexedRecord clone = new SimpleIndexedRecord(recordName);
        clone.shortDescription = shortDescription;
        clone.addAll(this);
        return clone;
    }
}
