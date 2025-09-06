/**
 * Create a Role Strategy role from a template role by copying its permissions, setting a pattern,
 * and (optionally) assigning it to users and/or groups.
 *
 * Usage:
 *   createRoleFromTemplate(
 *     roleName: 'dev-read',
 *     pattern: 'my-team-.*',
 *     template: 'Read',                // existing role to copy
 *     roleType: 'Global',              // 'Global' | 'Project' | 'Agent' (aka 'Slave' in older plugin)
 *     assignUsers: ['alice','bob'],    // optional
 *     assignGroups: ['devs','qa'],     // optional
 *     updateIfExists: false            // optional: if true, replace existing role
 *   )
 */

def call(Map cfg = [:]) {
  // ---- Validate required args
  String roleName = required(cfg, 'roleName')
  String pattern  = required(cfg, 'pattern')
  String template = required(cfg, 'template')

  // ---- Optional args
  String roleTypeName   = (cfg.roleType ?: 'Global') as String
  List<String> users    = (cfg.assignUsers ?: []) as List<String>
  List<String> groups   = (cfg.assignGroups ?: []) as List<String>
  boolean updateIfExists = (cfg.updateIfExists ?: false) as boolean

  echo "[RBAC] roleName=${roleName}, pattern=${pattern}, template=${template}, roleType=${roleTypeName}"
  if (users)  echo "[RBAC] assignUsers=${users}"
  if (groups) echo "[RBAC] assignGroups=${groups}"
  if (updateIfExists) echo "[RBAC] updateIfExists=true"

  // ---- Resolve Jenkins + Role Strategy classes dynamically (handles different plugin package names)
  def jenkins = jenkins.model.Jenkins.get()
  def strategy = jenkins.getAuthorizationStrategy()

  Class RBAS = loadClass('com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy')
  if (!(strategy instanceof RBAS)) {
    error "[RBAC] Role Strategy plugin is not active (Authorization must be 'Role-Based Strategy')."
  }

  Class RoleTypeCls = tryClasses([
    'com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType',   // newer
    'com.michelin.cio.hudson.plugins.rolestrategy.RoleType'     // older
  ])

  Class RoleCls = loadClass('com.michelin.cio.hudson.plugins.rolestrategy.Role')

  // ---- Map roleType input → enum (compat for Agent/Slave)
  String normalizedType = normalizeRoleType(roleTypeName, RoleTypeCls)
  def roleTypeEnum = Enum.valueOf(RoleTypeCls, normalizedType)

  // ---- Get the role map for the chosen type
  def roleMap = strategy.getRoleMap(roleTypeEnum)

  // ---- Find template role in the same role map
  def templateRole = roleMap.getRole(template)
  if (templateRole == null) {
    error "[RBAC] Template role '${template}' not found in ${normalizedType} role map."
  }

  // ---- If role exists
  def existing = roleMap.getRole(roleName)
  if (existing != null) {
    if (!updateIfExists) {
      error "[RBAC] Role '${roleName}' already exists in ${normalizedType} map. Set updateIfExists: true to replace."
    }
    echo "[RBAC] Role '${roleName}' exists; replacing (updateIfExists=true)."
    tryRemoveRole(roleMap, existing)
  }

  // ---- Create new role (copy permissions from template, apply pattern)
  def newRole = RoleCls.getConstructor(String, String, Set).newInstance(
    roleName,
    pattern,
    templateRole.getPermissions()
  )
  roleMap.addRole(newRole)
  echo "[RBAC] Added role '${roleName}' to ${normalizedType}."

  // ---- Assign to users/groups if provided
  (users + groups).each { sid ->
    roleMap.assignRole(newRole, sid as String)
    echo "[RBAC] Assigned role '${roleName}' to '${sid}'."
  }

  // ---- Persist
  jenkins.save()
  echo "[RBAC] Done. Saved Jenkins configuration."
}

/* --------------------- helpers --------------------- */

def required(Map cfg, String key) {
  def v = cfg[key]
  if (v == null || (v instanceof CharSequence && v.toString().trim().isEmpty())) {
    error "[RBAC] Missing required argument: ${key}"
  }
  return v.toString()
}

Class loadClass(String name) {
  try {
    return Class.forName(name)
  } catch (ClassNotFoundException e) {
    error "[RBAC] Required class not found: ${name}. Is the Role Strategy plugin installed and up to date?"
  }
}

Class tryClasses(List<String> names) {
  for (String n : names) {
    try { return Class.forName(n) } catch (Throwable ignore) {}
  }
  error "[RBAC] None of the expected classes found: ${names}"
}

String normalizeRoleType(String input, Class roleTypeEnumCls) {
  String s = (input ?: 'Global').trim().toUpperCase()
  // Accept a few synonyms
  if (s == 'SLAVE') s = 'AGENT'  // new plugin uses AGENT
  if (s == 'NODES') s = 'AGENT'

  // Check available constants in this plugin version
  def names = roleTypeEnumCls.getEnumConstants()*.name()
  if (names.contains(s)) return s

  // Fallbacks: prefer GLOBAL if unknown
  if (names.contains('GLOBAL')) return 'GLOBAL'
  // Older versions might be 'PROJECT' and 'SLAVE'
  if (s == 'AGENT' && names.contains('SLAVE')) return 'SLAVE'

  // Last chance error with available names
  error "[RBAC] Unsupported roleType '${input}'. Available: ${names}"
}

void tryRemoveRole(def roleMap, def roleObj) {
  // RoleMap has removeRole(Role) in most versions; try name-based removal if present
  try {
    roleMap.removeRole(roleObj)
  } catch (Throwable t1) {
    try {
      roleMap.removeRole(roleObj.getName())
    } catch (Throwable t2) {
      error "[RBAC] Could not remove existing role '${roleObj?.getName()}': ${t2.message}"
    }
  }
}
