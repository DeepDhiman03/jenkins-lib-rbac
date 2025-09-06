def call(String roleName, String pattern, String templateRoleName = "Read") {
    com.yourorg.rbac.RoleUtils.createRoleFromTemplate(roleName, pattern, templateRoleName)
}
