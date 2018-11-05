package com.tomcat360.admin.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tomcat360.admin.model.TbAdminRole;
import com.tomcat360.admin.service.AdminMenuPermissionService;
import com.tomcat360.admin.service.AdminRoleService;

/**
 * 需要刷新权限，则调用init方法即可，由于admin就部署一台，所以数据存储到内存中。
 * @author Administrator
 *
 */
@Component
public class PermissionCache{
	
	@Autowired
	private AdminMenuPermissionService adminMenuPermissionService;
	
	@Autowired
	private AdminRoleService adminRoleService;
	
	public static Map<String,List<String>> permission = new HashMap<String,List<String>>();
	
	@PostConstruct
	public void init(){
		List<TbAdminRole> roleList = adminRoleService.findAll();
		for(TbAdminRole role : roleList){
			List<String> permissionList = adminMenuPermissionService.getPermissionListByRoleId(role.getId());
			permission.put(role.getId()+"", permissionList);
		}
	}
	
	public static boolean checkPermission(Long roleId, String perssmission){
		List<String> permissionList = permission.get(roleId+"");
		if(permissionList.contains(perssmission)){
			return true;
		}
		return false;
	}
	
	
	
}
