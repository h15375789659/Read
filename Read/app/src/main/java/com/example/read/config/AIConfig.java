package com.example.read.config;

/**
 * AI服务配置类
 * 存储通义千问API的配置信息
 */
public class AIConfig {
    
    // 通义千问API配置
    public static final String DASHSCOPE_API_KEY = "sk-6bb2f7e1a8ad4b4384fa2bc19941a889";
    public static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    
    // 使用的模型 - qwen-turbo性价比最高
    public static final String MODEL_NAME = "qwen-turbo";
    
    // 超时设置（毫秒）
    public static final int API_TIMEOUT_MS = 30000; // 30秒
    
    // 摘要生成的最大token数
    public static final int MAX_TOKENS = 500;
    
    // 温度参数（控制输出的随机性，0-1之间，越低越确定）
    public static final float TEMPERATURE = 0.7f;
    
    private AIConfig() {
        // 私有构造函数，防止实例化
    }
}
