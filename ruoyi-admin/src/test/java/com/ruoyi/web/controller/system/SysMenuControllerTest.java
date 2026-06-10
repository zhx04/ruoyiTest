package com.ruoyi.web.controller.system;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.security.filter.JwtAuthenticationTokenFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SysMenuControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    private MockMvc mockMvc;
    private String adminToken;
    private String testToken;
    private String commonToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(jwtAuthenticationTokenFilter)
                .build();
        adminToken = login("admin", "admin123");
        testToken = login("aaa", "123456");
        commonToken = login("comtest", "123456");
    }

    private String login(String username, String password) throws Exception {
        MvcResult captcha = mockMvc.perform(get("/captchaImage")).andReturn();
        JSONObject capJson = JSON.parseObject(captcha.getResponse().getContentAsString());
        String uuid = capJson.getString("uuid");
        String realCode = redisCache.getCacheObject("captcha_codes:" + uuid);

        LoginBody loginBody = new LoginBody();
        loginBody.setUsername(username);
        loginBody.setPassword(password);
        loginBody.setCode(realCode);
        loginBody.setUuid(uuid);

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(loginBody)))
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        return json.getString("token");
    }

    // 访问菜单列表
    @Test
    void testMenu_01() throws Exception {
        MvcResult result = mockMvc.perform(get("/system/menu/list")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("=====访问菜单列表=====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
        JSONArray menuList = resp.getJSONArray("data");
        for (int i = 0; i < menuList.size(); i++) {
            JSONObject menu = menuList.getJSONObject(i);
            System.out.printf("菜单ID:%s,菜单名称:%s,路由地址:%s%n",
                    menu.getLong("menuId"), menu.getString("menuName"), menu.getString("path"));
        }
    }

    // 无权限用户无法访问菜单列表
    @Test
    void testMenu_02() throws Exception {
        MvcResult result = mockMvc.perform(get("/system/menu/list")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("=====无权限用户访问菜单=====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 未登录访问菜单列表
    @Test
    void testMenu_03() throws Exception {
        MvcResult result = mockMvc.perform(get("/system/menu/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("=====未登录访问菜单=====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 管理员新增菜单
    @Test
    void testMenu_04() throws Exception {
        String reqJson = """
        {
            "menuName": "测试菜单O0",
            "parentId": 0,
            "orderNum": 999,
            "path": "test",
            "menuType": "M",
            "isFrame": "1"
        }
        """;

        MvcResult result = mockMvc.perform(post("/system/menu")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("\n===== 新增菜单 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 新增菜单失败:菜单名称重复
    @Test
    void testMenu_05() throws Exception {
        String reqJson = """
        {
            "menuName": "系统管理",
            "parentId": 0,
            "orderNum": 1,
            "path": "system",
            "menuType": "M",
            "isFrame": "1"
        }
        """;

        MvcResult result = mockMvc.perform(post("/system/menu")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("\n===== 新增菜单 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 新增菜单：外链地址必须http开头
    @Test
    void testMenu_06() throws Exception {
        String reqJson = """
        {
            "menuName": "外链测试菜单03",
            "parentId": 0,
            "orderNum": 2,
            "path": "www.bbb.com",
            "menuType": "M",
            "isFrame": "0"
        }
        """;

        MvcResult result = mockMvc.perform(post("/system/menu")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("\n===== 新增菜单 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 新增菜单：路由名称/地址重复
    @Test
    void testMenu_07() throws Exception {
        String reqJson = """
        {
            "menuName": "路由重复测试00",
            "parentId": 0,
            "orderNum": 3,
            "path": "system",
            "menuType": "M",
            "isFrame": "1"
        }
        """;

        MvcResult result = mockMvc.perform(post("/system/menu")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("\n===== 新增菜单 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }
}
