package com.yourorg.rbac

import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.*
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType

class RoleUtils {
    static void createRoleFromTemplate(String newRoleName, String pattern, String templateRoleName) {
        def jenkins = Jenkins.get()
        def rbas = jenkins.getAuthorizationStrategy()

        if (!(rbas instanceof RoleBasedAuthorizationStrategy)) {
            throw new IllegalStateException("Role Strategy plugin is not active")
        }

        def itemRoleMap = rbas.getRoleMap(RoleType.Project)
        def templateRole = itemRoleMap.getRole(templateRoleName)
        if (templateRole == null) {
            throw new IllegalArgumentException("Template role '${templateRoleName}' not found")
        }

        def existing = itemRoleMap.getRole(newRoleName)
        if (existing != null) {
            itemRoleMap.removeRole(existing)
        }

        def newRole = new Role(newRoleName, pattern, templateRole.getPermissions())
        itemRoleMap.addRole(newRole)
        jenkins.save()
    }
}
