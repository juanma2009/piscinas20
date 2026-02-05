package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String APPLICATION_NAME = "Joyeria App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Autowired
    private GoogleOAuthService oAuthService;

    @Value("${google.calendar.enabled:false}")
    private boolean calendarEnabled;

    public void crearEventoCita(Cita cita, String userId) {
        if (!calendarEnabled) {
            log.info("Google Calendar está deshabilitado en configuración");
            return;
        }

        if (cita.getCliente() == null || cita.getFechaCita() == null) {
            log.warn("No se puede crear evento de calendario: datos de cita incompletos");
            return;
        }

        try {
            String accessToken = oAuthService.getValidAccessTokenOrRefresh(userId);
            Calendar service = getCalendarService(accessToken);

            Event event = new Event()
                    .setSummary("Cita - " + cita.getCliente().getNombre() + " " + cita.getCliente().getApellido())
                    .setDescription(construirDescripcion(cita))
                    .setLocation("Joyería");

            Date fechaInicio = Date.from(cita.getFechaCita().atZone(ZoneId.systemDefault()).toInstant());
            DateTime startDateTime = new DateTime(fechaInicio);
            EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("Europe/Madrid");
            event.setStart(start);

            Date fechaFin = Date.from(cita.getFechaCita().plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
            DateTime endDateTime = new DateTime(fechaFin);
            EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("Europe/Madrid");
            event.setEnd(end);

            String calendarId = "primary";
            event = service.events().insert(calendarId, event).execute();
            log.info("✅ Evento creado en Google Calendar: {}", event.getHtmlLink());

        } catch (Exception e) {
            log.error("Error al crear evento en Google Calendar: {}", e.getMessage());
        }
    }

    private Calendar getCalendarService(String accessToken) throws Exception {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private String construirDescripcion(Cita cita) {
        StringBuilder desc = new StringBuilder();
        desc.append("Cliente: ").append(cita.getCliente().getNombre()).append(" ").append(cita.getCliente().getApellido());
        
        if (cita.getCliente().getTelefono() != null) {
            desc.append("\nTeléfono: ").append(cita.getCliente().getTelefono());
        }
        
        if (cita.getCliente().getEmail() != null) {
            desc.append("\nEmail: ").append(cita.getCliente().getEmail());
        }
        
        if (cita.getTipo() != null) {
            desc.append("\nTipo: ").append(cita.getTipo());
        }
        
        if (cita.getPedido() != null) {
            desc.append("\nPedido: #").append(cita.getPedido().getNpedido());
        }
        
        if (cita.getObservacion() != null && !cita.getObservacion().isEmpty()) {
            desc.append("\n\nObservaciones: ").append(cita.getObservacion());
        }
        
        return desc.toString();
    }
}
