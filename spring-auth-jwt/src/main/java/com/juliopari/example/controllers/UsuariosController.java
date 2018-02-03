package com.juliopari.example.controllers;

import com.juliopari.example.Usuario;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Creado por Julio Pari - jpari18@gmail.com
 */
@RestController
public class UsuariosController {

    @GetMapping(path = "/users")
    public List<Usuario> getUsers() {
        return Arrays.asList(new Usuario(1, "Julio"), new Usuario(2, "Cesar"), new Usuario(3, "Juan"));
    }

}
