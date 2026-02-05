package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void enviarNotificacionCita(Cita cita) {
        if (cita.getCliente() == null || cita.getCliente().getEmail() == null || cita.getCliente().getEmail().isEmpty()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(cita.getCliente().getEmail());
            helper.setSubject("Confirmación de Cita - Joyería");

            String htmlContent = construirEmailHtml(cita);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de notificación de cita: " + e.getMessage());
        }
    }

    private String construirEmailHtml(Cita cita) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fecha = cita.getFechaCita().format(formatter);
        String clienteNombre = cita.getCliente().getNombre() + " " + cita.getCliente().getApellido();
        String tipo = cita.getTipo() != null ? cita.getTipo().toString() : "No especificado";
        String estado = cita.getEstado() != null ? cita.getEstado().toString() : "PROGRAMADA";
        String observacion = cita.getObservacion() != null ? cita.getObservacion() : "";
        String pedidoRef = cita.getPedido() != null ? "#" + cita.getPedido().getNpedido() : "Sin pedido asociado";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #0062cc, #004085); color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
                ".content { background: #f8f9fa; padding: 20px; border: 1px solid #dee2e6; }" +
                ".info-row { margin: 10px 0; padding: 10px; background: white; border-left: 4px solid #0062cc; }" +
                ".label { font-weight: bold; color: #0062cc; }" +
                ".footer { text-align: center; margin-top: 20px; color: #6c757d; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>Confirmación de Cita</h2>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Estimado/a <strong>" + clienteNombre + "</strong>,</p>" +
                "<p>Le confirmamos su cita con los siguientes detalles:</p>" +
                "<div class='info-row'>" +
                "<span class='label'>📅 Fecha y Hora:</span> " + fecha +
                "</div>" +
                "<div class='info-row'>" +
                "<span class='label'>📋 Tipo de Cita:</span> " + tipo +
                "</div>" +
                "<div class='info-row'>" +
                "<span class='label'>✅ Estado:</span> " + estado +
                "</div>" +
                "<div class='info-row'>" +
                "<span class='label'>📦 Pedido:</span> " + pedidoRef +
                "</div>" +
                (observacion.isEmpty() ? "" : 
                "<div class='info-row'>" +
                "<span class='label'>💬 Observaciones:</span> " + observacion +
                "</div>") +
                "<p style='margin-top: 20px;'>Por favor, llegue 5 minutos antes de la hora programada.</p>" +
                "<p>Si necesita modificar o cancelar su cita, por favor contáctenos.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Este es un correo automático, por favor no responder.</p>" +
                "<p>&copy; 2026 Joyería - Todos los derechos reservados</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
