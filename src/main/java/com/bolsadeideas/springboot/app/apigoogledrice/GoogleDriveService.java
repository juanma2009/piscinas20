package com.bolsadeideas.springboot.app.apigoogledrice;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "my-project-1470318760762";
    private static final String CREDENTIALS_FILE_PATH = "/cred.json";
    private static final String DEFAULT_FOLDER_NAME = "appjoyeria"; // Nombre de la carpeta donde se guardarán las fotos

    private final Drive driveService;

    // Constructor para inicializar el servicio de Google Drive
    public GoogleDriveService() throws GeneralSecurityException, IOException {
        this.driveService = getDriveService();
    }

    // Método para configurar el servicio de Google Drive
    private static Drive getDriveService() throws GeneralSecurityException, IOException {
        InputStream credentialsStream = GoogleDriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (credentialsStream == null) {
            throw new IllegalStateException("El archivo de credenciales no se encontró en la ruta especificada.");
        }

        GoogleCredential credential = GoogleCredential.fromStream(credentialsStream)
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName(APPLICATION_NAME).build();
    }

    // Método en GoogleDriveService
    public String uploadFileToFolder(MultipartFile multipartFile) throws IOException {
        String folderId = getOrCreateFolder("appjoyeria"); // Asegúrate de tener este método

        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());
        fileMetadata.setParents(Collections.singletonList(folderId));

        InputStreamContent mediaContent = new InputStreamContent(
                multipartFile.getContentType(), multipartFile.getInputStream()
        );

        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();

        return file.getId();
    }

    // Método para descargar un archivo por su ID
    public InputStream downloadFile(String fileId) throws IOException {
        return driveService.files().get(fileId).executeMediaAsInputStream();
    }

    // Método para eliminar un archivo por su ID
    public void deleteFile(String fileId) throws IOException {
        driveService.files().delete(fileId).execute();
    }

    // Método para listar archivos en la carpeta principal (o en general)
    public FileList listFiles() throws IOException {
        return driveService.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
    }

    // Método para buscar o crear la carpeta especificada
    private String getOrCreateFolder(String folderName) throws IOException {
        // Busca si la carpeta ya existe
        FileList result = driveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            // La carpeta ya existe
            return result.getFiles().get(0).getId();
        }

        // Crear una nueva carpeta si no existe
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId(); // Retorna el ID de la nueva carpeta
    }
}
