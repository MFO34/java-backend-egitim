package com.ogrenci.model;

import java.util.List;

/**
 * JAVA 21 SEALED CLASS ve PATTERN MATCHING KAVRAMI:
 *
 * Sealed interface: hangi sınıfların implement edebileceğini kısıtlar.
 * "permits" → sadece izin verilen sınıflar implement edebilir.
 *
 * NEDEN KULLANIYORUZ?
 * DAO metodları bazen başarılı, bazen boş, bazen hatalı sonuç döner.
 * Bu üç durumu tip güvenli şekilde modelleriz:
 *   Success<T>  → veri var, döner
 *   Empty       → sorgu çalıştı ama sonuç yok
 *   Failure     → hata oluştu
 *
 * Switch'te Pattern Matching ile kullanılır (Java 21):
 *   switch (result) {
 *     case Success<Student> s -> System.out.println(s.data());
 *     case Empty e            -> System.out.println("Bulunamadı");
 *     case Failure f          -> System.out.println(f.message());
 *   }
 */
public sealed interface QueryResult<T>
        permits QueryResult.Success, QueryResult.Empty, QueryResult.Failure, QueryResult.ListSuccess {

    /**
     * Success: sorgu başarılı, veri var.
     * Record olduğu için otomatik getter: data()
     */
    record Success<T>(T data) implements QueryResult<T> {}

    /**
     * Empty: sorgu çalıştı ama sonuç yok (0 satır döndü).
     * Tip parametresi yok çünkü veri taşımıyor.
     * @SuppressWarnings: ham tip uyarısını sustur
     */
    @SuppressWarnings("rawtypes")
    record Empty() implements QueryResult {}

    /**
     * Failure: bir hata oluştu.
     * message: hata açıklaması
     * cause: orijinal exception (debug için)
     */
    record Failure<T>(String message, Throwable cause) implements QueryResult<T> {
        // Sadece mesajla oluşturma (cause olmadan)
        public Failure(String message) {
            this(message, null);
        }
    }

    // ---- Yardımcı factory metodları ----

    /** Başarılı sonuç oluştur */
    static <T> QueryResult<T> success(T data) {
        return new Success<>(data);
    }

    /** Boş sonuç oluştur */
    @SuppressWarnings("unchecked")
    static <T> QueryResult<T> empty() {
        return new Empty();
    }

    /** Hata sonucu oluştur */
    static <T> QueryResult<T> failure(String message) {
        return new Failure<>(message);
    }

    static <T> QueryResult<T> failure(String message, Throwable cause) {
        return new Failure<>(message, cause);
    }

    // ---- Liste için özel Success ----
    record ListSuccess<T>(List<T> items) implements QueryResult<List<T>> {
        public int count() { return items.size(); }
        public boolean isEmpty() { return items.isEmpty(); }
    }

    static <T> QueryResult<List<T>> successList(List<T> items) {
        return new ListSuccess<>(items);
    }
}
