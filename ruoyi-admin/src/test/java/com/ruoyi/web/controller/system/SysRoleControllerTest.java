package com.ruoyi.web.controller.system;

import com.alibaba.fastjson2.JSON;
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

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest
public class SysRoleControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    private MockMvc mockMvc;
    private String token;
    private String testToken;
    private String commonToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(jwtAuthenticationTokenFilter)
                .build();
        token = login("admin", "admin123");
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

    // 正确查询角色列表
    @Test
    void testRole_01() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/role/list")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").exists())
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 角色列表查询成功 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("总角色数量：" + respJson.getInteger("total"));
        System.out.println("=====================================");
        var roleArray = respJson.getJSONArray("rows");
        for (int i = 0; i < roleArray.size(); i++) {
            JSONObject role = roleArray.getJSONObject(i);
            System.out.println("第 " + (i + 1) + " 个角色");
            System.out.println("角色ID：" + role.getLong("roleId"));
            System.out.println("角色名称：" + role.getString("roleName"));
            System.out.println("权限字符：" + role.getString("roleKey"));
            System.out.println("角色状态：" + role.getString("status"));
            System.out.println("-------------------------------------");
        }
    }


    // 根据ID查询角色详情，查询roleId=2
    @Test
    void testRole_02() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/role/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleId").value(2))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        JSONObject data = respJson.getJSONObject("data");

        System.out.println("========= 根据ID查询角色 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("角色ID：" + data.getLong("roleId"));
        System.out.println("角色名称：" + data.getString("roleName"));
        System.out.println("权限字符：" + data.getString("roleKey"));
    }

    // 新增角色
   @Test
    void testRole_03() throws Exception {
        String json = "{\n" +
                "    \"roleName\": \"测试角色001\",\n" +
                "    \"roleKey\": \"test001\",\n" +
                "    \"roleSort\": 99,\n" +
                "    \"status\": \"0\",\n" +
                "    \"menuIds\": []\n" +
                "}";
        MvcResult mvcResult = mockMvc.perform(post("/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 新增角色成功 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
    }

    // 批量给用户授权
    @Test
    void testRole_04() throws Exception {
        Long roleId = 2L;
        Long[] userIds = {107L, 109L};
        MvcResult mvcResult = mockMvc.perform(put("/system/role/authUser/selectAll")
                        .header("Authorization", "Bearer " + token)
                        .param("roleId", String.valueOf(roleId))
                        .param("userIds", String.join(",",
                                Arrays.stream(userIds).map(String::valueOf).toArray(String[]::new)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        String userIdStr = String.join(", ",
                Arrays.stream(userIds).map(String::valueOf).toArray(String[]::new));

        System.out.println("========= 批量用户授权 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("====================================");
        System.out.println("角色ID：" + roleId);
        System.out.println("用户ID：" + userIdStr);
    }


    // 批量授权失败 - 缺少参数
    @Test
    void testRole_05() throws Exception {
        Long roleId = 2L;
        MvcResult mvcResult = mockMvc.perform(put("/system/role/authUser/selectAll")
                        .header("Authorization", "Bearer " + token)
                        .param("roleId", String.valueOf(roleId))
                        // 不传userIds
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 角色授权用户 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("失败原因：未传递用户ID userIds");
    }



    // 批量取消用户授权
    @Test
    void testRole_06() throws Exception {
        Long roleId = 2L;
        Long[] userIds = {107L, 108L};
        String userIdStr = String.join(",",
                Arrays.stream(userIds).map(String::valueOf).toArray(String[]::new));
        MvcResult mvcResult = mockMvc.perform(put("/system/role/authUser/cancelAll")
                        .header("Authorization", "Bearer " + token)
                        .param("roleId", String.valueOf(roleId))
                        .param("userIds", userIdStr)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        String userIdPrintStr = String.join(", ",
                Arrays.stream(userIds).map(String::valueOf).toArray(String[]::new));
        System.out.println("========= 批量取消用户授权 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("====================================");
        System.out.println("角色ID：" + roleId);
        System.out.println("用户ID：" + userIdPrintStr);
    }




    // 取消单个用户授权
    @Test
    void testRole_07() throws Exception {
        Long roleId = 2L;
        Long userId = 107L;
        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"userId\": " + userId + "\n" +
                "}";
        MvcResult mvcResult = mockMvc.perform(put("/system/role/authUser/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("========= 取消单个用户授权 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("====================================");
        System.out.println("角色ID：" + roleId);
        System.out.println("用户ID：" + userId);
    }


    // 查询已分配该角色的用户列表
    @Test
    void testRole_08() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/role/authUser/allocatedList")
                        .header("Authorization", "Bearer " + token)
                        .param("roleId", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 已分配当前角色的用户列表 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("用户总数：" + respJson.getInteger("total"));
        System.out.println("==========================================");

        var userArray = respJson.getJSONArray("rows");
        if (userArray != null && !userArray.isEmpty()) {
            for (int i = 0; i < userArray.size(); i++) {
                JSONObject user = userArray.getJSONObject(i);
                System.out.println("第 " + (i + 1) + " 个用户");
                System.out.println("用户ID：" + user.getLong("userId"));
                System.out.println("用户名：" + user.getString("userName"));
                System.out.println("昵称：" + user.getString("nickName"));
                System.out.println("部门：" + user.getJSONObject("dept").getString("deptName"));
                System.out.println("--------------------------------------");
            }
        } else {
            System.out.println("暂无已分配用户");
        }
    }

    // 查询未分配该角色的用户列表
    @Test
    void testRole_09() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/role/authUser/unallocatedList")
                        .header("Authorization", "Bearer " + token)
                        .param("roleId", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 未分配当前角色的用户列表 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("用户总数：" + respJson.getInteger("total"));
        System.out.println("==========================================");

        var userArray = respJson.getJSONArray("rows");
        if (userArray != null && !userArray.isEmpty()) {
            for (int i = 0; i < userArray.size(); i++) {
                JSONObject user = userArray.getJSONObject(i);
                System.out.println("第 " + (i + 1) + " 个用户");
                System.out.println("用户ID：" + user.getLong("userId"));
                System.out.println("用户名：" + user.getString("userName"));
                System.out.println("昵称：" + user.getString("nickName"));
                System.out.println("--------------------------------------");
            }
        } else {
            System.out.println("暂无未分配用户");
        }
    }
    // 修改角色状态
    @Test
    void testRole_10() throws Exception {
        Long roleId = 104L;
        String status = "1";  // 0=启用，1=禁用
        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"status\": \"" + status + "\"\n" +
                "}";
        MvcResult mvcResult = mockMvc.perform(put("/system/role/changeStatus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 修改角色状态 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("角色ID：" + roleId);
        System.out.println("修改后状态：" + (status.equals("0") ? "启用" : "禁用"));
    }



    // 修改角色数据权限
   @Test
    void testRole_11() throws Exception {
        Long roleId = 104L;
        String targetDataScope = "5"; // 改为进本人数据权限
        // 查询原有权限
        MvcResult detailRes = mockMvc.perform(get("/system/role/" + roleId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        JSONObject detailJson = JSON.parseObject(detailRes.getResponse().getContentAsString());
        String oldScope = detailJson.getJSONObject("data").getString("dataScope");

        String oldScopeText = switch (oldScope) {
            case "1" -> "全部数据权限";
            case "2" -> "自定数据权限";
            case "3" -> "本部门数据权限";
            case "4" -> "本部门及以下数据权限";
            case "5" -> "仅本人数据权限";
            default -> "未知";
        };

        String newScopeText = switch (targetDataScope) {
            case "1" -> "全部数据权限";
            case "2" -> "自定数据权限";
            case "3" -> "本部门数据权限";
            case "4" -> "本部门及以下数据权限";
            case "5" -> "仅本人数据权限";
            default -> "未知";
        };

        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"dataScope\": \"" + targetDataScope + "\",\n" +
                "    \"deptIds\": []\n" +
                "}";

        MvcResult mvcResult = mockMvc.perform(put("/system/role/dataScope")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 修改数据权限 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("角色ID：" + roleId);
        System.out.println("修改前权限：" + oldScopeText);
        System.out.println("修改后权限：" + newScopeText);
    }

    // 修改角色数据权限失败：传入无效的数据权限值
   @Test
    void testRole_12() throws Exception {
        Long roleId = 2L;
        String invalidDataScope = "9";//无效值
        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"dataScope\": \"" + invalidDataScope + "\",\n" +
                "    \"deptIds\": []\n" +
                "}";
        MvcResult mvcResult = mockMvc.perform(put("/system/role/dataScope")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 无效数据权限值 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("角色ID：" + roleId);
        System.out.println("传入无效权限值：" + invalidDataScope);
    }

    // 修改角色数据权限失败：传入超长的数据权限值
    @Test
    void testRole_13() throws Exception {
        Long roleId = 2L;
        String invalidDataScope = "99";//无效值
        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"dataScope\": \"" + invalidDataScope + "\",\n" +
                "    \"deptIds\": []\n" +
                "}";
        MvcResult mvcResult = mockMvc.perform(put("/system/role/dataScope")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 无效数据权限值 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
        System.out.println("角色ID：" + roleId);
        System.out.println("传入无效权限值：" + invalidDataScope);
    }

    //管理员数据权限修改
    @Test
    void testRole_14() throws Exception {
        Long roleId = 1L; // 超级管理员
        String targetDataScope = "5";

        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"dataScope\": \"" + targetDataScope + "\",\n" +
                "    \"deptIds\": []\n" +
                "}";

        MvcResult mvcResult = mockMvc.perform(put("/system/role/dataScope")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("========= 修改管理员角色数据权限 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
    }

    //修改权限：未登录
    @Test
    void testRole_15() throws Exception {
        Long roleId = 1L;
        String targetDataScope = "5";

        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"dataScope\": \"" + targetDataScope + "\",\n" +
                "    \"deptIds\": []\n" +
                "}";

        MvcResult mvcResult = mockMvc.perform(put("/system/role/dataScope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("========= 未登录修改角色数据权限 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
    }

    //修改权限：无权限
    @Test
    void testRole_16() throws Exception {
        Long roleId = 1L;
        String targetDataScope = "5";

        String jsonBody = "{\n" +
                "    \"roleId\": " + roleId + ",\n" +
                "    \"dataScope\": \"" + targetDataScope + "\",\n" +
                "    \"deptIds\": []\n" +
                "}";

        MvcResult mvcResult = mockMvc.perform(put("/system/role/dataScope")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("========= 无权限修改角色数据权限 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("响应信息：" + respJson.getString("msg"));
    }
}