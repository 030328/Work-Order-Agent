package com.wo.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;

@Data
public class TransitionResult implements Serializable {

    private Boolean success;

    private String fromStatus;

    private String toStatus;

    private String message;
}
