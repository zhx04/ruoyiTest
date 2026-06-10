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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.system.mapper.SysDictDataMapper;

@ExtendWith(MockitoExtension.class)
public class SysDictDataServiceTest {

    @InjectMocks
    private SysDictDataServiceImpl dictDataService;

    @Mock
    private SysDictDataMapper dictDataMapper;

    /**
     * RY-DD-01: 根据字典数据ID查询信息
     * 设计方法：等价类划分（有效输入）
     */
    @Test
    public void testRY_DD_01() {
        System.out.println(">>> 开始执行 RY-DD-01: 根据字典数据ID查询信息测试...");
        SysDictData data = new SysDictData();
        data.setDictCode(1L);
        data.setDictLabel("男");

        when(dictDataMapper.selectDictDataById(1L)).thenReturn(data);

        SysDictData result = dictDataService.selectDictDataById(1L);
        assertNotNull(result);
        assertEquals("男", result.getDictLabel());
        System.out.println("结果：成功获取到指定 ID 的字典项标签内容。");
    }

    /**
     * RY-DD-02: 根据字典类型和字典键值查询字典数据标签
     * 设计方法：等价类划分（有效类型与键值组合）+ 参数化测试
     * 覆盖多组字典类型和键值组合
     */
    @ParameterizedTest(name = "RY-DD-02 参数映射校验 [{index}] type={0}, value={1}, expected={2}")
    @CsvSource({
            "sys_user_sex,1,男",
            "sys_user_sex,0,女",
            "sys_common_status,0,正常",
            "sys_common_status,1,停用",
            "sys_notice_type,1,通知",
            "sys_notice_type,2,公告",
            "sys_yes_no,Y,是",
            "sys_yes_no,N,否",
            "sys_oper_type,1,新增",
            "sys_oper_type,2,修改"
    })
    public void testRY_DD_02(String dictType, String dictValue, String expectedLabel) {
        System.out.println(">>> 开始执行 RY-DD-02: 查询字典标签名称测试... type=" + dictType + ", value=" + dictValue);
        when(dictDataMapper.selectDictLabel(dictType, dictValue)).thenReturn(expectedLabel);

        String label = dictDataService.selectDictLabel(dictType, dictValue);
        assertEquals(expectedLabel, label);
        System.out.println("结果：正确返回了类型=" + dictType + "、键值=" + dictValue + " 的标签名=" + label + "。");
    }

    /**
     * RY-DD-03: 新增保存字典数据信息
     * 设计方法：决策表（新增数据并同步刷新缓存）
     */
    @Test
    public void testRY_DD_03() {
        System.out.println(">>> 开始执行 RY-DD-03: 新增字典数据测试...");
        try (MockedStatic<DictUtils> utils = mockStatic(DictUtils.class)) {
            SysDictData data = new SysDictData();
            data.setDictType("sys_user_sex");
            data.setDictLabel("未知");
            data.setDictValue("2");

            when(dictDataMapper.insertDictData(data)).thenReturn(1);
            when(dictDataMapper.selectDictDataByType("sys_user_sex")).thenReturn(new ArrayList<>());

            int rows = dictDataService.insertDictData(data);
            assertEquals(1, rows);
            // 验证是否调用了缓存刷新逻辑
            utils.verify(() -> DictUtils.setDictCache(eq("sys_user_sex"), any()), times(1));
            System.out.println("结果：数据插入成功，且所属字典类型的缓存已同步更新。");
        }
    }

    /**
     * RY-DD-04: 修改保存字典数据信息
     * 设计方法：决策表（修改数据并同步刷新缓存）
     */
    @Test
    public void testRY_DD_04() {
        System.out.println(">>> 开始执行 RY-DD-04: 修改字典数据测试...");
        try (MockedStatic<DictUtils> utils = mockStatic(DictUtils.class)) {
            SysDictData data = new SysDictData();
            data.setDictCode(100L);
            data.setDictType("sys_user_sex");
            data.setDictLabel("男-修改");

            when(dictDataMapper.updateDictData(data)).thenReturn(1);
            when(dictDataMapper.selectDictDataByType("sys_user_sex")).thenReturn(new ArrayList<>());

            int rows = dictDataService.updateDictData(data);
            assertEquals(1, rows);
            // 验证是否调用了缓存刷新
            utils.verify(() -> DictUtils.setDictCache(eq("sys_user_sex"), any()), times(1));
            System.out.println("结果：数据修改完毕，且触发了相关字典类型的缓存刷新。");
        }
    }

    /**
     * RY-DD-05: 批量删除字典数据
     * 设计方法：边界值分析（删除1个及多个数据）
     */
    @Test
    public void testRY_DD_05() {
        System.out.println(">>> 开始执行 RY-DD-05: 批量删除字典数据项测试...");
        try (MockedStatic<DictUtils> utils = mockStatic(DictUtils.class)) {
            Long[] ids = {100L, 101L};
            SysDictData data1 = new SysDictData();
            data1.setDictType("sys_user_sex");

            // 模拟删除逻辑中的查询动作
            when(dictDataMapper.selectDictDataById(anyLong())).thenReturn(data1);
            when(dictDataMapper.deleteDictDataById(anyLong())).thenReturn(1);
            when(dictDataMapper.selectDictDataByType("sys_user_sex")).thenReturn(new ArrayList<>());

            dictDataService.deleteDictDataByIds(ids);

            // 验证Mapper被调用次数
            verify(dictDataMapper, times(2)).deleteDictDataById(anyLong());
            // 验证缓存刷新（循环调用2次）
            utils.verify(() -> DictUtils.setDictCache(eq("sys_user_sex"), any()), times(2));
            System.out.println("结果：批量删除记录完成，对应类型的缓存也随之刷新。");
        }
    }
}
