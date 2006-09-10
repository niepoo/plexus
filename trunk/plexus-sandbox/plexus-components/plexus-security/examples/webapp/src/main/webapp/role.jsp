<%@ taglib uri="/webwork" prefix="ww" %>

<html>
<head>
  <title>Plexus Example Webapp Role</title>
</head>

<body>
<p>
  Role Modification Page

</p>
<ww:actionerror/>
<ww:form action="saveRole" method="post">
  <ww:hidden name="roleId"/>

  <ww:textfield label="name" name="name"/> <br/>
  <ww:textfield label="description" name="description"/> <br/>
  <ww:checkbox label="assignable?" name="assignable"/><br/>
  <br/>
  Currently Assigned Permissions:<br/>
  <ww:iterator id="permission" value="permissions">
    <ww:url id="removeAssignedPermissionUrl" action="removeAssignedPermission">
      <ww:param name="roleId" value="roleId"/>
      <ww:param name="removePermissionId" value="${permission.id}"/>
    </ww:url>
    ${permission.name} | <ww:a href="%{removeAssignedPermissionUrl}">remove</ww:a><br/>
  </ww:iterator>
  <br/>
  <ww:select label="add new permission" name="assignedPermissionId" list="assignablePermissions"  listKey="id" listValue="name" emptyOption="true"/><br/>
  <br/>
  Currently Assigned Roles:<br/>
  <ww:iterator id="arole" value="childRoles.roles">
    <ww:url id="removeAssignedRoleUrl" action="removeAssignedRole">
      <ww:param name="roleId" value="roleId"/>
      <ww:param name="removeRoleId" value="${arole.id}"/>
    </ww:url>
    ${arole.name} | <ww:a href="%{removeAssignedRoleUrl}">remove</ww:a><br/>
  </ww:iterator>
  <br/>
  <ww:select label="add sub role" name="assignedRoleId" list="assignableRoles" listKey="id" listValue="name" emptyOption="true"/><br/>

  <p>
    <ww:submit/>
  </p>
</ww:form>

</body>
</html>