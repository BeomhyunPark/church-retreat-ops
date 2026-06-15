package com.gmc.retreat.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.gmc.retreat")
public class MyBatisConfig {
}
