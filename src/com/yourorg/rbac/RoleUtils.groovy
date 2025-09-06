import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import jenkins.model.Jenkins

def jenkins = Jenkins.get()
def rbas = jenkins.getAuthorizationStrategy()

if (!(rbas instanceof RoleBasedAuthorizationStrategy)) {
    throw new IllegalStateException("Role Strategy plugin is not active")
}

// Use PROJECT role map (Item roles) instead of GLOBAL
def itemRoleMap = rbas.getRoleMap(RoleType.Project)  // <-- changed from Item to Project

// Template role
def templateRoleName = "Read"
def templateRole = itemRoleMap.getRole(templateRoleName)
if (templateRole == null) {
    throw new IllegalArgumentException("Template role '${templateRoleName}' not found in Item roles")
}

// New role info
def newRoleName = "my-new-role"
def pattern = ".*"

// Remove existing if already exists
def existing = itemRoleMap.getRole(newRoleName)
if (existing != null) {
    itemRoleMap.removeRole(existing)
}

// Create new role from template
def newRole = new Role(newRoleName, pattern, templateRole.getPermissions())
itemRoleMap.addRole(newRole)

jenkins.save()
println "Item role '${newRoleName}' created successfully"
