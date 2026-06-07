package com.cloudnative.Semana3.controller;

import com.cloudnative.Semana3.model.GuiaRequest;
import com.cloudnative.Semana3.service.GuiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    // Endpoint: Crear o actualizar guías
    @PostMapping
    public ResponseEntity<String> crearOActualizarGuia(@RequestBody GuiaRequest request) {
        try {
            String s3Path = guiaService.crearOActualizarGuia(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Archivo guardado temporalmente en EFS y respaldado en S3 en la ruta: " + s3Path);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error de escritura en almacenamiento EFS: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al subir a AWS S3: " + e.getMessage());
        }
    }

    // Endpoint: Descargar guías (¡Corregido con G mayúscula!)
    @GetMapping("/descargar")
    public ResponseEntity<String> descargarGuia(
            @RequestParam String s3Key, 
            @RequestHeader(value = "X-User-Role", defaultValue = "INVITADO") String rolUsuario) {
        try {
            String contenido = guiaService.descargarGuia(s3Key, rolUsuario);
            return ResponseEntity.ok(contenido);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró el archivo solicitado.");
        }
    }

    // Endpoint: Consultar guías por filtros
    @GetMapping("/buscar")
    public ResponseEntity<List<String>> consultarGuias(
            @RequestParam String fecha, 
            @RequestParam String transportista) {
        List<String> guias = guiaService.consultarGuias(fecha, transportista);
        return ResponseEntity.ok(guias);
    }

    // Endpoint: Eliminar guías específicas
    @DeleteMapping
    public ResponseEntity<String> eliminarGuia(@RequestParam String s3Key) {
        try {
            guiaService.eliminarGuia(s3Key);
            return ResponseEntity.ok("Guía '" + s3Key + "' eliminada correctamente de AWS S3.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error al intentar eliminar: " + e.getMessage());
        }
    }
}