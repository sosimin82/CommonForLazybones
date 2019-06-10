package com.cfl.config;

import com.cfl.interceptor.NetworkAclInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    @Autowired
    private NetworkAclInterceptor networkAclInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(networkAclInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error");
    }
}
