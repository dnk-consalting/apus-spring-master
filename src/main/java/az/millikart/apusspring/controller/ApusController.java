package az.millikart.apusspring.controller;

import az.millikart.apusspring.dto.Template;
import az.millikart.apusspring.service.CommitService;
import az.millikart.apusspring.service.RedirectService;
import az.millikart.apusspring.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.SOAPException;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@Controller
public class ApusController {


    private final RegistrationService registrationService;
    private final CommitService commitService;

    private final RedirectService redirectService;

    public ApusController(RegistrationService registrationService,
                          CommitService commitService,
                          RedirectService redirectService) {
        this.registrationService = registrationService;
        this.commitService = commitService;
        this.redirectService = redirectService;
    }

    @ResponseBody
    @RequestMapping(value = "/**", produces = "text/xml")
    public String registerPayment(HttpServletRequest request, @RequestBody String xml)
            throws IOException, SOAPException {
        String uuid = UUID.randomUUID().toString();
        ThreadContext.push(uuid);
        ThreadContext.put("pay_id", uuid);
        ThreadContext.pop();
        return registrationService.registerPayment(xml, request);
    }


    @RequestMapping(value = "commit")
    public String commitPayment(HttpServletRequest request,
                                Model model,
                                @Nullable @RequestParam("trans_id") String transId,
                                @Nullable @RequestParam("error") String error,
                                @Nullable @RequestParam("ID") String id,
                                @Nullable @RequestParam("PASSWORD") String password
    ) {
        String uuid = UUID.randomUUID().toString();
        ThreadContext.push(uuid);
        ThreadContext.put("pay_id", uuid);
        ThreadContext.pop();
        Template template = commitService.commitPayment(request, transId, error, id, password);
        model.addAttribute("template", template);

        return "redirect";
    }

    @RequestMapping(value = "redirect")
    public String redirect(HttpServletRequest request,
                           Model model,
                           @Nullable @RequestHeader("Referer") String referer,
                           @Nullable @RequestParam("xid") String transId
    ) {
        String uuid = UUID.randomUUID().toString();
        ThreadContext.push(uuid);
        ThreadContext.put("pay_id", uuid);
        ThreadContext.pop();
        Object response = redirectService.redirect(request, referer, transId);
        if (response instanceof Template) {
            model.addAttribute("template", response);
            return "redirect";
        }
        if (response instanceof String) {
            return "redirect:" + response;
        }
        return "redirect";
    }

}
