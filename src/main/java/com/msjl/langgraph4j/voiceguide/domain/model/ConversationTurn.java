package com.msjl.langgraph4j.voiceguide.domain.model;

import java.io.Serializable;

public class ConversationTurn implements Serializable {

    private String role;
    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
