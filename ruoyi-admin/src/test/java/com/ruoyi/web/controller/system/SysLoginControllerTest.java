package com.ruoyi.web.controller.system;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SysLoginControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RedisCache redisCache;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    //正常登录
    @Test
    void testLogin_01() throws Exception {
        //获取验证码图片，得uuid
        MvcResult captchaResult = mockMvc.perform(get("/captchaImage"))
                .andReturn();
        String resp = captchaResult.getResponse().getContentAsString();
        JSONObject json = JSON.parseObject(resp);
        String uuid = json.getString("uuid");
        String realCode = redisCache.getCacheObject("captcha_codes:" + uuid);

        LoginBody loginBody = new LoginBody();
        loginBody.setUsername("admin");
        loginBody.setPassword("admin123");
        loginBody.setCode(realCode);
        loginBody.setUuid(uuid);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    //验证码错误，参数校验
    @Test
    void testLogin_02() throws Exception {
        //redisCache.setCacheObject("sys_config:sys.account.captchaEnabled", "true");// 强制开启验证码
        MvcResult captchaResult = mockMvc.perform(get("/captchaImage"))
                .andReturn();
        String resp = captchaResult.getResponse().getContentAsString();
        JSONObject json = JSON.parseObject(resp);
        String uuid = json.getString("uuid");
        String wrongCode = "XXXX";
        LoginBody loginBody = new LoginBody();
        loginBody.setUsername("admin");
        loginBody.setPassword("admin123");
        loginBody.setCode(wrongCode); //错误验证码
        loginBody.setUuid(uuid);
        //登录失败（无token，返回错误信息）
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(containsString("验证码")));
    }
    //密码错误，参数校验
    @Test
    void testLogin_03() throws Exception {
        MvcResult captchaResult = mockMvc.perform(get("/captchaImage"))
                .andReturn();
        String resp = captchaResult.getResponse().getContentAsString();
        JSONObject json = JSON.parseObject(resp);
        String uuid = json.getString("uuid");
        String realCode = redisCache.getCacheObject("captcha_codes:" + uuid);
        LoginBody loginBody = new LoginBody();
        loginBody.setUsername("admin");
        loginBody.setPassword("wrong_password_123");
        loginBody.setCode(realCode);
        loginBody.setUuid(uuid);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(containsString("密码")));
    }

    //无登录访问
    @Test
    void testLogin_04() throws Exception {
        mockMvc.perform(get("/getInfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("获取用户信息异常"));
    }

}
