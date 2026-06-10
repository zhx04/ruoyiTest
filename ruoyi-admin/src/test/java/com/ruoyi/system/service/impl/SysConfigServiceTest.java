package com.ruoyi.system.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;

@ExtendWith(MockitoExtension.class)
public class SysConfigServiceTest {

    @InjectMocks
    private SysConfigServiceImpl configService;

    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private RedisCache redisCache;

    /**
     * RY-CF-01: 根据参数ID查询参数配置信息
     * 设计方法：决策表（条件：配置ID在库中存在；动作：返回详情）
     */
    @Test
    public void testRY_CF_01() {
        System.out.println(">>> 开始执行 RY-CF-01: 根据参数ID查询测试 [决策表规则：有效ID路径]...");
        SysConfig config = new SysConfig();
        config.setConfigId(1L);
        config.setConfigName("测试配置");

        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(config);

        SysConfig result = configService.selectConfigById(1L);
        assertNotNull(result);
        System.out.println("结果：成功获取参数：" + result.getConfigName());
    }

    /**
     * RY-CF-02: 根据键名查询参数值（缓存命中）
     * 设计方法：决策表
     * 动作A1：查询 Redis 缓存
     * 动作A2：直接返回缓存值
     * 动作A3：不触发数据库查询
     */
    @Test
    public void testRY_CF_02() {
        System.out.println(">>> 开始执行 RY-CF-02: 参数查询 [决策表规则：缓存命中路径]...");
        String key = "sys.index.sideTheme";
        String value = "theme-dark";

        when(redisCache.getCacheObject(anyString())).thenReturn(value);

        String result = configService.selectConfigByKey(key);
        assertEquals(value, result);
        verify(configMapper, never()).selectConfig(any(SysConfig.class));
        System.out.println("结果：由于缓存命中，成功跳过数据库查询逻辑。");
    }

    /**
     * RY-CF-03: 根据键名查询参数值（缓存缺失回填）
     * 设计方法：决策表
     * 动作A1：查询 Redis 缓存
     * 动作A2：缓存未命中后查询数据库
     * 动作A3：将查询结果回填 Redis
     * 动作A4：返回数据库查询结果
     */
    @Test
    public void testRY_CF_03() {
        System.out.println(">>> 开始执行 RY-CF-03: 参数查询 [决策表规则：缓存缺失-查库回填路径]...");
        String key = "sys.index.sideTheme";
        String value = "theme-dark";
        SysConfig config = new SysConfig();
        config.setConfigValue(value);

        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(config);

        String result = configService.selectConfigByKey(key);
        assertEquals(value, result);
        verify(redisCache, times(1)).setCacheObject(anyString(), eq(value));
        System.out.println("结果：缓存缺失，已从数据库同步至缓存。");
    }

    /**
     * RY-CF-02-P: 根据键名查询参数值 - 参数化测试
     * 设计方法：决策表 + 参数化（多个不同键名的缓存命中/缺失场景）
     */
    @ParameterizedTest(name = "RY-CF-02-P 参数查询 [{index}] key={0}, cached={1}, expected={2}")
    @CsvSource({
            "sys.index.sideTheme,true,theme-dark",
            "sys.index.skinName,true,skin-blue",
            "sys.index.sideTheme,false,theme-dark",
            "sys.account.captchaEnabled,true,true",
            "sys.account.initPassword,false,123456"
    })
    public void testRY_CF_02_P(String key, boolean cacheHit, String expectedValue) {
        System.out.println(">>> 开始执行 RY-CF-02-P: 参数查询参数化测试 key=" + key + ", cacheHit=" + cacheHit);

        SysConfig config = new SysConfig();
        config.setConfigValue(expectedValue);

        if (cacheHit) {
            when(redisCache.getCacheObject(anyString())).thenReturn(expectedValue);
        } else {
            when(redisCache.getCacheObject(anyString())).thenReturn(null);
            when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(config);
        }

        String result = configService.selectConfigByKey(key);
        assertEquals(expectedValue, result);

        if (!cacheHit) {
            verify(redisCache, times(1)).setCacheObject(anyString(), eq(expectedValue));
        }
        System.out.println("结果：查询参数 key=" + key + " 返回 value=" + result + "，缓存命中=" + cacheHit);
    }

    /**
     * RY-CF-04: 新增参数配置
     * 设计方法：决策表
     * 动作A1：执行数据库插入
     * 动作A2：写入 Redis 缓存
     * 动作A3：返回受影响行数
     */
    @Test
    public void testRY_CF_04() {
        System.out.println(">>> 开始执行 RY-CF-04: 新增参数 [决策表规则：正常插入路径]...");
        SysConfig config = new SysConfig();
        config.setConfigKey("test_key");
        config.setConfigValue("test_value");

        when(configMapper.insertConfig(config)).thenReturn(1);

        int rows = configService.insertConfig(config);
        assertEquals(1, rows);
        verify(redisCache, times(1)).setCacheObject(anyString(), eq("test_value"));
        System.out.println("结果：参数插入成功并同步写入 Redis。");
    }

    /**
     * RY-CF-04-P: 新增参数配置 - 参数化测试
     * 设计方法：决策表 + 参数化（多种参数键值组合）
     */
    @ParameterizedTest(name = "RY-CF-04-P 新增参数 [{index}] key={0}, value={1}")
    @CsvSource({
            "sys.index.sideTheme,theme-dark",
            "sys.index.skinName,skin-blue",
            "sys.account.initPassword,123456",
            "sys.account.registerUser,false"
    })
    public void testRY_CF_04_P(String configKey, String configValue) {
        System.out.println(">>> 开始执行 RY-CF-04-P: 新增参数参数化测试 key=" + configKey + ", value=" + configValue);
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);

        when(configMapper.insertConfig(config)).thenReturn(1);

        int rows = configService.insertConfig(config);
        assertEquals(1, rows);
        verify(redisCache, times(1)).setCacheObject(anyString(), eq(configValue));
        System.out.println("结果：参数 key=" + configKey + " 插入成功并同步写入 Redis。");
    }

    /**
     * RY-CF-05: 批量删除参数信息（异常拦截）
     * 设计方法：决策表
     * 动作A1：读取待删除参数信息
     * 动作A2：判断是否为系统内置参数
     * 动作A3：若为内置参数则抛出业务异常
     */
    @Test
    public void testRY_CF_05() {
        System.out.println(">>> 开始执行 RY-CF-05: 删除测试 [决策表规则：内置参数保护规则]...");
        Long[] ids = {1L};
        SysConfig config = new SysConfig();
        config.setConfigType(UserConstants.YES); // 内置参数 ("Y")

        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(config);

        assertThrows(ServiceException.class, () -> {
            configService.deleteConfigByIds(ids);
        });
        System.out.println("结果：校验成功！系统已正确拒绝删除内置参数。");
    }

    /**
     * RY-CF-05-P: 批量删除参数信息 - 参数化测试
     * 设计方法：边界值分析（内置参数 vs 非内置参数）
     */
    @ParameterizedTest(name = "RY-CF-05-P 删除参数 [{index}] configType={0}, shouldThrow={1}")
    @CsvSource({
            "Y,true",
            "N,false"
    })
    public void testRY_CF_05_P(String configType, boolean shouldThrow) {
        System.out.println(">>> 开始执行 RY-CF-05-P: 删除参数参数化测试 configType=" + configType);
        Long[] ids = {1L};
        SysConfig config = new SysConfig();
        config.setConfigType(configType);
        config.setConfigKey("test_key");
        config.setConfigValue("test_value");

        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(config);
        if (!shouldThrow) {
            when(configMapper.deleteConfigById(anyLong())).thenReturn(1);
        }

        if (shouldThrow) {
            assertThrows(ServiceException.class, () -> {
                configService.deleteConfigByIds(ids);
            });
            System.out.println("结果：内置参数删除被正确拦截，抛出 ServiceException。");
        } else {
            assertDoesNotThrow(() -> configService.deleteConfigByIds(ids));
            verify(configMapper, times(1)).deleteConfigById(anyLong());
            System.out.println("结果：非内置参数删除成功。");
        }
    }
}
