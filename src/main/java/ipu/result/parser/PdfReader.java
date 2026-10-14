package ipu.result.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

/** Thin wrapper over Apache PDFBox that returns the plain text of a PDF. */
public class PdfReader {

    public String readText(File pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) {
                throw new IOException("encrypted PDFs are not supported: " + pdf.getName());
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }
}
