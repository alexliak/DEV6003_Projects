package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Role;
import com.nyc.hosp.model.HospuserDTO;
import com.nyc.hosp.repos.RoleRepository;
import com.nyc.hosp.service.HospuserService;
import com.nyc.hosp.util.CustomCollectors;
import com.nyc.hosp.util.ReferencedWarning;
import com.nyc.hosp.util.WebUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@Controller
@RequestMapping("/hospusers")
public class HospuserController {

    private final HospuserService hospuserService;
    private final RoleRepository roleRepository;

    public HospuserController(final HospuserService hospuserService,
            final RoleRepository roleRepository) {
        this.hospuserService = hospuserService;
        this.roleRepository = roleRepository;
    }

    @ModelAttribute
    public void prepareContext(final Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSecretary = auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SECRETARIAT"));
        
        if (isSecretary) {
            // Secretary can only assign PATIENT or DOCTOR roles
            model.addAttribute("roleValues", roleRepository.findAll(Sort.by("id"))
                    .stream()
                    .filter(role -> role.getName() == Role.RoleName.ROLE_PATIENT || 
                                   role.getName() == Role.RoleName.ROLE_DOCTOR)
                    .collect(CustomCollectors.toSortedMap(Role::getId, role -> role.getName().name())));
        } else {
            // Admins can assign any role
            model.addAttribute("roleValues", roleRepository.findAll(Sort.by("id"))
                    .stream()
                    .collect(CustomCollectors.toSortedMap(Role::getId, role -> role.getName().name())));
        }
    }

    @GetMapping
    public String list(final Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is secretary
        boolean isSecretary = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SECRETARIAT"));
        
        if (isSecretary) {
            // Secretary can only see patients and doctors, not admins or other secretaries
            model.addAttribute("hospusers", hospuserService.findPatientsAndDoctors());
        } else {
            // Admins can see all users
            model.addAttribute("hospusers", hospuserService.findAll());
        }
        
        return "hospuser/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("hospuser") final HospuserDTO hospuserDTO) {
        return "hospuser/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("hospuser") @Valid final HospuserDTO hospuserDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "hospuser/add";
        }
        // Check 2 passwords
        hospuserDTO.setLastlogondatetime(OffsetDateTime.now());
        hospuserDTO.setLocked(false);
        hospuserService.create(hospuserDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("hospuser.create.success"));
        return "redirect:/hospusers";
    }

    @GetMapping("/edit/{userId}")
    public String edit(@PathVariable(name = "userId") final Long userId, final Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSecretary = auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SECRETARIAT"));
        
        HospuserDTO user = hospuserService.get(userId);
        
        if (isSecretary) {
            // Check if user is admin or secretary - secretary cannot edit them
            if (user.getRole() != null && (user.getRole() == 1 || user.getRole() == 4)) {
                // Role 1 = ADMIN, Role 4 = SECRETARY
                throw new RuntimeException("Access denied: Secretaries cannot edit Admin or Secretary users");
            }
        }
        
        model.addAttribute("hospuser", user);
        return "hospuser/edit";
    }

    @PostMapping("/edit/{userId}")
    public String edit(@PathVariable(name = "userId") final Long userId,
            @ModelAttribute("hospuser") @Valid final HospuserDTO hospuserDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "hospuser/edit";
        }
        hospuserService.update(userId, hospuserDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("hospuser.update.success"));
        return "redirect:/hospusers";
    }

    @PostMapping("/delete/{userId}")
    public String delete(@PathVariable(name = "userId") final Long userId,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = hospuserService.getReferencedWarning(userId);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            hospuserService.delete(userId);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("hospuser.delete.success"));
        }
        return "redirect:/hospusers";
    }

}
