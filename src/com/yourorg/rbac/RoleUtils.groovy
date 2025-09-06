package com.yourorg.rbac

import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType

class RoleUtils {

    static void createRoleFromTemplate(String roleName, String pattern, String templateRoleName) {
        def jenkins = Jenkins.instance
        def strategy = jenkins.getAuthorizationStrategy()

        if (!(strategy instanceof RoleBasedAuthorizationStrategy)) {
            throw new IllegalStateException("Role Strategy plugin is not active")
        }

        def itemRoleMap = strategy.getRoleMap(RoleType.Item)

        def templateRole = itemRoleMap.getRole(templateRoleName)
        if (templateRole == null) {
            throw new IllegalArgumentException("Template role '${templateRoleName}' not found in Item roles")
        }

        def existing = itemRoleMap.getRole(roleName)
        if (existing != null) {
            itemRoleMap.removeRole(existing)
        }

        def newRole = new Role(roleName, pattern, templateRole.getPermissions())
        itemRoleMap.addRole(newRole)

        jenkins.save()
        println "[RBAC] Item role '${roleName}' created successfully"
    }
}
