package com.cloudnative.Semana3.service;

import com.cloudnative.Semana3.model.GuiaRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class GuiaService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String efsDirectory;

    // El constructor conecta el cliente de S3 y las rutas del application.properties
    public GuiaService(S3Client s3Client, 
                       @Value("${aws.s3.bucket-name}") String bucketName,
                       @Value("${app.efs.directory}") String efsDirectory) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.efsDirectory = efsDirectory;
    }

    /**
     * REQUISITO: Guarda temporalmente en la ruta local (EFS) y sube organizado a S3
     */
    public String crearOActualizarGuia(GuiaRequest request) throws IOException {
        // 1. Validar y crear el directorio local del EFS si no existe
        Path efsPath = Paths.get(efsDirectory);
        if (!Files.exists(efsPath)) {
            Files.createDirectories(efsPath);
        }
        
        String nombreArchivo = "guia" + request.getId() + ".pdf";
        Path archivoTemporal = efsPath.resolve(nombreArchivo);
        
        // Escribe el texto simulando el PDF dentro del almacenamiento local
        Files.writeString(archivoTemporal, request.getContenido(), StandardCharsets.UTF_8);

        // 2. Definir la estructura dinamica en S3: /{fecha}/{transportista}/guia{id}.pdf
        String s3Key = request.getFecha() + "/" + request.getTransportista() + "/" + nombreArchivo;

        // 3. Subir el archivo a Amazon S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivoTemporal.toFile()));
        
        return s3Key;
    }

    /**
     * REQUISITO: Descargar archivos con validacion de roles
     */
    public String descargarGuia(String s3Key, String rolUsuario) {
        // Validacion de seguridad solicitada en la rúbrica
        if (!"ADMIN".equalsIgnoreCase(rolUsuario) && !"TRANSPORTISTA".equalsIgnoreCase(rolUsuario)) {
            throw new SecurityException("Acceso denegado: No tienes los permisos requeridos.");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
        } catch (S3Exception e) {
            throw new RuntimeException("Error en AWS S3: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * REQUISITO: Consultar historial filtrando por transportista y fecha
     */
    public List<String> consultarGuias(String fecha, String transportista) {
        List<String> listaGuias = new ArrayList<>();
        String prefijoBusqueda = fecha + "/" + transportista + "/";

        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefijoBusqueda)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);
            
            for (S3Object s3Object : response.contents()) {
                listaGuias.add(s3Object.key());
            }
        } catch (S3Exception e) {
            throw new RuntimeException("Error de consulta en S3: " + e.awsErrorDetails().errorMessage());
        }

        return listaGuias;
    }

    /**
     * REQUISITO: Eliminar guías específicas
     */
    public void eliminarGuia(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new RuntimeException("Error al eliminar de S3: " + e.awsErrorDetails().errorMessage());
        }
    }
}