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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SysDeptControllerTest {

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
        adminToken = login("admin", "admin123");//admin
        testToken = login("aaa", "123456");//测试，无权限
        commonToken = login("comtest", "123456");//common
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

    // 查询部门列表
    @Test
    void testDept_01() throws Exception {
        MvcResult result = mockMvc.perform(get("/system/dept/list")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JSONObject resp = JSONObject.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 查询部门 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));

        JSONArray deptList = resp.getJSONArray("data");
        System.out.println("=== 部门列表 ===");
        for (int i = 0; i < deptList.size(); i++) {
            JSONObject dept = deptList.getJSONObject(i);
            System.out.println("部门ID：" + dept.getLong("deptId") + "，名称：" + dept.getString("deptName"));
        }
    }

    // 未登录访问部门
    @Test
    void testDept_02() throws Exception {
        MvcResult result = mockMvc.perform(get("/system/dept/list"))
                .andExpect(jsonPath("$.code").value(401))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 未登录访问部门列表 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 无权限访问部门列表
    @Test
    void testDept_03() throws Exception {
        MvcResult result = mockMvc.perform(get("/system/dept/list")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(jsonPath("$.code").value(403))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 无权限访问部门列表 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 修改部门
    @Test
    void testDept_04() throws Exception {
        String reqJson = """
        {
            "deptId": 109,
            "deptName": "财务部门（测试版）",
            "parentId": 102,
            "orderNum": 2,
            "status": "0"
        }""";

        MvcResult result = mockMvc.perform(put("/system/dept")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 修改部门 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    //修改部门：重复名称
    @Test
    void testDept_05() throws Exception {
        String reqJson = """
        {
            "deptId": 108,
            "deptName": "财务部门（测试版）",
            "parentId": 102,
            "orderNum": 2
        }""";

        MvcResult result = mockMvc.perform(put("/system/dept")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 修改部门 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    //修改部门：上级是自己
    @Test
    void testDept_06() throws Exception {
        String reqJson = """
        {
            "deptId": 102,
            "deptName": "长沙分公司（测试）",
            "parentId": 102,
            "orderNum": 2
        }""";

        MvcResult result = mockMvc.perform(put("/system/dept")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 修改部门 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 删除部门
    @Test
    void testDept_07() throws Exception {
        MvcResult result = mockMvc.perform(delete("/system/dept/107")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 删除部门 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 删除部门：存在子部门
    @Test
    void testDept_08() throws Exception {
        MvcResult result = mockMvc.perform(delete("/system/dept/102")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(601))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 删除部门 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    // 删除部门：存在员工
    @Test
    void testDept_09() throws Exception {
        MvcResult result = mockMvc.perform(delete("/system/dept/103")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(601))
                .andReturn();
        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        System.out.println("===== 删除部门 =====");
        System.out.println("响应code：" + resp.getInteger("code"));
        System.out.println("响应msg：" + resp.getString("msg"));
    }

    @Test
    void testDept_10() throws Exception {
        MvcResult result = mockMvc.perform(get("/system/dept/list")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JSONObject resp = JSON.parseObject(result.getResponse().getContentAsString());
        JSONArray deptList = resp.getJSONArray("data");
        // 按parentId分组
        Map<Long, List<JSONObject>> groupByParent = new HashMap<>();
        for (int i = 0; i < deptList.size(); i++) {
            JSONObject dept = deptList.getJSONObject(i);
            Long parentId = dept.getLong("parentId");
            groupByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(dept);
        }

        System.out.println("===== 部门层级关系 =====");
        printTree(groupByParent, 0L, "");
    }
    private void printTree(Map<Long, List<JSONObject>> groupByParent, Long parentId, String indent) {
        List<JSONObject> children = groupByParent.get(parentId);
        if (children == null) return;
        for (JSONObject child : children) {
            System.out.println(indent + "├── " + child.getString("deptName")
                    + " (ID:" + child.getLong("deptId") + ")");
            printTree(groupByParent, child.getLong("deptId"), indent + "│   ");
        }
    }

}
