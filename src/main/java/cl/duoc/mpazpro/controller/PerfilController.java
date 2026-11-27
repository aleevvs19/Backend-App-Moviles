package cl.duoc.mpazpro.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PerfilController {

    @GetMapping("/yo")
    public Map<String, Object> yo(Authentication auth) {
        return Map.of(
                "usuario", auth.getName(),
                "roles", auth.getAuthorities().toString()
        );
    }
}