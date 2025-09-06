package com.yourorg.rbac

import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import com.michelin.cio.hudson.plugins.rolestrategy.PermissionEntry
import com.michelin.cio.hudson.plugins.rolestrategy.AuthorizationType

class RoleUtils {

    /**
     * Create a role by copying permissions from a template role
     * Only handles: roleName, pattern, templateRoleName
     */
    static void createRoleFromTemplate(String roleName, String pattern, String templateRoleName) {

        Jenkins jenkins = Jenkins.get()
        def strategy = jenkins.getAuthorizationStrategy()

        if (!(strategy instanceof RoleBasedAuthorizationStrategy)) {
            throw new IllegalStateException("[RBAC] Role Strategy plugin is not active.")
        }

        // Use Global Role Map
        def globalRoleMap = strategy.getRoleMap(RoleType.Global)

        // Get template role
        def templateRole = globalRoleMap.getRole(templateRoleName)
        if (templateRole == null) {
            throw new IllegalArgumentException("[RBAC] Template role '${templateRoleName}' not found.")
        }

        // Remove existing role if present
        def existing = globalRoleMap.getRole(roleName)
        if (existing != null) {
            globalRoleMap.removeRole(existing)
        }

        // Create new role (copy permissions from template)
        def newRole = new Role(roleName, pattern, templateRole.getPermissions())
        globalRoleMap.addRole(newRole)

        // Optional: assign role to a user (can skip if only creating role)
        // globalRoleMap.assignRole(newRole, new PermissionEntry(AuthorizationType.USER, "username"))

        // Persist Jenkins config
        jenkins.save()
    }
}
