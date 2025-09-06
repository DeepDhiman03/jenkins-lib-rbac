def call(Map cfg) {
    String roleName = cfg.roleName
    String pattern  = cfg.pattern
    String template = cfg.template

    com.yourorg.rbac.RoleUtils.createRoleFromTemplate(roleName, pattern, template)
}
