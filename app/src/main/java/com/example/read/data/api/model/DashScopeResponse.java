package com.example.read.data.api.model;

import java.util.List;

/**
 * 通义千问API响应模型
 */
public class DashScopeResponse {
    
    private String request_id;
    private Output output;
    private Usage usage;
    private String code;
    private String message;
    
    /**
     * 检查响应是否成功
     */
    public boolean isSuccess() {
        return code == null && output != null;
    }
    
    /**
     * 获取生成的文本内容
     */
    public String getGeneratedText() {
        if (output != null && output.getChoices() != null && !output.getChoices().isEmpty()) {
            Choice choice = output.getChoices().get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }
    
    /**
     * 获取错误信息
     */
    public String getErrorMessage() {
        if (code != null) {
            return code + ": " + message;
        }
        return null;
    }
    
    public static class Output {
        private List<Choice> choices;
        private String finish_reason;
        
        public List<Choice> getChoices() {
            return choices;
        }
        
        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }
        
        public String getFinish_reason() {
            return finish_reason;
        }
        
        public void setFinish_reason(String finish_reason) {
            this.finish_reason = finish_reason;
        }
    }
    
    public static class Choice {
        private String finish_reason;
        private Message message;
        
        public String getFinish_reason() {
            return finish_reason;
        }
        
        public void setFinish_reason(String finish_reason) {
            this.finish_reason = finish_reason;
        }
        
        public Message getMessage() {
            return message;
        }
        
        public void setMessage(Message message) {
            this.message = message;
        }
    }
    
    public static class Message {
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
    
    public static class Usage {
        private int input_tokens;
        private int output_tokens;
        private int total_tokens;
        
        public int getInput_tokens() {
            return input_tokens;
        }
        
        public void setInput_tokens(int input_tokens) {
            this.input_tokens = input_tokens;
        }
        
        public int getOutput_tokens() {
            return output_tokens;
        }
        
        public void setOutput_tokens(int output_tokens) {
            this.output_tokens = output_tokens;
        }
        
        public int getTotal_tokens() {
            return total_tokens;
        }
        
        public void setTotal_tokens(int total_tokens) {
            this.total_tokens = total_tokens;
        }
    }
    
    // Getters and Setters
    public String getRequest_id() {
        return request_id;
    }
    
    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }
    
    public Output getOutput() {
        return output;
    }
    
    public void setOutput(Output output) {
        this.output = output;
    }
    
    public Usage getUsage() {
        return usage;
    }
    
    public void setUsage(Usage usage) {
        this.usage = usage;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
