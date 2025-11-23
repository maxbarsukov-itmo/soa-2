package ru.ifmo.soa.peopleservice.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serial;
import java.io.Serializable;

@XmlRootElement(name = "AsyncSearchResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class AsyncSearchResponseDto implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private String taskId;
  private String message;
  private String estimatedCompletion;

  public AsyncSearchResponseDto(String taskId, String message, String estimatedCompletion) {
    this.taskId = taskId;
    this.message = message;
    this.estimatedCompletion = estimatedCompletion;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getEstimatedCompletion() {
    return estimatedCompletion;
  }

  public void setEstimatedCompletion(String estimatedCompletion) {
    this.estimatedCompletion = estimatedCompletion;
  }
}
