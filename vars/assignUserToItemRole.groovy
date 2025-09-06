// vars/assignUserToItemRole.groovy
def call(String roleName, String username) {
    com.yourorg.rbac.RoleUtils.assignUserToItemRole(roleName, username)
}
