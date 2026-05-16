package com.pvp.backend.dto;

import com.pvp.backend.model.Stowage;

import java.util.ArrayList;
import java.util.List;

public class StowagePlanResponse {
    private boolean success;
    private String message;
    private List<String> messages = new ArrayList<>();
    private Stowage stowagePlan;

    public StowagePlanResponse() {
    }

    public StowagePlanResponse(boolean success, String message, List<String> messages, Stowage stowagePlan) {
        this.success = success;
        this.message = message;
        this.messages = messages;
        this.stowagePlan = stowagePlan;
    }

    public boolean isSuccess() { return success; }

    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public List<String> getMessages() { return messages; }

    public void setMessages(List<String> messages) { this.messages = messages; }

    public Stowage getStowagePlan() { return stowagePlan; }

    public void setStowagePlan(Stowage stowagePlan) { this.stowagePlan = stowagePlan; }
}
