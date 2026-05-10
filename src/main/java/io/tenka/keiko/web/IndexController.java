package io.tenka.keiko.web;

import io.tenka.keiko.service.PickerService;
import io.tenka.keiko.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the SPA shell at "/". The Thymeleaf template resolves
 * {@code subject_*} and {@code project_version} from this model and
 * renders the masthead + initial card-pool count.
 */
@Controller
public class IndexController {

    private final Subject subject;
    private final PickerService picker;

    public IndexController(Subject subject, PickerService picker) {
        this.subject = subject;
        this.picker = picker;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("subject", subject);
        // TODO(follow-up PR): wire from build.gradle.kts version via
        // application.properties so we don't hand-edit two places per bump.
        model.addAttribute("projectVersion", "v0.7");
        model.addAttribute("defaultDeckCount", picker.poolSize("default"));
        model.addAttribute("experimentalDeckCount", picker.poolSize("experimental"));
        return "index";
    }
}
