package com.ruoyi.system.service.impl;

import static org.junit.jupiter.api.Assertions.*;
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

import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.mapper.SysNoticeMapper;

@ExtendWith(MockitoExtension.class)
public class SysNoticeServiceTest {

    @InjectMocks
    private SysNoticeServiceImpl noticeService;

    @Mock
    private SysNoticeMapper noticeMapper;

    /**
     * RY-NT-01: 根据公告ID查询公告信息
     * 设计方法：等价类划分（有效输入）
     */
    @Test
    public void testRY_NT_01() {
        System.out.println(">>> 开始执行 RY-NT-01: 根据公告ID查询详情测试...");
        SysNotice notice = new SysNotice();
        notice.setNoticeId(1L);
        notice.setNoticeTitle("温馨提示");

        when(noticeMapper.selectNoticeById(1L)).thenReturn(notice);

        SysNotice result = noticeService.selectNoticeById(1L);
        assertNotNull(result);
        assertEquals("温馨提示", result.getNoticeTitle());
        System.out.println("结果：成功查询到公告详情，标题验证匹配。");
    }

    /**
     * RY-NT-01-P: 根据公告ID查询 - 参数化测试
     * 设计方法：等价类划分 + 参数化（多个公告ID及标题组合）
     */
    @ParameterizedTest(name = "RY-NT-01-P 公告查询 [{index}] id={0}, title={1}")
    @CsvSource({
            "1,温馨提示",
            "2,系统维护公告",
            "3,放假通知",
            "100,测试公告100"
    })
    public void testRY_NT_01_P(Long noticeId, String noticeTitle) {
        System.out.println(">>> 开始执行 RY-NT-01-P: 公告查询参数化测试 id=" + noticeId + ", title=" + noticeTitle);
        SysNotice notice = new SysNotice();
        notice.setNoticeId(noticeId);
        notice.setNoticeTitle(noticeTitle);

        when(noticeMapper.selectNoticeById(noticeId)).thenReturn(notice);

        SysNotice result = noticeService.selectNoticeById(noticeId);
        assertNotNull(result);
        assertEquals(noticeTitle, result.getNoticeTitle());
        System.out.println("结果：成功查询到 ID=" + noticeId + " 的公告，标题=" + result.getNoticeTitle());
    }

    /**
     * RY-NT-02: 查询公告列表
     * 设计方法：等价类划分（分页或过滤查询）
     */
    @Test
    public void testRY_NT_02() {
        System.out.println(">>> 开始执行 RY-NT-02: 分页/关键字查询公告列表测试...");
        List<SysNotice> list = new ArrayList<>();
        list.add(new SysNotice());
        when(noticeMapper.selectNoticeList(any(SysNotice.class))).thenReturn(list);

        List<SysNotice> result = noticeService.selectNoticeList(new SysNotice());
        assertEquals(1, result.size());
        System.out.println("结果：列表查询服务返回成功，记录数校验一致。");
    }

    /**
     * RY-NT-03: 新增公告
     * 设计方法：决策表（新增不同类型或状态的公告）
     */
    @Test
    public void testRY_NT_03() {
        System.out.println(">>> 开始执行 RY-NT-03: 发布公告测试...");
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("系统维护公告");
        notice.setNoticeType("1"); // 1-通知 2-公告

        when(noticeMapper.insertNotice(notice)).thenReturn(1);

        int rows = noticeService.insertNotice(notice);
        assertEquals(1, rows);
        System.out.println("结果：新公告发布成功，数据库插入记录 1 条。");
    }

    /**
     * RY-NT-03-P: 新增公告 - 参数化测试
     * 设计方法：决策表 + 参数化（不同类型和状态的公告组合）
     */
    @ParameterizedTest(name = "RY-NT-03-P 新增公告 [{index}] type={0}, title={1}")
    @CsvSource({
            "1,系统维护通知",
            "1,放假通知",
            "2,公司规章制度公告",
            "2,年度评优公告"
    })
    public void testRY_NT_03_P(String noticeType, String noticeTitle) {
        System.out.println(">>> 开始执行 RY-NT-03-P: 新增公告参数化测试 type=" + noticeType + ", title=" + noticeTitle);
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle(noticeTitle);
        notice.setNoticeType(noticeType);

        when(noticeMapper.insertNotice(notice)).thenReturn(1);

        int rows = noticeService.insertNotice(notice);
        assertEquals(1, rows);
        System.out.println("结果：新公告（类型=" + noticeType + ", 标题=" + noticeTitle + "）发布成功。");
    }

    /**
     * RY-NT-04: 修改公告
     * 设计方法：等价类划分（有效更新）
     */
    @Test
    public void testRY_NT_04() {
        System.out.println(">>> 开始执行 RY-NT-04: 编辑公告测试...");
        SysNotice notice = new SysNotice();
        notice.setNoticeId(1L);
        notice.setNoticeTitle("修改后的公告标题");

        when(noticeMapper.updateNotice(notice)).thenReturn(1);

        int rows = noticeService.updateNotice(notice);
        assertEquals(1, rows);
        System.out.println("结果：公告内容更新成功。");
    }

    /**
     * RY-NT-04-P: 修改公告 - 参数化测试
     * 设计方法：等价类划分 + 参数化（多个公告ID更新）
     */
    @ParameterizedTest(name = "RY-NT-04-P 修改公告 [{index}] id={0}, title={1}")
    @CsvSource({
            "1,更新后的公告标题A",
            "2,更新后的公告标题B",
            "3,更新后的公告标题C"
    })
    public void testRY_NT_04_P(Long noticeId, String noticeTitle) {
        System.out.println(">>> 开始执行 RY-NT-04-P: 修改公告参数化测试 id=" + noticeId + ", title=" + noticeTitle);
        SysNotice notice = new SysNotice();
        notice.setNoticeId(noticeId);
        notice.setNoticeTitle(noticeTitle);

        when(noticeMapper.updateNotice(notice)).thenReturn(1);

        int rows = noticeService.updateNotice(notice);
        assertEquals(1, rows);
        System.out.println("结果：公告 ID=" + noticeId + " 标题更新为 " + noticeTitle + " 成功。");
    }

    /**
     * RY-NT-05: 批量删除公告
     * 设计方法：边界值分析（删除多个ID）
     */
    @Test
    public void testRY_NT_05() {
        System.out.println(">>> 开始执行 RY-NT-05: 批量删除公告测试...");
        Long[] ids = {1L, 2L, 3L};
        when(noticeMapper.deleteNoticeByIds(ids)).thenReturn(3);

        int rows = noticeService.deleteNoticeByIds(ids);
        assertEquals(3, rows);
        verify(noticeMapper, times(1)).deleteNoticeByIds(ids);
        System.out.println("结果：成功执行批量删除，数据库受影响记录数符合预期。");
    }

    /**
     * RY-NT-06: 批量删除：参数列表为空
     * 设计方法：错误推测法
     */
    @Test
    public void testRY_NT_06() {
        System.out.println(">>> 开始执行 RY-NT-01: 批量删除（空列表输入）健壮性测试...");
        Long[] noticeIds = {};

        // 执行逻辑
        int rows = noticeService.deleteNoticeByIds(noticeIds);

        // 验证：Mapper被调用，但传入空数组，返回 0
        assertEquals(0, rows);
        verify(noticeMapper, times(1)).deleteNoticeByIds(any());
        System.out.println("结果：系统成功传入空数组到 Mapper，返回行数为 0。");
    }

    /**
     * RY-NT-07: 查询：不存在的 ID
     * 设计方法：错误推测法
     */
    @Test
    public void testRY_NT_07() {
        System.out.println(">>> 开始执行 RY-NT-02: 越界 ID 查询异常处理测试...");
        Long noticeId = -1L;

        when(noticeMapper.selectNoticeById(noticeId)).thenReturn(null);

        SysNotice result = noticeService.selectNoticeById(noticeId);
        assertNull(result);
        System.out.println("结果：成功验证对不存在 ID 的查询返回 null，防止了后续可能的空指针风险。");
    }

    /**
     * RY-NT-08: 修改：数据库记录已不存在
     * 设计方法：错误推测法
     */
    @Test
    public void testRY_NT_08() {
        System.out.println(">>> 开始执行 RY-NT-03: 并发场景下修改不存在记录测试...");
        SysNotice notice = new SysNotice();
        notice.setNoticeId(9999L); // 一个已被其他人删除的 ID

        when(noticeMapper.updateNotice(notice)).thenReturn(0);

        int rows = noticeService.updateNotice(notice);
        assertEquals(0, rows);
        System.out.println("结果：系统正确反馈受影响行数为 0，说明业务逻辑能感知到记录已不存在。");
    }
}
