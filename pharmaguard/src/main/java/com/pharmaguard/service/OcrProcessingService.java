package com.pharmaguard.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrProcessingService {

    public Map<String, Object> processOcrText(String rawText) {
        Map<String, Object> result = new HashMap<>();
        if (rawText == null || rawText.isBlank()) {
            result.put("confidence", 0.0);
            result.put("extracted", Map.of());
            return result;
        }

        String cleaned = rawText.replaceAll("\r", "\n");
        Map<String, String> extracted = new HashMap<>();

        // Batch Number pattern (e.g. Batch: PCM20260812 or B.No. AMX-102)
        Pattern batchPattern = Pattern.compile("(?i)(?:batch|b\\.no|bno|lot)[\\s#:]*([A-Z0-9\\-]+)");
        Matcher batchMatcher = batchPattern.matcher(cleaned);
        if (batchMatcher.find()) {
            extracted.put("batchNumber", batchMatcher.group(1).trim());
        }

        // Expiry Date pattern (e.g., Exp: 07/2028 or Expiry Date: 2028-07-31)
        Pattern expPattern = Pattern.compile("(?i)(?:exp|expiry|exp\\s*date)[\\s:]*([0-9]{2}[/\\-][0-9]{4}|[0-9]{4}[/\\-][0-9]{2}[/\\-][0-9]{2}|[0-9]{2}[/\\-][0-9]{2}[/\\-][0-9]{4})");
        Matcher expMatcher = expPattern.matcher(cleaned);
        if (expMatcher.find()) {
            extracted.put("expiryDate", expMatcher.group(1).trim());
        }

        // Mfg Date pattern
        Pattern mfgPattern = Pattern.compile("(?i)(?:mfg|mfd|mfg\\s*date)[\\s:]*([0-9]{2}[/\\-][0-9]{4}|[0-9]{4}[/\\-][0-9]{2})");
        Matcher mfgMatcher = mfgPattern.matcher(cleaned);
        if (mfgMatcher.find()) {
            extracted.put("manufacturingDate", mfgMatcher.group(1).trim());
        }

        // Strength pattern (e.g. 500 mg, 500mg, 10 ml, 250 mcg)
        Pattern strengthPattern = Pattern.compile("(?i)\\b(\\d+\\s*(?:mg|g|ml|mcg|iu))\\b");
        Matcher strengthMatcher = strengthPattern.matcher(cleaned);
        if (strengthMatcher.find()) {
            extracted.put("strength", strengthMatcher.group(1).trim());
        }

        // Dosage form pattern
        Pattern formPattern = Pattern.compile("(?i)\\b(Tablet|Tablets|Capsule|Capsules|Syrup|Injection|Ointment|Gel|Solution|Suspension|Drops)\\b");
        Matcher formMatcher = formPattern.matcher(cleaned);
        if (formMatcher.find()) {
            extracted.put("dosageForm", formMatcher.group(1).trim());
        }

        // Barcode pattern (890 prefix or 12-14 digits)
        Pattern barcodePattern = Pattern.compile("\\b(890\\d{9,11}|\\d{12,14})\\b");
        Matcher barcodeMatcher = barcodePattern.matcher(cleaned);
        if (barcodeMatcher.find()) {
            extracted.put("barcode", barcodeMatcher.group(1).trim());
        }

        // Manufacturer pattern (e.g. Mfd by: ABC Pharma, Manufactured by: XYZ)
        Pattern mfrPattern = Pattern.compile("(?i)(?:mfd\\s*by|manufactured\\s*by|mfr)[\\s:]*([A-Za-z0-9\\s,\\.]{3,40})");
        Matcher mfrMatcher = mfrPattern.matcher(cleaned);
        if (mfrMatcher.find()) {
            extracted.put("manufacturer", mfrMatcher.group(1).trim());
        }

        // Simple line heuristic for Medicine Name if not explicitly detected
        String[] lines = cleaned.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 3 && !trimmed.matches("(?i).*(batch|exp|mfg|mfd|price|mrp|license).*")) {
                if (!extracted.containsKey("medicineName")) {
                    extracted.put("medicineName", trimmed);
                    break;
                }
            }
        }

        double confidence = extracted.size() >= 3 ? 0.85 : (extracted.size() >= 1 ? 0.60 : 0.25);
        result.put("confidence", confidence);
        result.put("extracted", extracted);
        result.put("rawText", rawText);
        return result;
    }
}
