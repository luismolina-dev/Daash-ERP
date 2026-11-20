package com.daash.erp.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envía la factura de compra por SMTP real cuando está configurado.
 * Sin SMTP se registra SIMULADO y el PDF queda disponible en la aplicación.
 */
@Service
public class PurchaseMailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String host;
    private final String from;

    public PurchaseMailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${spring.mail.host:}") String host,
                               @Value("${app.mail-from:no-reply@daash.local}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.host = host;
        this.from = from;
    }

    public MailResult sendInvoice(String to, String numeroCompra, byte[] pdf) {
        if (to == null || to.isBlank()) {
            return new MailResult("FALLIDO", "No se configuró el correo del cliente");
        }
        if (host == null || host.isBlank()) {
            return new MailResult("SIMULADO", "SMTP no configurado; descargue el PDF desde la aplicación");
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || pdf == null || pdf.length == 0) {
            return new MailResult("FALLIDO", "No hay servicio de correo o PDF disponible");
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Factura de compra " + numeroCompra + " - Daash ERP & DTE");
            helper.setText("""
                    <p>Estimado cliente:</p>
                    <p>Adjunto la factura de compra (crédito fiscal) <strong>%s</strong> generada por Daash ERP &amp; DTE.</p>
                    <p>El documento también está disponible en el módulo Compras de la aplicación.</p>
                    <p>Atentamente,<br/>Daash ERP &amp; DTE</p>
                    """.formatted(numeroCompra), true);
            helper.addAttachment(numeroCompra + ".pdf", new ByteArrayResource(pdf));
            sender.send(message);
            return new MailResult("ENVIADO", null);
        } catch (Exception ex) {
            return new MailResult("FALLIDO", truncate(ex.getMessage()));
        }
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) return "Error al enviar el correo";
        return message.length() > 480 ? message.substring(0, 480) : message;
    }

    public record MailResult(String estado, String error) { }
}
