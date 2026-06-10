package com.ruoyi.system.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.core.domain.entity.SysDictType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.mapper.SysDictTypeMapper;

import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
public class SysDictTypeServiceTest {

    @InjectMocks
    private SysDictTypeServiceImpl dictTypeService;

    @Mock
    private SysDictTypeMapper dictTypeMapper;

    @Mock
    private SysDictDataMapper dictDataMapper;

    /**
     * 编号：RY-DT-01
     * 描述：正常新增字典类型
     * 等价类：覆盖 有效等价类(1), (5)
     */
    @Test
    public void testRY_DT_01() {
        System.out.println(">>> 开始执行 RY-DT-01: 正常新增字典类型测试...");
        try (MockedStatic<DictUtils> utils = mockStatic(DictUtils.class)) {
            SysDictType dict = new SysDictType();
            dict.setDictType("sys_bus_status");
            dict.setDictName("业务状态");

            when(dictTypeMapper.insertDictType(dict)).thenReturn(1);

            int rows = dictTypeService.insertDictType(dict);
            assertEquals(1, rows);
            utils.verify(() -> DictUtils.setDictCache(eq("sys_bus_status"), any()), times(1));
            System.out.println("结果：字典类型记录插入成功，且同步刷新了 Redis 缓存。");
        }
    }

    /**
     * 编号：RY-DT-02
     * 描述：新增已存在的字典类型（标识冲突）
     * 等价类：覆盖 无效等价类(3)
     */
    @Test
    public void testRY_DT_02() {
        System.out.println(">>> 开始执行 RY-DT-02: 校验重复字典类型测试...");
        SysDictType dict = new SysDictType();
        dict.setDictType("sys_user_sex");

        SysDictType existing = new SysDictType();
        existing.setDictId(1L);

        when(dictTypeMapper.checkDictTypeUnique("sys_user_sex")).thenReturn(existing);

        boolean result = dictTypeService.checkDictTypeUnique(dict);
        assertFalse(result, "预期返回非唯一状态");
        System.out.println("结果：成功检测到该字典类型已存在，系统返回非唯一状态。");
    }

    /**
     * RY-DT-02-P: 校验字典类型唯一性 - 参数化测试
     * 设计方法：等价类划分（不同类型的字典唯一性校验）
     */
    @ParameterizedTest(name = "RY-DT-02-P 字典唯一性校验 [{index}] type={0}, exists={1}, expected={2}")
    @CsvSource({
            "sys_user_sex,true,false",
            "sys_notice_type,true,false",
            "sys_oper_type,true,false",
            "new_type_not_exists,false,true",
            "another_new_type,false,true"
    })
    public void testRY_DT_02_P(String dictType, boolean exists, boolean expected) {
        System.out.println(">>> 开始执行 RY-DT-02-P: 字典唯一性参数化测试 type=" + dictType + ", exists=" + exists);
        SysDictType dict = new SysDictType();
        dict.setDictType(dictType);

        if (exists) {
            SysDictType existing = new SysDictType();
            existing.setDictId(1L);
            when(dictTypeMapper.checkDictTypeUnique(dictType)).thenReturn(existing);
        } else {
            when(dictTypeMapper.checkDictTypeUnique(dictType)).thenReturn(null);
        }

        boolean result = dictTypeService.checkDictTypeUnique(dict);
        assertEquals(expected, result);
        System.out.println("结果：类型=" + dictType + " 唯一性校验结果=" + result);
    }

    /**
     * 编号：RY-DT-03
     * 描述：删除无关联数据的字典类型
     * 等价类：覆盖 有效等价类(7)
     */
    @Test
    public void testRY_DT_03() {
        System.out.println(">>> 开始执行 RY-DT-03: 删除无关联数据的字典类型测试...");
        // 使用 try-with-resources 确保静态 Mock 被正确关闭，防止干扰后续测试
        try (MockedStatic<DictUtils> utils = mockStatic(DictUtils.class)) {
            Long[] ids = {10L};
            SysDictType dict = new SysDictType();
            dict.setDictType("test_type");

            when(dictTypeMapper.selectDictTypeById(10L)).thenReturn(dict);
            when(dictDataMapper.countDictDataByType("test_type")).thenReturn(0);

            dictTypeService.deleteDictTypeByIds(ids);

            verify(dictTypeMapper, times(1)).deleteDictTypeById(10L);
            // 验证静态方法是否被调用
            utils.verify(() -> DictUtils.removeDictCache(eq("test_type")), times(1));
            System.out.println("结果：物理记录删除成功，且该类型的 Redis 缓存已注销。");
        }
    }


    /**
     * 编号：RY-DT-04
     * 描述：尝试删除已分配数据的字典类型
     * 等价类：覆盖 无效等价类(8)
     */
    @Test
    public void testRY_DT_04() {
        System.out.println(">>> 开始执行 RY-DT-04: 删除已分配数据的字典类型测试...");
        Long[] ids = {1L};
        SysDictType dict = new SysDictType();
        dict.setDictType("sys_user_sex");

        when(dictTypeMapper.selectDictTypeById(1L)).thenReturn(dict);
        when(dictDataMapper.countDictDataByType("sys_user_sex")).thenReturn(5);

        assertThrows(ServiceException.class, () -> {
            dictTypeService.deleteDictTypeByIds(ids);
        }, "存在关联数据时应抛出业务异常");
        System.out.println("结果：捕获到预期的 ServiceException，逻辑正确地阻止了因有关联数据而触发的删除。");
    }

    /**
     * 编号：RY-DT-05
     * 描述：修改字典类型名称
     * 等价类：覆盖 有效等价类(2), (5)
     */
     @Test
    public void testRY_DT_05() {
         System.out.println(">>> 开始执行 RY-DT-05: 修改字典类型名称测试...");
         try (MockedStatic<DictUtils> utils = mockStatic(DictUtils.class)) {
             SysDictType dict = new SysDictType();
             dict.setDictId(1L);
             dict.setDictType("sys_user_sex");
             dict.setDictName("用户性别新名称");

             when(dictTypeMapper.selectDictTypeById(1L)).thenReturn(dict);
             when(dictTypeMapper.updateDictType(any())).thenReturn(1);
             // 模拟 updateDictType 内部会调用的查询逻辑
             when(dictDataMapper.selectDictDataByType("sys_user_sex")).thenReturn(new ArrayList<>());

             int rows = dictTypeService.updateDictType(dict);

             assertEquals(1, rows);
             // 验证静态方法是否被调用
             utils.verify(() -> DictUtils.setDictCache(eq("sys_user_sex"), any()), times(1));
             System.out.println("结果：字典名称更新成功，且 Redis 中的字典项数据已重新加载。");
         }
     }

    /**
     * RY-DT-06: 查询字典类型列表
     * 设计方法：等价类划分（查询全部字典类型）
     */
    @Test
    public void testRY_DT_06() {
        System.out.println(">>> 开始执行 RY-DT-06: 查询全部字典类型测试...");
        java.util.List<SysDictType> list = new ArrayList<>();
        list.add(new SysDictType());
        when(dictTypeMapper.selectDictTypeAll()).thenReturn(list);

        java.util.List<SysDictType> result = dictTypeService.selectDictTypeAll();
        assertEquals(1, result.size());
        System.out.println("结果：成功查询到 " + result.size() + " 条字典类型记录。");
    }
 }
