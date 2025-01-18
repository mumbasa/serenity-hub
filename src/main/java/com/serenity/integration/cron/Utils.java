package com.serenity.integration.cron;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.format.annotation.DateTimeFormat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class Utils {
public static void main(String args[]){
    String string = "2018-04-10T12313";
   // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
  
   // LocalDateTime date = LocalDateTime.parse(string, formatter);
    System.out.println(string.split("T")[0]);
    //UUID k = UUID.fromString("LLSHHI1415217");
    UUID k = UUID.randomUUID();
    Random sk =new Random(100000000);
    System.err.println(sk.nextInt());

    try {
        // Example usage with logo
        generateQRCode(
            "www.spectacularoptics.vision", // Text or URL to encode
            500,                       // Width of QR code
            500,                       // Height of QR code
            "/home/bryan/Pictures/qr_code_with_logo.png",   // Output file name
            "/home/bryan/Downloads/spec.png"                 // Logo file path (set to null if no logo is needed)
        );
        System.out.println("QR Code with logo generated successfully!");
        
    } catch (WriterException | IOException e) {
        System.out.println("Error generating QR code: " + e.getMessage());
    }
}



public static void generateQRCode(String text, int width, int height, String filePath, String logoPath)
            throws WriterException, IOException {
        // Create QR code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        // Convert bit matrix to BufferedImage
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        if (logoPath != null && !logoPath.isEmpty()) {
            // Load the logo image
            BufferedImage logoImage = ImageIO.read(new File(logoPath));
            
            // Calculate the size of the logo (e.g., 20% of QR code size)
            int logoWidth = width / 3;
            int logoHeight = height / 3;
            
            // Scale the logo
            BufferedImage scaledLogo = new BufferedImage(logoWidth, logoHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledLogo.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(logoImage, 0, 0, logoWidth, logoHeight, null);
            g2d.dispose();
            
            // Calculate center position for logo
            int centerX = (width - logoWidth) / 2;
            int centerY = (height - logoHeight) / 2;
            
            // Create combined image
            BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combined.createGraphics();
            g.drawImage(qrImage, 0, 0, null);
            g.drawImage(scaledLogo, centerX, centerY, null);
            g.dispose();
            
            // Save the final image
            ImageIO.write(combined, "PNG", new File(filePath));
        } else {
            // If no logo is provided, save the QR code directly
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        }
    }
}
