package com.codeaudit.service.impl;

import com.codeaudit.service.ReportService;
import com.codeaudit.vo.IssueVO;
import com.codeaudit.vo.ReviewResultVO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报告导出 - PDF / Word
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 严重级别对应颜色
    private static Color colorOf(String severity) {
        return switch (severity == null ? "" : severity) {
            case "CRITICAL" -> new Color(220, 38, 38);     // 红
            case "ERROR" -> new Color(234, 88, 12);         // 橙
            case "WARNING" -> new Color(202, 138, 4);      // 黄
            case "SUGGESTION" -> new Color(37, 99, 235);   // 蓝
            default -> Color.DARK_GRAY;
        };
    }

    @Override
    public byte[] exportPdf(ReviewResultVO r) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // 标题
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
            Paragraph title = new Paragraph("Code Review Report / 代码审查报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(Chunk.NEWLINE);

            // 概要
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 41, 59));
            doc.add(new Paragraph("1. Summary / 审查概要", h2));
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            doc.add(new Paragraph("File: " + nullSafe(r.getFileName()), body));
            doc.add(new Paragraph("Review Time: " + (r.getReviewTime() == null ? "-" : r.getReviewTime().format(DTF)), body));
            doc.add(new Paragraph("Lines: " + r.getLineCount() + "    Issues: " + r.getIssueCount() + "    Cost: " + r.getCostMs() + "ms", body));
            doc.add(Chunk.NEWLINE);

            // 严重级别统计
            doc.add(new Paragraph("2. Severity Statistics / 严重级别统计", h2));
            if (r.getStats() != null) {
                doc.add(new Paragraph(String.format("  CRITICAL=%d  ERROR=%d  WARNING=%d  SUGGESTION=%d  TOTAL=%d",
                        r.getStats().getCritical(), r.getStats().getError(),
                        r.getStats().getWarning(), r.getStats().getSuggestion(), r.getStats().getTotal()), body));
            }
            doc.add(Chunk.NEWLINE);

            // 安全风险摘要
            if (r.getSecurityIssues() != null && !r.getSecurityIssues().isEmpty()) {
                Font warnFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(220, 38, 38));
                doc.add(new Paragraph("3. Security Risks / 安全风险摘要", warnFont));
                doc.add(new Paragraph("⚠ 共发现 " + r.getSecurityIssues().size() + " 个安全问题，请优先修复", body));
                doc.add(Chunk.NEWLINE);
            }

            // 问题清单
            doc.add(new Paragraph("4. Issues / 问题清单", h2));
            if (r.getIssues() == null || r.getIssues().isEmpty()) {
                doc.add(new Paragraph("  ✓ 未发现任何问题，代码质量良好", body));
            } else {
                for (IssueVO i : r.getIssues()) {
                    Paragraph p = new Paragraph();
                    Font sev = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, colorOf(i.getSeverity()));
                    p.add(new Chunk("[" + i.getSeverity() + "]", sev));
                    p.add(new Chunk(" Line " + i.getLineNumber() + "  ", body));
                    p.add(new Chunk(i.getCategory() + " - " + nullSafe(i.getRuleName()), body));
                    doc.add(p);
                    doc.add(new Paragraph("    " + i.getDescription(), body));
                    if (i.getSuggestion() != null && !i.getSuggestion().isBlank()) {
                        Font fix = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(22, 163, 74));
                        doc.add(new Paragraph("    Fix: " + i.getSuggestion(), fix));
                    }
                    if (i.getAiExplain() != null && !i.getAiExplain().isBlank()) {
                        Font ai = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new Color(99, 102, 241));
                        doc.add(new Paragraph("    AI Explain: " + i.getAiExplain(), ai));
                    }
                    doc.add(Chunk.NEWLINE);
                }
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[REPORT] PDF 导出失败", e);
            throw new RuntimeException("PDF 导出失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportWord(ReviewResultVO r) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             XWPFDocument doc = new XWPFDocument()) {

            // 标题
            XWPFParagraph t = doc.createParagraph();
            t.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr = t.createRun();
            tr.setBold(true);
            tr.setFontSize(20);
            tr.setText("Code Review Report / 代码审查报告");

            doc.createParagraph().createRun().addBreak();

            // 概要
            doc.createParagraph(); // 空行
            XWPFParagraph h1 = doc.createParagraph();
            XWPFRun h1r = h1.createRun();
            h1r.setBold(true); h1r.setFontSize(14);
            h1r.setText("1. Summary / 审查概要");

            addKV(doc, "File", nullSafe(r.getFileName()));
            addKV(doc, "Review Time", r.getReviewTime() == null ? "-" : r.getReviewTime().format(DTF));
            addKV(doc, "Lines / Issues / Cost", r.getLineCount() + " / " + r.getIssueCount() + " / " + r.getCostMs() + "ms");

            // 严重级别
            XWPFParagraph h2 = doc.createParagraph();
            XWPFRun h2r = h2.createRun();
            h2r.setBold(true); h2r.setFontSize(14);
            h2r.setText("2. Severity Statistics / 严重级别统计");
            if (r.getStats() != null) {
                addKV(doc, "Counts", String.format("CRITICAL=%d ERROR=%d WARNING=%d SUGGESTION=%d TOTAL=%d",
                        r.getStats().getCritical(), r.getStats().getError(),
                        r.getStats().getWarning(), r.getStats().getSuggestion(), r.getStats().getTotal()));
            }

            // 安全
            if (r.getSecurityIssues() != null && !r.getSecurityIssues().isEmpty()) {
                XWPFParagraph h3 = doc.createParagraph();
                XWPFRun h3r = h3.createRun();
                h3r.setBold(true); h3r.setFontSize(14);
                h3r.setColor("DC2626");
                h3r.setText("3. Security Risks / 安全风险摘要");
                addKV(doc, "Security issues", r.getSecurityIssues().size() + " 个 - 请优先修复");
            }

            // 问题清单
            XWPFParagraph h4 = doc.createParagraph();
            XWPFRun h4r = h4.createRun();
            h4r.setBold(true); h4r.setFontSize(14);
            h4r.setText("4. Issues / 问题清单");
            if (r.getIssues() == null || r.getIssues().isEmpty()) {
                XWPFParagraph ok = doc.createParagraph();
                XWPFRun okr = ok.createRun();
                okr.setText("✓ 未发现任何问题，代码质量良好");
            } else {
                for (IssueVO i : r.getIssues()) {
                    XWPFParagraph ip = doc.createParagraph();
                    XWPFRun ir = ip.createRun();
                    ir.setBold(true);
                    ir.setText(String.format("[%s] Line %d  %s - %s",
                            i.getSeverity(), i.getLineNumber(), i.getCategory(), nullSafe(i.getRuleName())));
                    doc.createParagraph().createRun().setText("    " + i.getDescription());
                    if (i.getSuggestion() != null && !i.getSuggestion().isBlank()) {
                        XWPFParagraph fp = doc.createParagraph();
                        XWPFRun fr = fp.createRun();
                        fr.setColor("16A34A");
                        fr.setText("    Fix: " + i.getSuggestion());
                    }
                    if (i.getAiExplain() != null && !i.getAiExplain().isBlank()) {
                        XWPFParagraph ap = doc.createParagraph();
                        XWPFRun ar = ap.createRun();
                        ar.setItalic(true);
                        ar.setColor("6366F1");
                        ar.setText("    AI Explain: " + i.getAiExplain());
                    }
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[REPORT] Word 导出失败", e);
            throw new RuntimeException("Word 导出失败: " + e.getMessage());
        }
    }

    private void addKV(XWPFDocument doc, String k, String v) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setText(k + ": ");
        r.setBold(false);
        r.setText(v == null ? "-" : v);
    }

    private String nullSafe(String s) { return s == null ? "-" : s; }
}
