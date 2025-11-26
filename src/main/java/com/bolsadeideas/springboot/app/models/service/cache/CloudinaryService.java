package com.bolsadeideas.springboot.app.models.service.cache;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private  Cloudinary cloudinary;

    @Cacheable(value = "cloudinaryImages", key = "#fileName")  // Cachea la URL de la imagen por nombre
    public String uploadImage(File imageBytes, String fileName) throws IOException {
        // Subir la imagen a Cloudinary
        Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.emptyMap());
        return (String) uploadResult.get("secure_url");  // Devuelve la URL segura de Cloudinary
    }
}

