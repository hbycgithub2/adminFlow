package com.hmall.config;

import cn.hutool.core.collection.CollUtil;
import com.hmall.interceptor.LoginInterceptor;
import com.hmall.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class MvcConfig implements WebMvcConfigurer {

   private final JwtTool jwtTool;
   private final AuthProperties authProperties;

/*    @Bean
    public CommonExceptionAdvice commonExceptionAdvice(){
        return new CommonExceptionAdvice();
    }*/

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置TTS静态资源访问（包括音频和视频文件）
        registry.addResourceHandler("/tts/**")
                .addResourceLocations("file:./tts/");
        
        // 配置静态HTML页面访问（包括字幕编辑器）
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ==================== Token权限验证已禁用 ====================
        // 如需启用，取消下面代码的注释
        
        /*
        // 1.添加拦截器
        LoginInterceptor loginInterceptor = new LoginInterceptor(jwtTool);
        InterceptorRegistration registration = registry.addInterceptor(loginInterceptor);
        // 2.配置拦截路径
        List<String> includePaths = authProperties.getIncludePaths();
        if (CollUtil.isNotEmpty(includePaths)) {
            registration.addPathPatterns(includePaths);
        }
        // 3.配置放行路径
        List<String> excludePaths = authProperties.getExcludePaths();
        if (CollUtil.isNotEmpty(excludePaths)) {
            registration.excludePathPatterns(excludePaths);
        }
        registration.excludePathPatterns(
                "/error",
                "/favicon.ico",
                "/v2/**",
                "/v3/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/doc.html",
                "/tts/**",  // TTS音频文件访问
                "/api/document-tts/**",  // TTS API接口
                "/api/video-generator/**",  // 视频生成API接口（无需认证）
                "/api/audio/**",  // 音频生成API接口（无需认证）⭐ 新增
                "/api/subtitle-editor/**",  // 字幕编辑API接口（无需认证）
                "/subtitle-editor.html",  // 字幕编辑器页面
                "/video-generator-test.html",  // 视频生成测试页面
                "/document-tts-test.html"  // 文档TTS测试页面
                );
        */

    }
}
