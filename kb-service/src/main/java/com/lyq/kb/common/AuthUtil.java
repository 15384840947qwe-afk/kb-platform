package com.lyq.kb.common;

/**
 * 权限校验工具：全部从UserContext取当前用户判断，
 * 不满足就抛异常，交给全局异常处理器翻译
 */
public class AuthUtil {

    /** 写操作门槛：VIEWER只读，拦掉 */
    public static void requireWritable() {
        if (Role.VIEWER.name().equals(UserContext.get().getRole())) {
            throw new ForbiddenException("只读访客不能执行写操作");
        }
    }

    /** 管理员专属 */
    public static void requireAdmin() {
        if (!Role.ADMIN.name().equals(UserContext.get().getRole())) {
            throw new ForbiddenException("需要管理员权限");
        }
    }

    /** 管理员或资源本人：删知识库这种"我的东西我做主+管理员兜底"的场景 */
    public static void requireAdminOrOwner(Long ownerId) {
        UserContext.CurrentUser me = UserContext.get();
        if (!Role.ADMIN.name().equals(me.getRole()) && !me.getId().equals(ownerId)) {
            throw new ForbiddenException("无权限：仅创建者或管理员可操作");
        }
    }
}