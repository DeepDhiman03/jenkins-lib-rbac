// vars/createRoleFromTemplate.groovy
def call(String roleName, String pattern, String templateRoleName) {
    com.yourorg.rbac.RoleUtils.createRoleFromTemplate(roleName, pattern, templateRoleName)
}
