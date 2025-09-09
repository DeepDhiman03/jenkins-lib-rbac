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

        def itemRoleMap = rbas.getRoleMap(RoleType.Project)

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

        def existing = itemRoleMap.getRole(newRoleName)
        if (existing != null) {
            itemRoleMap.removeRole(existing)
            println "[RBAC] Existing role '${newRoleName}' removed."
        }

        def newRole = new Role(newRoleName, pattern, templateRole.getPermissions())
        itemRoleMap.addRole(newRole)
        println "[RBAC] Role '${newRoleName}' created successfully from template '${templateRoleName}'."
    }

    static void assignRoleToGroup(String roleName, String groupName) {
        Jenkins jenkins = Jenkins.get()
        def rbas = jenkins.getAuthorizationStrategy()

        if (!(rbas instanceof RoleBasedAuthorizationStrategy)) {
            throw new IllegalStateException("Role Strategy plugin is not active")
        }

        // Assign item role
        def itemRoleMap = rbas.getRoleMap(RoleType.Project)
        def role = itemRoleMap.getRole(roleName)
        if (role == null) {
            throw new IllegalArgumentException("Role '${roleName}' does not exist")
        }
        itemRoleMap.assignRole(role, new PermissionEntry(AuthorizationType.GROUP, groupName))
        println "[RBAC] Role '${roleName}' assigned to group '${groupName}'"

        // Ensure global role "read_access"
        def globalRoleMap = rbas.getRoleMap(RoleType.Global)
        def globalRole = globalRoleMap.getRole("read_access")
        if (globalRole == null) {
            println "[RBAC] Global role 'read_access' not found, creating it..."
            def readPermissions = [
                Permission.fromId("hudson.model.Item.Read"),
                Permission.fromId("hudson.model.View.Read")
            ] as Set
            globalRole = new Role("read_access", ".*", readPermissions)
            globalRoleMap.addRole(globalRole)
            println "[RBAC] Global role 'read_access' created."
        }
        globalRoleMap.assignRole(globalRole, new PermissionEntry(AuthorizationType.GROUP, groupName))
        jenkins.save()
        println "[RBAC] Global role 'read_access' assigned to group '${groupName}'"
    }
}
