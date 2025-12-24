package com.example.read.data.api.model;

import java.util.List;

/**
 * 通义千问API请求模型
 */
public class DashScopeRequest {
    
    private String model;
    private Input input;
    private Parameters parameters;
    
    public DashScopeRequest(String model, String systemPrompt, String userContent) {
        this.model = model;
        this.input = new Input(systemPrompt, userContent);
        this.parameters = new Parameters();
    }
    
    public static class Input {
        private List<Message> messages;
        
        public Input(String systemPrompt, String userContent) {
            this.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userContent)
            );
        }
        
        public List<Message> getMessages() {
            return messages;
        }
        
        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }
    }
    
    public static class Message {
        private String role;
        private String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
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
    
    public static class Parameters {
        private int max_tokens = 500;
        private float temperature = 0.7f;
        private String result_format = "message";
        
        public int getMax_tokens() {
            return max_tokens;
        }
        
        public void setMax_tokens(int max_tokens) {
            this.max_tokens = max_tokens;
        }
        
        public float getTemperature() {
            return temperature;
        }
        
        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }
        
        public String getResult_format() {
            return result_format;
        }
        
        public void setResult_format(String result_format) {
            this.result_format = result_format;
        }
    }
    
    // Getters and Setters
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Input getInput() {
        return input;
    }
    
    public void setInput(Input input) {
        this.input = input;
    }
    
    public Parameters getParameters() {
        return parameters;
    }
    
    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}
