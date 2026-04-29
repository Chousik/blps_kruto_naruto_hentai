package ru.chousik.erpnext.ra;

import jakarta.resource.cci.MappedRecord;
import java.io.Serializable;
import java.util.LinkedHashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SimpleMappedRecord extends LinkedHashMap implements MappedRecord, Serializable {
    private String recordName;
    private String shortDescription;

    public SimpleMappedRecord(String recordName) {
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
        SimpleMappedRecord clone = new SimpleMappedRecord(recordName);
        clone.shortDescription = shortDescription;
        clone.putAll(this);
        return clone;
    }
}
