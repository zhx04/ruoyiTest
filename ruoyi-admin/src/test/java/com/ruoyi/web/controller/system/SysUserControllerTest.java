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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SysUserControllerTest {

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


    // 成功获取用户列表
    @Test
    void testUser_01() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").exists())
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("========= 所有用户列表数据 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("总用户数：" + respJson.getInteger("total"));
        System.out.println("====================================");
        var userArray = respJson.getJSONArray("rows");
        for (int i = 0; i < userArray.size(); i++) {
            JSONObject user = userArray.getJSONObject(i);
            System.out.println("第 " + (i + 1) + " 个用户");
            System.out.println("用户ID：" + user.getLong("userId"));
            System.out.println("用户名：" + user.getString("userName"));
            System.out.println("昵称：" + user.getString("nickName"));
            System.out.println("邮箱：" + user.getString("email"));
            System.out.println("状态：" + user.getString("status"));
            System.out.println("------------------------------------");
        }
    }


    // 根据ID获取用户详情
   @Test
    void testUser_02() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/user/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.userName").value("admin"))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        JSONObject userInfo = respJson.getJSONObject("data");
        System.out.println("\n========= ID=1 用户详情 =========");
        System.out.println("用户ID：" + userInfo.getLong("userId"));
        System.out.println("登录用户名：" + userInfo.getString("userName"));
        System.out.println("昵称：" + userInfo.getString("nickName"));
        System.out.println("邮箱：" + userInfo.getString("email"));
        System.out.println("手机号：" + userInfo.getString("phonenumber"));
        System.out.println("性别：" + userInfo.getString("sex"));
        System.out.println("账号状态：" + userInfo.getString("status"));
        System.out.println("所属部门：" + userInfo.getString("deptName"));
    }

    // 添加用户成功
    @Test
    void testUser_03() throws Exception {
        String addUserJson = "{\n" +
                "    \"userName\": \"testuser006\",\n" +
                "    \"nickName\": \"测试用户006\",\n" +
                "    \"password\": \"123456\",\n" +
                "    \"email\": \"test006@qq.com\",\n" +
                "    \"phonenumber\": \"13866611111\",\n" +
                "    \"sex\": \"0\",\n" +
                "    \"status\": \"0\",\n" +
                "    \"deptId\": 103\n" +
                "}";

        MvcResult mvcResult = mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addUserJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 新增用户结果 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("提示信息：" + respJson.getString("msg"));
        System.out.println("新增用户成功！");
    }

    // 新增用户失败：用户已存在
    @Test
    void testUser_04() throws Exception {
        String repeatUserJson = "{\n" +
                "    \"userName\": \"admin\",\n" +
                "    \"nickName\": \"重复账号测试\",\n" +
                "    \"password\": \"123456\",\n" +
                "    \"email\": \"repeat@test.com\",\n" +
                "    \"phonenumber\": \"13900002222\",\n" +
                "    \"sex\": \"0\",\n" +
                "    \"status\": \"0\",\n" +
                "    \"deptId\": 103\n" +
                "}";
        MvcResult mvcResult = mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(repeatUserJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增用户'admin'失败，登录账号已存在"))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 新增用户-用户名重复异常结果 =========");
        System.out.println("响应业务码：" + respJson.getInteger("code"));
        System.out.println("错误提示信息：" + respJson.getString("msg"));
        System.out.println("用户名重复，新增操作拦截失败");
    }

    // 密码重置成功
    @Test
    void testUser_05() throws Exception {
        Long userId = 106L;
        String newPassword = "888888";
        String resetJson = "{\n" +
                "    \"userId\": " + userId + ",\n" +
                "    \"password\": \"" + newPassword + "\"\n" +
                "}";

        MvcResult mvcResult = mockMvc.perform(put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 重置密码结果 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("提示：" + respJson.getString("msg"));
        System.out.println("用户ID[" + userId + "] 密码已重置为：" + newPassword);
    }

    // 重置密码失败：空ID，参数校验失败
    @Test
    void testUser_06() throws Exception {
        String json = "{\n" +
                "    \"password\": \"666666\"\n" +
                "}";

        MvcResult result = mockMvc.perform(put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        String respStr = result.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 重置密码测试结果 =========");
        System.out.println("响应码: " + respJson.getInteger("code"));
        System.out.println("响应信息: " + respJson.getString("msg"));
    }

    // 重置密码失败：空密码
    @Test
    void testUser_07() throws Exception {
        String json = "{\n" +
                "    \"userId\": 107\n" +
                "}";

        MvcResult result = mockMvc.perform(put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        String respStr = result.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 重置密码测试：空密码 =========");
        System.out.println("响应码: " + respJson.getInteger("code"));
        System.out.println("响应信息: " + respJson.getString("msg"));
    }

    //重置密码失败：userid不存在
    @Test
    void testUser_08() throws Exception {
        String json = "{\n" +
                "    \"userId\": 999999,\n" +
                "    \"password\": \"666666\"\n" +
                "}";

        MvcResult result = mockMvc.perform(put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        String respStr = result.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 重置密码测试：用户ID不存在 =========");
        System.out.println("响应码: " + respJson.getInteger("code"));
        System.out.println("响应信息: " + respJson.getString("msg"));
    }
    // 重置密码失败：未登录
    @Test
    void testUser_09() throws Exception {
        String json = "{\n" +
                "    \"userId\": 106,\n" +
                "    \"password\": \"666666\"\n" +
                "}";

        MvcResult result = mockMvc.perform(put("/system/user/resetPwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        String respStr = result.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 重置密码测试：未登录 =========");
        System.out.println("响应码: " + respJson.getInteger("code"));
        System.out.println("响应信息: " + respJson.getString("msg"));
    }
    // 重置密码失败：修改管理员（用户不允许修改）
    @Test
    void testUser_10() throws Exception {
        String json = "{\n" +
                "    \"userId\": 1,\n" + // admin
                "    \"password\": \"666666\"\n" +
                "}";

        MvcResult result = mockMvc.perform(put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String respStr = result.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);

        System.out.println("\n========= 重置密码测试：修改管理员密码 =========");
        System.out.println("响应码: " + respJson.getInteger("code"));
        System.out.println("响应信息: " + respJson.getString("msg"));
    }

    // 重置密码：无密码修改权限
    @Test
    void testUser_11() throws Exception {
        String json = "{\n" +
                "    \"userId\": 106,\n" +
                "    \"password\": \"666666\"\n" +
                "}";

        MvcResult result = mockMvc.perform(put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        String respStr = result.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 重置密码测试：无权限 =========");
        System.out.println("响应码: " + respJson.getInteger("code"));
        System.out.println("响应信息: " + respJson.getString("msg"));
    }

    // 删除用户
    @Test
    void testUser_12() throws Exception {
        Long deleteUserId = 106L;
        MvcResult mvcResult = mockMvc.perform(delete("/system/user/" + deleteUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 删除用户结果 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("提示信息：" + respJson.getString("msg"));
        System.out.println("用户ID[" + deleteUserId + "] 已成功删除");
        // 删除后查询
        MvcResult listResult = mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        JSONObject listJson = JSON.parseObject(listResult.getResponse().getContentAsString());
        System.out.println("删除后剩余用户数量：" + listJson.getInteger("total"));
    }

    // 用户导出
    @Test
    void testUser_13() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/system/user/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();

        String fileName = "user_export_" + System.currentTimeMillis() + ".xlsx";
        String filePath = "C:\\Users\\31073\\Desktop\\" + fileName;  // 保存到桌面

        java.nio.file.Files.write(java.nio.file.Paths.get(filePath), excelBytes);

        System.out.println("\n========= 导出用户结果 =========");
        System.out.println("导出成功！");
        System.out.println("文件已保存到：" + filePath);
        System.out.println("文件大小：" + excelBytes.length + " 字节");
    }


    // 下载导入模板
    @Test
    void testUser_14() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/system/user/importTemplate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        byte[] templateBytes = mvcResult.getResponse().getContentAsByteArray();

        String desktopPath = "C:\\Users\\31073\\Desktop\\用户导入模板.xlsx";
        java.nio.file.Files.write(java.nio.file.Paths.get(desktopPath), templateBytes);

        System.out.println("\n========= 导入模板下载成功 =========");
        System.out.println("模板已保存到：" + desktopPath);
    }

    // 读取Excel并导入用户
   @Test
    void testUser_15() throws Exception {
        String excelPath = "C:\\Users\\31073\\Desktop\\用户导入模板.xlsx";
        java.io.File file = new java.io.File(excelPath);
        if (!file.exists()) {
            System.out.println("文件不存在：" + excelPath);
            return;
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        org.springframework.mock.web.MockMultipartFile multipartFile =
                new org.springframework.mock.web.MockMultipartFile(
                        "file",
                        "用户导入模板.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        bytes
                );
        MvcResult mvcResult = mockMvc.perform(multipart("/system/user/importData")
                        .file(multipartFile)
                        .param("updateSupport", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 用户导入结果 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("结果信息：" + respJson.getString("msg"));
        System.out.println("导入完成！");
    }

    // 导入非Excel文件
    @Test
    void testUser_16() throws Exception {
        String fakeFileContent = "普通txt文件";
        MockMultipartFile notExcelFile = new MockMultipartFile(
                "file",
                "notExcel.txt",
                "text/plain",
                fakeFileContent.getBytes()
        );
        MvcResult mvcResult = mockMvc.perform(multipart("/system/user/importData")
                        .file(notExcelFile)
                        .param("updateSupport", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        String respStr = mvcResult.getResponse().getContentAsString();
        JSONObject respJson = JSON.parseObject(respStr);
        System.out.println("\n========= 导入非Excel文件测试 =========");
        System.out.println("响应码：" + respJson.getInteger("code"));
        System.out.println("错误信息：" + respJson.getString("msg"));
        System.out.println("上传非Excel文件，导入失败");
    }


    // 未登录调用导出接口
    @Test
    void testUser_17() throws Exception {
        // 不带token请求
        MvcResult mvcResult = mockMvc.perform(post("/system/user/export")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        int status = mvcResult.getResponse().getStatus();
        String response = mvcResult.getResponse().getContentAsString();
        System.out.println("========= 无权导出测试 =========");
        System.out.println("HTTP状态码：" + status);
        System.out.println("返回内容：" + response);
    }

}
