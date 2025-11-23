package ru.ifmo.soa.peopleservice.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serial;
import java.io.Serializable;

@XmlRootElement(name = "CallbackError")
@XmlAccessorType(XmlAccessType.FIELD)
public class CallbackError implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  public int code;
  public String message;

  public CallbackError(int code, String message) {
    this.code = code;
    this.message = message;
  }
}
