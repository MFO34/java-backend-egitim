package com.banka.interfaces;

/**
 * Konsola yazdırılabilir nesneler için arayüz.
 * Bu interface'i implement eden her nesne kendini konsola yazdırabilir.
 *
 * PEDAGOJIK NOT:
 * Bu ikinci interface örneği — bir sınıf hem Transactable
 * hem de Printable implement edebilir. Çoklu interface budur!
 */
public interface Printable {

    /**
     * Nesnenin kısa özetini konsola yazdırır (tek satır).
     */
    void printSummary();

    /**
     * Nesnenin tüm detaylarını konsola yazdırır (çok satır).
     */
    void printDetails();

    /**
     * DEFAULT METOD: Interface'lerde Java 8'den itibaren
     * gövdeli (default) metodlar yazılabilir.
     * Implement eden sınıf bunu override etmek ZORUNDA DEĞİL.
     */
    default void printSeparator() {
        // String.repeat() — Java 11+ özelliği
        System.out.println("-".repeat(50));
    }
}
