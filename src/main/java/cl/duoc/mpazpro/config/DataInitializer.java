package cl.duoc.mpazpro.config;

import cl.duoc.mpazpro.model.Perfil;
import cl.duoc.mpazpro.model.Usuario;
import cl.duoc.mpazpro.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        crear("Estudiante de prueba", "estudiante01@mpazpro.test", Perfil.estudiante);
        crear("Docente de prueba", "docente01@mpazpro.test", Perfil.docente);
        crear("Jefatura UTP de prueba", "utp01@mpazpro.test", Perfil.utp);
    }

    private void crear(String nombre, String correo, Perfil perfil) {
        if (usuarios.findByCorreo(correo).isEmpty()) {
            usuarios.save(new Usuario(nombre, correo, encoder.encode("1234"), perfil));
            System.out.println(">>> Usuario creado: " + correo);
        }
    }
}