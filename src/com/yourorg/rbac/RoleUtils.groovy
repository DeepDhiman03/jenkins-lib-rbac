package com.yourorg.rbac

import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy

class RoleUtils {

    /**
     * Add a role by copying permissions from a template role.
     * Only handles: roleName, pattern, template.
     */
    static void createRoleFromTemplate(String roleName, String pattern, String templateRoleName) {

        def jenkins = Jenkins.get()
        def strategy = jenkins.getAuthorizationStrategy()

        if (!(strategy instanceof RoleBasedAuthorizationStrategy)) {
            throw new IllegalStateException("[RBAC] Role Strategy plugin is not active.")
        }

        // Choose Global role map (for simplicity)
        def roleMap = strategy.getRoleMap(RoleBasedAuthorizationStrategy.GLOBAL)

        // Get template role
        def templateRole = roleMap.getRole(templateRoleName)
        if (templateRole == null) {
            throw new IllegalArgumentException("[RBAC] Template role '${templateRoleName}' not found.")
        }

        // Remove existing role if present
        def existing = roleMap.getRole(roleName)
        if (existing != null) {
            roleMap.removeRole(existing)
        }

        // Create new role (copy permissions from template)
        def newRole = new Role(roleName, pattern, templateRole.getPermissions())
        roleMap.addRole(newRole)

        // Persist Jenkins config
        jenkins.save()
    }
}
