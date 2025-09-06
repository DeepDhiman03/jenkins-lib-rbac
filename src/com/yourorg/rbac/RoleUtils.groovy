package com.yourorg.rbac
import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType

def jenkins = Jenkins.instance
def strategy = jenkins.getAuthorizationStrategy()

if (!(strategy instanceof RoleBasedAuthorizationStrategy)) {
    throw new IllegalStateException("Role Strategy plugin is not active")
}

// Use ITEM Role map instead of GLOBAL
def itemRoleMap = strategy.getRoleMap(RoleType.Item)

// Template role name (must exist in Item roles)
def templateRoleName = "Read"
def templateRole = itemRoleMap.getRole(templateRoleName)
if (templateRole == null) {
    throw new IllegalArgumentException("Template role '${templateRoleName}' not found in Item roles")
}

// New role info
def newRoleName = "my-new-role"
def pattern = ".*"  // regex pattern for jobs/projects

// Remove if already exists
def existing = itemRoleMap.getRole(newRoleName)
if (existing != null) {
    itemRoleMap.removeRole(existing)
}

// Create new role (copy permissions from template)
def newRole = new Role(newRoleName, pattern, templateRole.getPermissions())
itemRoleMap.addRole(newRole)

jenkins.save()
println "Item role '${newRoleName}' created successfully"
