package com.eticaret.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(max = 300)
    String name,

    @NotBlank(message = "Slug boş olamaz")
    @Size(max = 300)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug sadece küçük harf, rakam ve tire içerebilir")
    String slug,

    String description,

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.01", message = "Fiyat 0'dan büyük olmalıdır")
    BigDecimal price,

    BigDecimal originalPrice,

    @NotNull @Min(0) Integer stockQuantity,

    Long categoryId,

    // Ürün oluşturulurken etiket isimleri (yeni tag'ler de oluşturulabilir)
    List<String> tagNames
) {}
