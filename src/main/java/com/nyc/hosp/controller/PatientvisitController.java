package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.model.PatientvisitDTO;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.service.PatientvisitService;
import com.nyc.hosp.util.CustomCollectors;
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


@Controller
@RequestMapping("/patientvisits")
public class PatientvisitController {

    private final PatientvisitService patientvisitService;
    private final HospuserRepository hospuserRepository;

    public PatientvisitController(final PatientvisitService patientvisitService,
            final HospuserRepository hospuserRepository) {
        this.patientvisitService = patientvisitService;
        this.hospuserRepository = hospuserRepository;
    }

    @ModelAttribute
    public void prepareContext(final Model model) {
        model.addAttribute("patientValues", hospuserRepository.findByRoleName(com.nyc.hosp.domain.Role.RoleName.ROLE_PATIENT)
                .stream()
                .collect(CustomCollectors.toSortedMap(Hospuser::getId, Hospuser::getUsername)));
        model.addAttribute("doctorValues", hospuserRepository.findByRoleName(com.nyc.hosp.domain.Role.RoleName.ROLE_DOCTOR)
                .stream()
                .collect(CustomCollectors.toSortedMap(Hospuser::getId, Hospuser::getUsername)));
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("patientvisits", patientvisitService.findAll());
        return "patientvisit/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("patientvisit") final PatientvisitDTO patientvisitDTO) {
        return "patientvisit/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("patientvisit") @Valid final PatientvisitDTO patientvisitDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "patientvisit/add";
        }
        patientvisitService.create(patientvisitDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("patientvisit.create.success"));
        return "redirect:/patientvisits";
    }

    @GetMapping("/edit/{visitid}")
    public String edit(@PathVariable(name = "visitid") final Integer visitid, final Model model) {
        model.addAttribute("patientvisit", patientvisitService.get(visitid));
        return "patientvisit/edit";
    }

    @PostMapping("/edit/{visitid}")
    public String edit(@PathVariable(name = "visitid") final Integer visitid,
            @ModelAttribute("patientvisit") @Valid final PatientvisitDTO patientvisitDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "patientvisit/edit";
        }
        patientvisitService.update(visitid, patientvisitDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("patientvisit.update.success"));
        return "redirect:/patientvisits";
    }

    @PostMapping("/delete/{visitid}")
    public String delete(@PathVariable(name = "visitid") final Integer visitid,
            final RedirectAttributes redirectAttributes) {
        patientvisitService.delete(visitid);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("patientvisit.delete.success"));
        return "redirect:/patientvisits";
    }

}
