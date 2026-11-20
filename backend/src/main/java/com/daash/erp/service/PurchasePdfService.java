package com.daash.erp.service;

import com.daash.erp.model.Purchase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Genera el PDF del crédito fiscal de una compra (factura de compra). */
@Service
public class PurchasePdfService {

    private static final Color PRIMARY = new Color(15, 118, 110);
    private static final Color PRIMARY_DARK = new Color(6, 78, 72);
    private static final Color SOFT = new Color(236, 253, 245);
    private static final Color PALE = new Color(248, 250, 252);
    private static final Color LINE = new Color(203, 213, 225);
    private static final Color INK = new Color(15, 23, 42);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color WHITE = new Color(255, 255, 255);

    private final String companyNombre;
    private final String companyNit;
    private final String companyNrc;
    private final String companyDireccion;

    public PurchasePdfService(@Value("${app.company.nombre}") String companyNombre,
                              @Value("${app.company.nit}") String companyNit,
                              @Value("${app.company.nrc}") String companyNrc,
                              @Value("${app.company.direccion}") String companyDireccion) {
        this.companyNombre = companyNombre;
        this.companyNit = companyNit;
        this.companyNrc = companyNrc;
        this.companyDireccion = companyDireccion;
    }

    public byte[] generate(Purchase purchase) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.LETTER, 38, 38, 36, 34);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            document.open();

            Font brand = new Font(Font.HELVETICA, 17, Font.BOLD, WHITE);
            Font brandSub = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, new Color(204, 251, 241));
            Font badge = new Font(Font.HELVETICA, 9, Font.BOLD, WHITE);
            Font label = new Font(Font.HELVETICA, 7.5f, Font.BOLD, MUTED);
            Font value = new Font(Font.HELVETICA, 9, Font.BOLD, INK);
            Font section = new Font(Font.HELVETICA, 9, Font.BOLD, PRIMARY_DARK);
            Font normal = new Font(Font.HELVETICA, 9, Font.NORMAL, INK);
            Font small = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
            Font headerWhite = new Font(Font.HELVETICA, 8, Font.BOLD, WHITE);

            // Cabecera con franja corporativa y distintivo de simulación.
            PdfPTable bannerInner = new PdfPTable(new float[]{4.2f, 1f});
            bannerInner.setWidthPercentage(100);
            PdfPCell bannerLeft = borderlessCell();
            Paragraph company = new Paragraph(safe(companyNombre), brand);
            company.setSpacingAfter(2f);
            bannerLeft.addElement(company);
            bannerLeft.addElement(new Paragraph("CRÉDITO FISCAL ELECTRÓNICO · FACTURA DE COMPRA", brandSub));
            PdfPCell bannerRight = borderlessCell();
            bannerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            bannerRight.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bannerRight.addElement(new Paragraph("SIMULADO", badge));
            bannerInner.addCell(bannerLeft);
            bannerInner.addCell(bannerRight);

            PdfPCell banner = new PdfPCell(bannerInner);
            banner.setBackgroundColor(PRIMARY_DARK);
            banner.setBorder(Rectangle.NO_BORDER);
            banner.setPadding(12f);
            PdfPTable bannerTable = new PdfPTable(1);
            bannerTable.setWidthPercentage(100);
            bannerTable.addCell(banner);
            document.add(bannerTable);

            // Datos clave del documento.
            PdfPTable meta = new PdfPTable(new float[]{1.05f, 1f, 1.5f});
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(10f);
            meta.setSpacingAfter(9f);
            meta.addCell(stackedCell("NÚMERO", safe(purchase.numeroCompra), label, value));
            meta.addCell(stackedCell("FECHA", formatDate(purchase), label, value));
            meta.addCell(stackedCell("CÓDIGO DE GENERACIÓN", safe(purchase.codigoGeneracion), label, value));
            document.add(meta);

            // Emisor y receptor.
            PdfPTable parties = new PdfPTable(new float[]{1f, 1f});
            parties.setWidthPercentage(100);
            parties.setSpacingAfter(10f);
            parties.addCell(partyCell("EMISOR (PROVEEDOR)", section, normal,
                    "Nombre: " + safe(purchase.supplierNombre),
                    "NIT: " + safe(purchase.supplierNit),
                    "NRC: " + safe(purchase.supplierNrc),
                    "Dirección: " + safe(purchase.supplierDireccion)));
            parties.addCell(partyCell("RECEPTOR", section, normal,
                    "Nombre: " + safe(companyNombre),
                    "NIT: " + safe(companyNit),
                    "NRC: " + safe(companyNrc),
                    "Dirección: " + safe(companyDireccion)));
            document.add(parties);

            // Detalle de bienes/servicios.
            PdfPTable items = new PdfPTable(new float[]{0.75f, 4.3f, 1.25f, 1.6f, 1.6f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(8f);
            addHeaderCell(items, "CANT.", headerWhite, Element.ALIGN_CENTER);
            addHeaderCell(items, "DESCRIPCIÓN", headerWhite, Element.ALIGN_LEFT);
            addHeaderCell(items, "P. UNIT.", headerWhite, Element.ALIGN_RIGHT);
            addHeaderCell(items, "SUBTOTAL", headerWhite, Element.ALIGN_RIGHT);
            addHeaderCell(items, "IVA 13%", headerWhite, Element.ALIGN_RIGHT);

            List<PurchaseItemView> lines = purchase.items == null
                    ? List.of()
                    : purchase.items.stream().map(PurchaseItemView::from).toList();
            int row = 0;
            for (PurchaseItemView item : lines) {
                boolean striped = row++ % 2 == 0;
                addDataCell(items, item.cantidad == null ? "-" : String.valueOf(item.cantidad),
                        normal, Element.ALIGN_CENTER, striped);
                addDataCell(items, safe(item.productoNombre), normal, Element.ALIGN_LEFT, striped);
                addDataCell(items, money(item.precioUnitario), normal, Element.ALIGN_RIGHT, striped);
                addDataCell(items, money(item.subtotal), normal, Element.ALIGN_RIGHT, striped);
                addDataCell(items, money(nz(item.subtotal).multiply(new BigDecimal("0.13"))
                        .setScale(2, RoundingMode.HALF_UP)), normal, Element.ALIGN_RIGHT, striped);
            }
            if (lines.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Sin ítems registrados", normal));
                empty.setColspan(5);
                empty.setPadding(8f);
                empty.setBorderColor(LINE);
                items.addCell(empty);
            }
            document.add(items);

            // Resumen de montos.
            PdfPTable totals = new PdfPTable(new float[]{3f, 1.5f});
            totals.setWidthPercentage(42);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingAfter(10f);
            addTotalRow(totals, "Subtotal", money(purchase.subtotal), value, false);
            addTotalRow(totals, "IVA 13%", money(purchase.iva), value, false);
            addTotalRow(totals, "TOTAL", money(purchase.total),
                    new Font(Font.HELVETICA, 10, Font.BOLD, WHITE), true);
            document.add(totals);

            // Bloque de verificación con QR.
            String hash = sha256(safe(purchase.dteFirma));
            PdfPTable verification = new PdfPTable(new float[]{1.25f, 4f});
            verification.setWidthPercentage(100);
            verification.setSpacingBefore(4f);
            verification.setSpacingAfter(8f);

            PdfPCell qrCell = new PdfPCell();
            qrCell.setBackgroundColor(SOFT);
            qrCell.setBorderColor(LINE);
            qrCell.setPadding(7f);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Image qr = qrImage(qrPayload(purchase, hash));
            qr.scaleAbsolute(84f, 84f);
            qrCell.addElement(qr);
            verification.addCell(qrCell);

            PdfPCell verifyInfo = new PdfPCell();
            verifyInfo.setBackgroundColor(PALE);
            verifyInfo.setBorderColor(LINE);
            verifyInfo.setPadding(8f);
            Paragraph verifyTitle = new Paragraph("VERIFICACIÓN SIMULADA", section);
            verifyTitle.setSpacingAfter(3f);
            verifyInfo.addElement(verifyTitle);
            verifyInfo.addElement(new Paragraph("Escanea el QR para leer los datos firmados del crédito fiscal.", normal));
            verifyInfo.addElement(new Paragraph("Huella de firma: " + hash.substring(0, Math.min(24, hash.length())) + "…", small));
            verifyInfo.addElement(new Paragraph("Correo destino: " + safe(purchase.customerEmail), small));
            verification.addCell(verifyInfo);
            document.add(verification);

            Paragraph footer = new Paragraph(
                    "Generado electrónicamente por Daash ERP & DTE · Firma JWT HS256 simulada · "
                            + "No sustituye un documento fiscal autorizado por Hacienda.", small);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            // Marca de agua diagonal para dejar claro que es una simulación.
            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState alpha = new PdfGState();
            alpha.setFillOpacity(0.09f);
            canvas.saveState();
            canvas.setGState(alpha);
            canvas.setColorFill(PRIMARY);
            canvas.beginText();
            canvas.setFontAndSize(
                    BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED),
                    62f);
            canvas.showTextAligned(Element.ALIGN_CENTER, "SIMULADO", 306f, 370f, 42f);
            canvas.endText();
            canvas.restoreState();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la compra", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return output.toByteArray();
    }

    private PdfPCell borderlessCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2f);
        return cell;
    }

    private PdfPCell stackedCell(String labelText, String valueText, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(SOFT);
        cell.setBorderColor(LINE);
        cell.setPadding(7f);
        cell.addElement(new Paragraph(labelText, labelFont));
        Paragraph paragraph = new Paragraph(safe(valueText), valueFont);
        paragraph.setSpacingBefore(3f);
        cell.addElement(paragraph);
        return cell;
    }

    private PdfPCell partyCell(String title, Font titleFont, Font bodyFont, String... lines) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PALE);
        cell.setBorderColor(LINE);
        cell.setPadding(8f);
        Paragraph heading = new Paragraph(title, titleFont);
        heading.setSpacingAfter(4f);
        cell.addElement(heading);
        for (String line : lines) {
            cell.addElement(new Paragraph(safe(line), bodyFont));
        }
        return cell;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(PRIMARY);
        cell.setBorderColor(PRIMARY);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Font font, int alignment, boolean striped) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setBackgroundColor(striped ? WHITE : PALE);
        cell.setBorderColor(LINE);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String labelText, String amount, Font font, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(labelText, font));
        PdfPCell amountCell = new PdfPCell(new Phrase(amount, font));
        labelCell.setBackgroundColor(highlight ? PRIMARY : WHITE);
        amountCell.setBackgroundColor(highlight ? PRIMARY : WHITE);
        labelCell.setBorderColor(highlight ? PRIMARY : LINE);
        amountCell.setBorderColor(highlight ? PRIMARY : LINE);
        labelCell.setPadding(5f);
        amountCell.setPadding(5f);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(amountCell);
    }

    private Image qrImage(String payload) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        var matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 220, 220, hints);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? INK.getRGB() : WHITE.getRGB());
            }
        }
        Image pdfImage = Image.getInstance(image, WHITE);
        pdfImage.setAlignment(Element.ALIGN_CENTER);
        return pdfImage;
    }

    private String qrPayload(Purchase purchase, String hash) {
        long fecha = purchase.fecha == null ? 0L : purchase.fecha.toEpochMilli();
        return "DAASH-DTE|1.4|CF_COMPRA|"
                + token(purchase.numeroCompra) + "|"
                + token(purchase.codigoGeneracion) + "|"
                + fecha + "|"
                + money(purchase.total) + "|"
                + hash;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            return "SIN-HASH";
        }
    }

    private String token(String value) {
        return safe(value).replace("|", "/").replace("\n", " ");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return "$" + nz(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(Purchase purchase) {
        if (purchase.fecha == null) return "-";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date.from(purchase.fecha));
    }

    /** Adaptador local para evitar depender de la entidad en el renderizado. */
    private record PurchaseItemView(Integer cantidad, String productoNombre,
                                    BigDecimal precioUnitario, BigDecimal subtotal) {
        static PurchaseItemView from(com.daash.erp.model.PurchaseItem item) {
            return new PurchaseItemView(item.cantidad, item.productoNombre,
                    item.precioUnitario, item.subtotal);
        }
    }
}
