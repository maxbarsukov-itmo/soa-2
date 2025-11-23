package ru.ifmo.soa.peopleservice.util.datetime;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  @Override
  public OffsetDateTime unmarshal(String v) {
    return v == null ? null : OffsetDateTime.parse(v, FORMATTER);
  }

  @Override
  public String marshal(OffsetDateTime v) {
    return v == null ? null : FORMATTER.format(v);
  }
}
