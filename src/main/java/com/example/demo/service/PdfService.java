package com.example.demo.service;

import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class PdfService {

    @Autowired private CustomerRepository customerRepo;

    private static final DecimalFormat df = new DecimalFormat("#,##0.00");
    private static final String[] units = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen" };
    private static final String[] tens = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };

    public ByteArrayInputStream generateInvoice(Sale sale) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // --- FONTS ---
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(44, 62, 80));
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            // --- HEADER ---
            Paragraph storeName = new Paragraph("SMARTSTORE Enterprises", headerFont);
            storeName.setAlignment(Element.ALIGN_CENTER);
            document.add(storeName);

            Paragraph storeAddr = new Paragraph("Tech Innovation Drive, TechPioneers, 400090- Mumbai\n9089853298 | www.smartstore.com\nBandra Kurla Complex", subHeaderFont);
            storeAddr.setAlignment(Element.ALIGN_CENTER);
            storeAddr.setSpacingAfter(15);
            document.add(storeAddr);

            LineSeparator ls = new LineSeparator();
            ls.setLineColor(Color.LIGHT_GRAY);
            document.add(new Chunk(ls));

            // --- METADATA ---
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingBefore(10);
            metaTable.setSpacingAfter(10);

            PdfPCell leftMeta = new PdfPCell();
            leftMeta.setBorder(Rectangle.NO_BORDER);
            leftMeta.addElement(new Paragraph("INVOICE / RECEIPT", boldFont));
            leftMeta.addElement(new Paragraph("Invoice No: " + sale.getId(), bodyFont));
            leftMeta.addElement(new Paragraph("Date: " + formatDate(sale.getDate()), bodyFont));
            
            if(sale.getCustomerPhone() != null && !sale.getCustomerPhone().isEmpty()) {
                leftMeta.addElement(new Paragraph("Customer: " + sale.getCustomerPhone(), bodyFont));
            }
            
            metaTable.addCell(leftMeta);

            PdfPCell rightMeta = new PdfPCell();
            rightMeta.setBorder(Rectangle.NO_BORDER);
            rightMeta.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph pRight = new Paragraph();
            pRight.setAlignment(Element.ALIGN_RIGHT);
            pRight.add(new Chunk("Cashier: " + (sale.getCashierName() != null ? sale.getCashierName() : "N/A") + "\n", bodyFont));
            pRight.add(new Chunk("Status: " + (sale.getStatus()!=null?sale.getStatus():"Paid") + "\nMethod: " + (sale.getPaymentMethod()!=null?sale.getPaymentMethod():"Cash"), bodyFont));
            rightMeta.addElement(pRight);
            metaTable.addCell(rightMeta);

            document.add(metaTable);

            // --- ITEMS TABLE ---
            PdfPTable table = new PdfPTable(5); 
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 4f, 2f, 1.5f, 2f});
            table.setSpacingBefore(5);

            table.addCell(createStyledHeader("S/No.", tableHeaderFont));
            table.addCell(createStyledHeader("Description", tableHeaderFont));
            table.addCell(createStyledHeader("Unit Price", tableHeaderFont)); 
            table.addCell(createStyledHeader("Qty", tableHeaderFont));
            table.addCell(createStyledHeader("Total (Rs.)", tableHeaderFont));

            int i = 1;
            for (SaleItem item : sale.getItems()) {
                table.addCell(createCell(String.valueOf(i++), bodyFont, Element.ALIGN_CENTER));
                table.addCell(createCell(item.getProductName(), bodyFont, Element.ALIGN_LEFT));
                table.addCell(createCell(df.format(item.getPrice()), bodyFont, Element.ALIGN_RIGHT));
                table.addCell(createCell(String.valueOf(item.getQuantity()), bodyFont, Element.ALIGN_CENTER));
                BigDecimal subtotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
                table.addCell(createCell(df.format(subtotal), bodyFont, Element.ALIGN_RIGHT));
            }
            document.add(table);

            // --- TOTALS ---
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(40);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setSpacingBefore(10);
            summaryTable.addCell(createSummaryCell("Grand Total", boldFont));
            summaryTable.addCell(createSummaryCell("Rs. " + df.format(sale.getTotalAmount()), boldFont));
            document.add(summaryTable);
            
            // --- AMOUNT IN WORDS ---
            document.add(new Paragraph(" "));
            String moneyString = convertToWords(sale.getTotalAmount().longValue());
            Paragraph words = new Paragraph("Amount In Words: " + moneyString + " Rupees Only", 
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.DARK_GRAY));
            document.add(words);

            // 🚀 THE FIX: This Line Separator is now OUTSIDE the 'if' block.
            // It will draw every time, regardless of customer loyalty status.
            document.add(new Paragraph(" ")); 
            LineSeparator bottomLine = new LineSeparator();
            bottomLine.setLineColor(Color.LIGHT_GRAY);
            document.add(new Chunk(bottomLine));

            // --- LOYALTY FOOTER (Optional) ---
            if(sale.getCustomerPhone() != null && !sale.getCustomerPhone().isEmpty()) {
                // Direct call instead of Optional to avoid errors
                com.example.demo.model.Customer cust = customerRepo.findByPhone(sale.getCustomerPhone());
                
                if(cust != null) {
                    document.add(new Paragraph(" ")); 
                    
                    PdfPTable loyaltyTable = new PdfPTable(1);
                    loyaltyTable.setWidthPercentage(100);
                    
                    PdfPCell lCell = new PdfPCell();
                    lCell.setBorder(Rectangle.NO_BORDER); // Removed border to blend with new line
                    lCell.setPadding(5);
                    lCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    
                    Font loyaltyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(16, 185, 129)); 
                    Paragraph p1 = new Paragraph("LOYALTY BALANCE: " + cust.getPoints() + " POINTS", loyaltyFont);
                    p1.setAlignment(Element.ALIGN_CENTER);
                    
                    Paragraph p2 = new Paragraph("(Includes points earned on this purchase)", footerFont);
                    p2.setAlignment(Element.ALIGN_CENTER);
                    
                    lCell.addElement(p1);
                    lCell.addElement(p2);
                    loyaltyTable.addCell(lCell);
                    
                    document.add(loyaltyTable);

                    // Add a second line below loyalty to keep it neat (Optional)
                    document.add(new Paragraph(" ")); 
                    document.add(new Chunk(new LineSeparator()));
                }
            }

            // --- FOOTER ---
            document.add(new Paragraph(" "));
            Paragraph footerTitle = new Paragraph("System Validation", boldFont);
            footerTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(footerTitle);

            Paragraph footerMsg = new Paragraph("This is a computer-generated invoice.\n" +
                    "Returns: Within 7 days with original invoice.\n" +
                    "Support: support@smartstore.com. \n"+ "Thank you for shopping with us!", footerFont);
            footerMsg.setAlignment(Element.ALIGN_CENTER);
            document.add(footerMsg);

            document.close();
        } catch (DocumentException e) { e.printStackTrace(); }
        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- HELPER METHODS ---
    private PdfPCell createStyledHeader(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(52, 73, 94));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
    private PdfPCell createCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }
    private PdfPCell createSummaryCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    private String formatDate(Object dateObj) {
        if(dateObj == null) return "";
        try {
            String dateStr = dateObj.toString();
            if(dateStr.contains("T")) { return dateStr.replace("T", " ").substring(0, 16); }
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
            return sdf.format((Date) dateObj);
        } catch(Exception e) { return dateObj.toString(); }
    }
    public static String convertToWords(long n) {
        if (n == 0) return "Zero";
        if (n < 0) return "Minus " + convertToWords(-n);
        if (n < 20) return units[(int) n];
        if (n < 100) return tens[(int) n / 10] + ((n % 10 != 0) ? " " : "") + units[(int) n % 10];
        if (n < 1000) return units[(int) n / 100] + " Hundred" + ((n % 100 != 0) ? " and " : "") + convertToWords(n % 100);
        if (n < 1000000) return convertToWords(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " : "") + convertToWords(n % 1000);
        return convertToWords(n / 1000000) + " Million" + ((n % 1000000 != 0) ? " " : "") + convertToWords(n % 1000000);
    }
}