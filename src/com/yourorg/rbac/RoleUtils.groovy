package com.yourorg.rbac

import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import com.michelin.cio.hudson.plugins.rolestrategy.AuthorizationType
import com.michelin.cio.hudson.plugins.rolestrategy.PermissionEntry
import jenkins.model.Jenkins
import hudson.security.Permission

class RoleUtils implements Serializable {

    static void createRoleFromTemplate(String newRoleName, String pattern, String templateRoleName = "Read") {
        Jenkins jenkins = Jenkins.get()
        def rbas = jenkins.getAuthorizationStrategy()

        if (!(rbas instanceof RoleBasedAuthorizationStrategy)) {
            throw new IllegalStateException("Role Strategy plugin is not active")
        }

        // Use Project/Item roles
        def itemRoleMap = rbas.getRoleMap(RoleType.Project)

        // Ensure template role exists (create if missing)
        def templateRole = itemRoleMap.getRole(templateRoleName)
        if (templateRole == null) {
            println "[RBAC] Template role '${templateRoleName}' not found, creating it..."
            def readPermissions = [
                Permission.fromId("hudson.model.Item.Read"),
                Permission.fromId("hudson.model.View.Read")
            ] as Set
            templateRole = new Role(templateRoleName, ".*", readPermissions)
            itemRoleMap.addRole(templateRole)
            println "[RBAC] Template role '${templateRoleName}' created."
        }

        // Remove existing role if already exists
        def existing = itemRoleMap.getRole(newRoleName)
        if (existing != null) {
            itemRoleMap.removeRole(existing)
            println "[RBAC] Existing role '${newRoleName}' removed."
        }

        // Create new role from template
        def newRole = new Role(newRoleName, pattern, templateRole.getPermissions())
        itemRoleMap.addRole(newRole)
        println "[RBAC] Role '${newRoleName}' created successfully from template '${templateRoleName}'."
    }

    // New method to assign a role to a user or group explicitly
    static void assignRoleToUser(String roleName, String username, boolean isGroup = false) {
        Jenkins jenkins = Jenkins.get()
        def rbas = jenkins.getAuthorizationStrategy()

        if (!(rbas instanceof RoleBasedAuthorizationStrategy)) {
            throw new IllegalStateException("Role Strategy plugin is not active")
        }

        def itemRoleMap = rbas.getRoleMap(RoleType.Project)
        def role = itemRoleMap.getRole(roleName)
        if (role == null) {
            throw new IllegalArgumentException("Role '${roleName}' does not exist")
        }

        def authType = isGroup ? AuthorizationType.GROUP : AuthorizationType.USER
        itemRoleMap.assignRole(role, new PermissionEntry(authType, username))
        jenkins.save()
        println "[RBAC] Role '${roleName}' assigned to ${isGroup ? 'group' : 'user'} '${username}'"
    }
}
