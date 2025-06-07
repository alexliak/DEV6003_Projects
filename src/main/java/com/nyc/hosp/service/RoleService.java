package com.nyc.hosp.service;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Role;
import com.nyc.hosp.model.RoleDTO;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.RoleRepository;
import com.nyc.hosp.util.NotFoundException;
import com.nyc.hosp.util.ReferencedWarning;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final HospuserRepository hospuserRepository;

    public RoleService(final RoleRepository roleRepository,
            final HospuserRepository hospuserRepository) {
        this.roleRepository = roleRepository;
        this.hospuserRepository = hospuserRepository;
    }

    public List<RoleDTO> findAll() {
        final List<Role> roles = roleRepository.findAll(Sort.by("id"));
        return roles.stream()
                .map(role -> mapToDTO(role, new RoleDTO()))
                .toList();
    }

    public RoleDTO get(final Long roleId) {
        return roleRepository.findById(roleId)
                .map(role -> mapToDTO(role, new RoleDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final RoleDTO roleDTO) {
        final Role role = new Role();
        mapToEntity(roleDTO, role);
        return roleRepository.save(role).getId();
    }

    public void update(final Long roleId, final RoleDTO roleDTO) {
        final Role role = roleRepository.findById(roleId)
                .orElseThrow(NotFoundException::new);
        mapToEntity(roleDTO, role);
        roleRepository.save(role);
    }

    public void delete(final Long roleId) {
        roleRepository.deleteById(roleId);
    }

    private RoleDTO mapToDTO(final Role role, final RoleDTO roleDTO) {
        roleDTO.setRoleId(role.getId().intValue());
        roleDTO.setRolename(role.getName() != null ? role.getName().name() : null);
        return roleDTO;
    }

    private Role mapToEntity(final RoleDTO roleDTO, final Role role) {
        if (roleDTO.getRolename() != null) {
            try {
                role.setName(Role.RoleName.valueOf(roleDTO.getRolename()));
            } catch (IllegalArgumentException e) {
                // Handle invalid role name
                throw new IllegalArgumentException("Invalid role name: " + roleDTO.getRolename());
            }
        }
        return role;
    }

    public ReferencedWarning getReferencedWarning(final Long roleId) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final Role role = roleRepository.findById(roleId)
                .orElseThrow(NotFoundException::new);
        
        // Check if any users have this role
        List<Hospuser> usersWithRole = hospuserRepository.findByRoleName(role.getName());
        if (!usersWithRole.isEmpty()) {
            referencedWarning.setKey("role.hospuser.role.referenced");
            referencedWarning.addParam(usersWithRole.get(0).getId());
            return referencedWarning;
        }
        return null;
    }
}
