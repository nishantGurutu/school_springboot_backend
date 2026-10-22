package com.school.management.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.school.management.entity.CertificateEntity;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class CertificatePdfService {

    // Color Palette matching the exact screenshot
    private static final Color GOLD_BORDER = new Color(245, 158, 11);  // #F59E0B
    private static final Color GOLD_LIGHT  = new Color(253, 230, 138); // #FDE68A
    private static final Color GOLD_AMBER  = new Color(217, 119, 6);   // #D97706
    private static final Color GOLD_STAR   = new Color(234, 179, 8);   // #EAB308
    private static final Color ROYAL_BLUE  = new Color(37, 99, 235);   // #2563EB
    private static final Color NAVY_TITLE  = new Color(30, 58, 138);   // #1E3A8A
    private static final Color DARK_NAME   = new Color(15, 23, 42);     // #0F172A
    private static final Color SLATE_TEXT  = new Color(71, 85, 105);   // #475569
    private static final Color MUTED_TEXT  = new Color(100, 116, 139); // #64748B
    private static final Color DASH_LINE   = new Color(148, 163, 184); // #94A3B8
    private static final Color DIVIDER_LINE= new Color(226, 232, 240); // #E2E8F0
    private static final Color SEAL_BG     = new Color(254, 243, 199); // #FEF3C7

    public byte[] generateCertificatePdf(CertificateEntity cert) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // A4 Landscape: 842 x 595 pt
            Document document = new Document(new Rectangle(842, 740), 60, 60, 125, 240);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Draw outer and inner borders, stars, top medal, and center seal
            writer.setPageEvent(new CertificateDesignEvent());

            document.open();

            // 1. EDUDASH ACADEMY
            Paragraph academyPara = new Paragraph("EDUDASH ACADEMY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, ROYAL_BLUE));
            academyPara.setAlignment(Element.ALIGN_CENTER);
            academyPara.setSpacingBefore(0f);
            academyPara.setSpacingAfter(8f);
            document.add(academyPara);

            // 2. Certificate Title: APPRETIATION
            String title = (cert.getCertificateName() != null && !cert.getCertificateName().isBlank())
                    ? cert.getCertificateName().toUpperCase()
                    : "APPRETIATION";
            Paragraph titlePara = new Paragraph(title,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, NAVY_TITLE));
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(6f);
            document.add(titlePara);

            // 3. Subtitle: is hereby granted to:
            Paragraph subPara = new Paragraph("is hereby granted to:",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, MUTED_TEXT));
            subPara.setAlignment(Element.ALIGN_CENTER);
            subPara.setSpacingAfter(14f);
            document.add(subPara);

            // 4. Student Name in large Serif
            String studentName = (cert.getName() != null && !cert.getName().isBlank())
                    ? cert.getName()
                    : "Student Name";
            Paragraph namePara = new Paragraph(studentName,
                    FontFactory.getFont(FontFactory.TIMES_BOLD, 36, DARK_NAME));
            namePara.setAlignment(Element.ALIGN_CENTER);
            namePara.setSpacingAfter(4f);
            document.add(namePara);

            // 5. Dashed underline beneath name
            Paragraph dashPara = new Paragraph("--------------------------------------------------------------------------------",
                    FontFactory.getFont(FontFactory.COURIER, 10, DASH_LINE));
            dashPara.setAlignment(Element.ALIGN_CENTER);
            dashPara.setSpacingAfter(14f);
            document.add(dashPara);

            // 6. Citation paragraph
            String className = (cert.getClassName() != null && !cert.getClassName().isBlank())
                    ? cert.getClassName()
                    : "Play (A)";
            Paragraph citationPara = new Paragraph();
            citationPara.setAlignment(Element.ALIGN_CENTER);
            citationPara.setSpacingAfter(14f);
            citationPara.setLeading(18f);

            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12, SLATE_TEXT);
            Font boldTextFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, DARK_NAME);

            citationPara.add(new Chunk("for outstanding academic performance, exemplary conduct, and active participation in ", textFont));
            citationPara.add(new Chunk(className, boldTextFont));
            citationPara.add(new Chunk(".", textFont));
            document.add(citationPara);

            // 7. Date: 02/10/2026
            String dateStr = (cert.getDate() != null && !cert.getDate().isBlank())
                    ? cert.getDate()
                    : "02/10/2026";
            Paragraph datePara = new Paragraph("Date: " + dateStr,
                    FontFactory.getFont(FontFactory.HELVETICA, 11.5f, MUTED_TEXT));
            datePara.setAlignment(Element.ALIGN_CENTER);
            datePara.setSpacingAfter(36f);
            document.add(datePara);

            // 8. Signatures Table (Left and Right, center left empty for vector seal badge)
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(86);
            footerTable.setWidths(new float[]{38, 24, 38});

            String footerLeft = (cert.getFooterLeft() != null && !cert.getFooterLeft().isBlank())
                    ? cert.getFooterLeft()
                    : "Principal Signature";
            String footerRight = (cert.getFooterRight() != null && !cert.getFooterRight().isBlank())
                    ? cert.getFooterRight()
                    : "Class Teacher Signature";

            // Extract person name from title if in format "Name (Role)"
            String leftName = "";
            if (footerLeft.contains("(")) {
                leftName = footerLeft.substring(0, footerLeft.indexOf("(")).trim();
            } else if (!footerLeft.isBlank()) {
                leftName = footerLeft.trim();
            }

            String rightName = "";
            if (footerRight.contains("(")) {
                rightName = footerRight.substring(0, footerRight.indexOf("(")).trim();
            } else if (!footerRight.isBlank()) {
                rightName = footerRight.trim();
            }

            // Left Cell (Principal)
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);

            Paragraph leftSign = new Paragraph(leftName, FontFactory.getFont(FontFactory.TIMES_ITALIC, 18, DARK_NAME));
            leftSign.setAlignment(Element.ALIGN_LEFT);
            leftCell.addElement(leftSign);

            Paragraph leftLine = new Paragraph("_________________________", FontFactory.getFont(FontFactory.HELVETICA, 8, DASH_LINE));
            leftLine.setAlignment(Element.ALIGN_LEFT);
            leftLine.setSpacingAfter(2f);
            leftCell.addElement(leftLine);

            Paragraph leftTitle = new Paragraph(footerLeft, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, MUTED_TEXT));
            leftTitle.setAlignment(Element.ALIGN_LEFT);
            leftCell.addElement(leftTitle);

            footerTable.addCell(leftCell);

            // Center Cell (Spacing cell for Seal drawn at cx=421, cy=95)
            PdfPCell centerCell = new PdfPCell();
            centerCell.setBorder(Rectangle.NO_BORDER);
            centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph emptySpace = new Paragraph(" ");
            centerCell.addElement(emptySpace);
            footerTable.addCell(centerCell);

            // Right Cell (Class Teacher)
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph rightSign = new Paragraph(rightName, FontFactory.getFont(FontFactory.TIMES_ITALIC, 18, DARK_NAME));
            rightSign.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(rightSign);

            Paragraph rightLine = new Paragraph("_________________________", FontFactory.getFont(FontFactory.HELVETICA, 8, DASH_LINE));
            rightLine.setAlignment(Element.ALIGN_RIGHT);
            rightLine.setSpacingAfter(2f);
            rightCell.addElement(rightLine);

            Paragraph rightTitle = new Paragraph(footerRight, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, MUTED_TEXT));
            rightTitle.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(rightTitle);

            footerTable.addCell(rightCell);

            footerTable.setTotalWidth(document.getPageSize().getWidth() - 120f);
            footerTable.writeSelectedRows(0, -1, 60f, 200f, writer.getDirectContent());

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certificate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Draws borders, stars, top medal, signature divider, and center gold seal.
     */
    private static class CertificateDesignEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();
            float cx = width / 2.0f;

            cb.saveState();

            // 1. Outer gold rounded border (#F59E0B)
            cb.setLineWidth(3.5f);
            cb.setColorStroke(GOLD_BORDER);
            cb.setColorFill(SEAL_BG);
            cb.roundRectangle(26f, 26f, width - 52f, height - 52f, 16f);
            cb.fillStroke();

            // 2. Inner subtle accent border
            cb.setLineWidth(0.8f);
            cb.setColorStroke(GOLD_LIGHT);
            cb.setColorFill(Color.WHITE);
            cb.roundRectangle(32f, 32f, width - 64f, height - 64f, 12f);
            cb.fillStroke();

            // 3. Top Emblem: Stars and Medal
            // Stars on left
            drawStar(cb, cx - 62f, height - 72f, 8.5f, 3.8f, GOLD_STAR);
            drawStar(cb, cx - 36f, height - 72f, 8.5f, 3.8f, GOLD_STAR);

            // Center Medal
            drawMedal(cb, cx, height - 70f, 12f, GOLD_BORDER);

            // Stars on right
            drawStar(cb, cx + 36f, height - 72f, 8.5f, 3.8f, GOLD_STAR);
            drawStar(cb, cx + 62f, height - 72f, 8.5f, 3.8f, GOLD_STAR);

            // 4. Subtle divider line above signatures
            cb.setLineWidth(0.8f);
            cb.setColorStroke(DIVIDER_LINE);
            cb.moveTo(60f, 220f);
            cb.lineTo(width - 60f, 220f);
            cb.stroke();

            // 5. Center Seal Badge between Arthur Vance and Sarah Jenkins
            drawSealBadge(cb, cx, 167f, 24f);

            cb.restoreState();
        }

        private void drawStar(PdfContentByte cb, float cx, float cy, float outerR, float innerR, Color color) {
            cb.saveState();
            cb.setColorFill(color);
            cb.setColorStroke(color);
            double angle = -Math.PI / 2.0;
            double step = Math.PI / 5.0;
            for (int i = 0; i < 10; i++) {
                float r = (i % 2 == 0) ? outerR : innerR;
                float x = (float) (cx + r * Math.cos(angle));
                float y = (float) (cy + r * Math.sin(angle));
                if (i == 0) {
                    cb.moveTo(x, y);
                } else {
                    cb.lineTo(x, y);
                }
                angle += step;
            }
            cb.closePath();
            cb.fill();
            cb.restoreState();
        }

        private void drawMedal(PdfContentByte cb, float cx, float cy, float radius, Color color) {
            cb.saveState();
            cb.setColorStroke(color);
            cb.setLineWidth(2.2f);

            // Medal circle
            cb.circle(cx, cy, radius);
            cb.stroke();

            // Ribbon tails hanging below
            float ribbonBottom = cy - radius - 10f;
            cb.setLineWidth(2.0f);

            // Left ribbon tail
            cb.moveTo(cx - 4f, cy - radius + 2f);
            cb.lineTo(cx - 9f, ribbonBottom);
            cb.lineTo(cx - 4f, ribbonBottom + 4f);
            cb.lineTo(cx - 1f, cy - radius);
            cb.stroke();

            // Right ribbon tail
            cb.moveTo(cx + 1f, cy - radius);
            cb.lineTo(cx + 4f, ribbonBottom + 4f);
            cb.lineTo(cx + 9f, ribbonBottom);
            cb.lineTo(cx + 4f, cy - radius + 2f);
            cb.stroke();

            cb.restoreState();
        }

        private void drawSealBadge(PdfContentByte cb, float cx, float cy, float radius) {
            cb.saveState();

            // Warm light background
            cb.setColorFill(SEAL_BG);
            cb.circle(cx, cy, radius);
            cb.fill();

            // Outer gold border
            cb.setLineWidth(2.2f);
            cb.setColorStroke(GOLD_BORDER);
            cb.circle(cx, cy, radius);
            cb.stroke();

            // Inner circle
            cb.setLineWidth(0.8f);
            cb.setColorStroke(GOLD_AMBER);
            cb.circle(cx, cy, radius - 4f);
            cb.stroke();

            // Inside medal emblem
            drawMedal(cb, cx, cy + 2f, 8.5f, GOLD_AMBER);

            cb.restoreState();
        }
    }
}

