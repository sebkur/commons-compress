package org.apache.commons.compress.archivers.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public class JdkBug8143613Test {

    /**
     * Source: https://bugs.openjdk.java.net/browse/JDK-8143613
     */
    public static byte[] testData(boolean general_purpose_bit_flag_bit3_on) {
        final byte gpbf = (byte) (general_purpose_bit_flag_bit3_on ? 0x08
                : 0x00);

        return new byte[] {
                // Local File header
                'P', 'K', 3, 4, // Local File Header Signature
                13, 0, // Version needed to extract
                gpbf, 8, // General purpose bit flag
                ZipEntry.STORED, 0, // Compression method
                'q', 'l', 't', 'G', // Last Modification time & date
                0, 0, 0, 0, // CRC32
                0, 0, 0, 0, // Compressed Size
                0, 0, 0, 0, // Uncompressed Size
                12, 0, // File name length
                0, 0, // Extra field length
                'F', 'o', 'l', 'd', 'e', 'r', '_', 'n', 'a', 'm', 'e', '/',
                // File name
                // Central directory file header
                'P', 'K', 1, 2, // Central Directory File Header Signature
                13, 0, // Version made by
                13, 0, // Version needed to extract
                gpbf, 8, // General purpose bit flag
                ZipEntry.STORED, 0, // Compression method
                'q', 'l', 't', 'G', // Last Modification time & date
                0, 0, 0, 0, // CRC32
                0, 0, 0, 0, // Compressed Size
                0, 0, 0, 0, // Uncompressed Size
                12, 0, // File name length
                0, 0, // Extra field length
                0, 0, // File comment length
                0, 0, // Disk number where file starts
                0, 0, // Internal File attributes
                0, 0, 0, 0, // External File attributes
                0, 0, 0, 0, // Relative offset of local header file
                'F', 'o', 'l', 'd', 'e', 'r', '_', 'n', 'a', 'm', 'e', '/',
                // File name
                // End of Central Directory Record
                'P', 'K', 5, 6, // Local File Header Signature
                0, 0, // Number of this disk
                0, 0, // Disk where CD starts
                1, 0, // Number of CD records on this disk
                1, 0, // Total number of records
                58, 0, 0, 0, // Size of CD
                42, 0, 0, 0, // Offset of start of CD
                0, 0, // Comment length
        };
    }

    /**
     * If we use the default ZipArchiveInputStream constructor, we get an
     * exception while reading the entries.
     */
    @Test(expected = UnsupportedZipFeatureException.class)
    public void testFailing() throws IOException {
        byte[] contents = testData(true);
        InputStream is = new ByteArrayInputStream(contents);
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(is)) {
            while (true) {
                ZipArchiveEntry entry = zis.getNextZipEntry();
                if (entry == null) {
                    break;
                }
                System.out
                        .println(String.format("entry: '%s'", entry.getName()));
            }
        }
    }

    /**
     * If we use the extended ZipArchiveInputStream constructor, and pass the
     * default options, we get an exception while reading the entries as with
     * the default constructor.
     */
    @Test(expected = UnsupportedZipFeatureException.class)
    public void testFailingWithExtendedConstructor() throws IOException {
        byte[] contents = testData(true);
        InputStream is = new ByteArrayInputStream(contents);
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(is,
                StandardCharsets.UTF_8.toString(), true, false)) {
            while (true) {
                ZipArchiveEntry entry = zis.getNextZipEntry();
                if (entry == null) {
                    break;
                }
                System.out
                        .println(String.format("entry: '%s'", entry.getName()));
            }
        }
    }

    /**
     * If we use the extended ZipArchiveInputStream constructor and pass true
     * for the parameter allowStoredEntriesWithDataDescriptor, we can read the
     * names of each entry.
     *
     * Ideally this test would succeed, but it doesn't.
     */
    @Test
    public void testSuccess() throws IOException {
        Map<String, byte[]> entryToData = new HashMap<>();

        byte[] contents = testData(true);
        InputStream is = new ByteArrayInputStream(contents);
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(is,
                StandardCharsets.UTF_8.toString(), true, true)) {
            while (true) {
                ZipArchiveEntry entry = zis.getNextZipEntry();
                if (entry == null) {
                    break;
                }
                long uncompressedSize = entry.getSize();
                long compressedSize = entry.getCompressedSize();
                System.out.println(String.format(
                        "entry: '%s' compressed: %d, uncompressed: %d",
                        entry.getName(), compressedSize, uncompressedSize));
                if (uncompressedSize >= 0) {
                    byte[] data = new byte[(int) uncompressedSize];
                    IOUtils.readFully(zis, data);
                    entryToData.put(entry.getName(), data);
                }
            }
        }
    }

}
