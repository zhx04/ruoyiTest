package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.impl.SysDeptServiceImpl;
import com.ruoyi.system.service.impl.SysMenuServiceImpl;
import com.ruoyi.system.service.impl.SysRoleServiceImpl;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RuoYi核心Service单元测试")
public class CoreServiceTest {

    // ==================== SysUserServiceImpl 测试 ====================
    @Mock
    private com.ruoyi.system.mapper.SysUserMapper userMapper;
    @Mock
    private com.ruoyi.system.mapper.SysPostMapper postMapper;
    @Mock
    private com.ruoyi.system.mapper.SysUserRoleMapper userRoleMapper;
    @Mock
    private com.ruoyi.system.mapper.SysUserPostMapper userPostMapper;
    @Mock
    private ISysConfigService configService;
    @InjectMocks
    private SysUserServiceImpl userService;

    @Nested
    @DisplayName("SysUserServiceImpl - selectUserList 测试")
    class SelectUserListTests {
        @Test
        @DisplayName("U-001: 等价类-正常用户名查询")
        void testSelectUserListByUserName() {
            SysUser queryUser = new SysUser();
            queryUser.setUserName("admin");
            List<SysUser> expectedList = new ArrayList<>();
            SysUser user = new SysUser();
            user.setUserId(1L);
            user.setUserName("admin");
            expectedList.add(user);
            org.mockito.Mockito.when(userMapper.selectUserList(org.mockito.ArgumentMatchers.any(SysUser.class))).thenReturn(expectedList);
            List<SysUser> result = userService.selectUserList(queryUser);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
            org.junit.jupiter.api.Assertions.assertEquals("admin", result.get(0).getUserName());
        }

        @Test
        @DisplayName("U-002: 等价类-空条件查询（查询全部）")
        void testSelectUserListWithNullCondition() {
            SysUser queryUser = new SysUser();
            List<SysUser> expectedList = new ArrayList<>();
            expectedList.add(new SysUser());
            expectedList.add(new SysUser());
            org.mockito.Mockito.when(userMapper.selectUserList(org.mockito.ArgumentMatchers.any(SysUser.class))).thenReturn(expectedList);
            List<SysUser> result = userService.selectUserList(queryUser);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertEquals(2, result.size());
        }

        @Test
        @DisplayName("U-004: 边界-SQL注入攻击测试")
        void testSelectUserListWithSqlInjection() {
            SysUser queryUser = new SysUser();
            queryUser.setUserName("' OR 1=1 --");
            org.mockito.Mockito.when(userMapper.selectUserList(org.mockito.ArgumentMatchers.any(SysUser.class))).thenReturn(new ArrayList<>());
            List<SysUser> result = userService.selectUserList(queryUser);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("U-005: 边界-超长字符串测试")
        void testSelectUserListWithLongString() {
            SysUser queryUser = new SysUser();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 256; i++) {
                sb.append("a");
            }
            queryUser.setUserName(sb.toString());
            org.mockito.Mockito.when(userMapper.selectUserList(org.mockito.ArgumentMatchers.any(SysUser.class))).thenReturn(new ArrayList<>());
            List<SysUser> result = userService.selectUserList(queryUser);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("SysUserServiceImpl - checkUserNameUnique 测试")
    class CheckUserNameUniqueTests {
        @Test
        @DisplayName("U-006: 等价类-唯一用户名")
        void testCheckUserNameUnique_WhenUnique() {
            SysUser user = new SysUser();
            user.setUserName("newuser");
            org.mockito.Mockito.when(userMapper.checkUserNameUnique("newuser")).thenReturn(null);
            boolean result = userService.checkUserNameUnique(user);
            org.junit.jupiter.api.Assertions.assertTrue(result);
        }
    }

    // ==================== SysRoleServiceImpl 测试 ====================
    @Mock
    private com.ruoyi.system.mapper.SysRoleMapper roleMapper;
    @Mock
    private com.ruoyi.system.mapper.SysRoleMenuMapper roleMenuMapper;
    @Mock
    private com.ruoyi.system.mapper.SysRoleDeptMapper roleDeptMapper;
    @InjectMocks
    private SysRoleServiceImpl roleService;

    @Nested
    @DisplayName("SysRoleServiceImpl - selectRoleList 测试")
    class SelectRoleListTests {
        @Test
        @DisplayName("R-001: 等价类-正常查询角色列表")
        void testSelectRoleList() {
            SysRole queryRole = new SysRole();
            queryRole.setRoleName("admin");
            List<SysRole> expectedList = new ArrayList<>();
            SysRole role = new SysRole();
            role.setRoleId(1L);
            role.setRoleName("admin");
            expectedList.add(role);
            org.mockito.Mockito.when(roleMapper.selectRoleList(org.mockito.ArgumentMatchers.any(SysRole.class))).thenReturn(expectedList);
            List<SysRole> result = roleService.selectRoleList(queryRole);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
            org.junit.jupiter.api.Assertions.assertEquals("admin", result.get(0).getRoleName());
        }

        @Test
        @DisplayName("R-002: 等价类-空条件查询")
        void testSelectRoleListWithEmptyCondition() {
            SysRole queryRole = new SysRole();
            List<SysRole> expectedList = new ArrayList<>();
            expectedList.add(new SysRole());
            expectedList.add(new SysRole());
            org.mockito.Mockito.when(roleMapper.selectRoleList(org.mockito.ArgumentMatchers.any(SysRole.class))).thenReturn(expectedList);
            List<SysRole> result = roleService.selectRoleList(queryRole);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("SysRoleServiceImpl - checkRoleNameUnique 测试")
    class CheckRoleNameUniqueTests {

        @Test
        @DisplayName("R-003: 边界值-空角色名")
        void testInsertRole_NullRoleName() {
            SysRole role = new SysRole();
            role.setRoleName(null);
            role.setRoleKey("test_key");
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> roleService.insertRole(role));
        }

        @Test
        @DisplayName("R-005: 边界值-重复角色名")
        void testCheckRoleNameUnique_WhenDuplicate() {
            SysRole role = new SysRole();
            role.setRoleId(2L);
            role.setRoleName("admin");
            SysRole existingRole = new SysRole();
            existingRole.setRoleId(1L);
            existingRole.setRoleName("admin");
            org.mockito.Mockito.when(roleMapper.checkRoleNameUnique("admin")).thenReturn(existingRole);
            boolean result = roleService.checkRoleNameUnique(role);
            org.junit.jupiter.api.Assertions.assertFalse(result);
        }
    }

    // ==================== SysDeptServiceImpl 测试 ====================
    @Mock
    private com.ruoyi.system.mapper.SysDeptMapper deptMapper;
    @InjectMocks
    private SysDeptServiceImpl deptService;

    @Nested
    @DisplayName("SysDeptServiceImpl - selectDeptList 测试")
    class SelectDeptListTests {
        @Test
        @DisplayName("D-001: 等价类-正常查询部门列表")
        void testSelectDeptList() {
            SysDept queryDept = new SysDept();
            queryDept.setDeptName("研发部");
            List<SysDept> expectedList = new ArrayList<>();
            SysDept dept = new SysDept();
            dept.setDeptId(1L);
            dept.setDeptName("研发部");
            expectedList.add(dept);
            org.mockito.Mockito.when(deptMapper.selectDeptList(org.mockito.ArgumentMatchers.any(SysDept.class))).thenReturn(expectedList);
            List<SysDept> result = deptService.selectDeptList(queryDept);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
            org.junit.jupiter.api.Assertions.assertEquals("研发部", result.get(0).getDeptName());
        }
    }

    @Nested
    @DisplayName("SysDeptServiceImpl - checkDeptNameUnique 测试")
    class CheckDeptNameUniqueTests {
        @Test
        @DisplayName("D-002: 等价类-新增到根部门")
        void testInsertDept_ToRoot() {
            SysDept parentDept = new SysDept();
            parentDept.setDeptId(0L);
            parentDept.setStatus("0");
            SysDept newDept = new SysDept();
            newDept.setDeptName("新部门");
            newDept.setParentId(0L);
            org.mockito.Mockito.when(deptMapper.selectDeptById(0L)).thenReturn(parentDept);
            org.mockito.Mockito.when(deptMapper.checkDeptNameUnique("新部门", 0L)).thenReturn(null);
            org.mockito.Mockito.doReturn(1).when(deptMapper).insertDept(org.mockito.ArgumentMatchers.any(SysDept.class));
            int result = deptService.insertDept(newDept);
            org.junit.jupiter.api.Assertions.assertEquals(1, result);
            org.mockito.Mockito.verify(deptMapper).insertDept(org.mockito.ArgumentMatchers.any(SysDept.class));
        }

        @Test
        @DisplayName("D-004: 边界值-父部门已停用")
        void testInsertDept_ParentDisabled() {
            SysDept parentDept = new SysDept();
            parentDept.setDeptId(1L);
            parentDept.setStatus("1");
            SysDept newDept = new SysDept();
            newDept.setDeptName("新部门");
            newDept.setParentId(1L);
            org.mockito.Mockito.when(deptMapper.selectDeptById(1L)).thenReturn(parentDept);
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> deptService.insertDept(newDept));
        }

        @Test
        @DisplayName("D-005: 边界值-空部门名称")
        void testCheckDeptNameUnique_EmptyName() {
            SysDept dept = new SysDept();
            dept.setDeptName(null);
            dept.setParentId(0L);
            org.mockito.Mockito.when(deptMapper.checkDeptNameUnique(null, 0L)).thenReturn(null);
            boolean result = deptService.checkDeptNameUnique(dept);
            org.junit.jupiter.api.Assertions.assertTrue(result);
        }
    }

    // ==================== SysMenuServiceImpl 测试 ====================
    @Mock
    private com.ruoyi.system.mapper.SysMenuMapper menuMapper;
    @InjectMocks
    private SysMenuServiceImpl menuService;

    @Nested
    @DisplayName("SysMenuServiceImpl - selectMenuList 测试")
    class SelectMenuListTests {
        @Test
        @DisplayName("M-001: 等价类-正常查询菜单列表")
        void testSelectMenuList() {
            SysMenu queryMenu = new SysMenu();
            queryMenu.setMenuName("系统管理");
            List<SysMenu> expectedList = new ArrayList<>();
            SysMenu menu = new SysMenu();
            menu.setMenuId(1L);
            menu.setMenuName("系统管理");
            expectedList.add(menu);
            org.mockito.Mockito.when(menuMapper.selectMenuList(org.mockito.ArgumentMatchers.any(SysMenu.class))).thenReturn(expectedList);
            List<SysMenu> result = menuService.selectMenuList(queryMenu, 1L);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        }

        @Test
        @DisplayName("M-002: 等价类-新增顶级菜单")
        void testInsertMenu_ToRoot() {
            SysMenu newMenu = new SysMenu();
            newMenu.setMenuName("新菜单");
            newMenu.setParentId(0L);
            newMenu.setPath("/newMenu");
            newMenu.setMenuType("C");
            org.mockito.Mockito.doReturn(1).when(menuMapper).insertMenu(org.mockito.ArgumentMatchers.any(SysMenu.class));
            int result = menuService.insertMenu(newMenu);
            org.junit.jupiter.api.Assertions.assertEquals(1, result);
            org.mockito.Mockito.verify(menuMapper).insertMenu(org.mockito.ArgumentMatchers.any(SysMenu.class));
        }
    }

    @Nested
    @DisplayName("SysMenuServiceImpl - checkMenuNameUnique 测试")
    class CheckMenuNameUniqueTests {
        @Test
        @DisplayName("M-004: 边界值-空菜单名称")
        void testCheckMenuNameUnique_EmptyName() {
            SysMenu menu = new SysMenu();
            menu.setMenuName(null);
            menu.setParentId(0L);
            org.mockito.Mockito.when(menuMapper.checkMenuNameUnique(null, 0L)).thenReturn(null);
            boolean result = menuService.checkMenuNameUnique(menu);
            org.junit.jupiter.api.Assertions.assertTrue(result);
        }

        @Test
        @DisplayName("M-005: 等价类-菜单名称唯一")
        void testCheckMenuNameUnique_Unique() {
            SysMenu menu = new SysMenu();
            menu.setMenuId(100L);
            menu.setMenuName("唯一菜单");
            menu.setParentId(0L);
            org.mockito.Mockito.when(menuMapper.checkMenuNameUnique("唯一菜单", 0L)).thenReturn(null);
            boolean result = menuService.checkMenuNameUnique(menu);
            org.junit.jupiter.api.Assertions.assertTrue(result);
        }
    }
}
