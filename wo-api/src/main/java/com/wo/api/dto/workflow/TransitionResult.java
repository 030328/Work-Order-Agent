package com.wo.api.dto.workflow;

import lombok.Data;

import java.io.Serializable;

@Data
public class TransitionResult implements Serializable {

    private Boolean success;

    private String fromStatus;

    private String toStatus;

    private String message;

    public static TransitionResult success(String fromStatus, String toStatus) {
        TransitionResult result = new TransitionResult();
        result.setSuccess(true);
        result.setFromStatus(fromStatus);
        result.setToStatus(toStatus);
        result.setMessage("Transition executed successfully");
        return result;
    }

    public static TransitionResult failure(String fromStatus, String toStatus, String message) {
        TransitionResult result = new TransitionResult();
        result.setSuccess(false);
        result.setFromStatus(fromStatus);
        result.setToStatus(toStatus);
        result.setMessage(message);
        return result;
    }
}
