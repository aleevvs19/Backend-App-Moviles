package cl.duoc.mpazpro.controller;

import cl.duoc.mpazpro.dto.LoginRequest;
import cl.duoc.mpazpro.dto.LoginResponse;
import cl.duoc.mpazpro.model.Usuario;
import cl.duoc.mpazpro.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtEncoder jwtEncoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest datos) {
        if (datos.correo() == null || datos.password() == null || datos.perfil() == null) {
            return rechazo();
        }

        Optional<Usuario> encontrado = usuarios.findByCorreo(datos.correo().trim().toLowerCase());
        if (encontrado.isEmpty()) {
            return rechazo();
        }

        Usuario usuario = encontrado.get();
        boolean passwordOk = encoder.matches(datos.password(), usuario.getPasswordHash());
        boolean perfilOk = usuario.getPerfil().name().equals(datos.perfil());
        if (!passwordOk || !perfilOk) {
            return rechazo();
        }

        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getCorreo())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(8, ChronoUnit.HOURS))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return ResponseEntity.ok(new LoginResponse(token, usuario.getPerfil().name(), usuario.getNombre()));
    }

    private ResponseEntity<?> rechazo() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Correo o contraseña incorrectos"));
    }
}