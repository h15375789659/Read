package com.example.read.data.api;

import com.example.read.data.api.model.DashScopeRequest;
import com.example.read.data.api.model.DashScopeResponse;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * 通义千问API接口
 */
public interface DashScopeApi {
    
    /**
     * 调用通义千问文本生成API
     * 
     * @param authorization 授权头，格式为 "Bearer {api_key}"
     * @param request 请求体
     * @return API响应
     */
    @POST("services/aigc/text-generation/generation")
    Single<DashScopeResponse> generateText(
            @Header("Authorization") String authorization,
            @Header("Content-Type") String contentType,
            @Body DashScopeRequest request
    );
}
