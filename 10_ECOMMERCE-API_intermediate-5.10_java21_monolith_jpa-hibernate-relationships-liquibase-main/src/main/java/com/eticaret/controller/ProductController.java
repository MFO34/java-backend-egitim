package com.eticaret.controller;

import com.eticaret.dto.request.CreateProductRequest;
import com.eticaret.dto.request.ProductFilterRequest;
import com.eticaret.dto.request.UpdateProductRequest;
import com.eticaret.dto.response.ProductResponse;
import com.eticaret.dto.response.ProductSummaryResponse;
import com.eticaret.service.impl.ProductServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Ürünler", description = "Ürün CRUD, arama, filtreleme ve stok yönetimi")
public class ProductController {

    private final ProductServiceImpl productService;

    public ProductController(ProductServiceImpl productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Yeni ürün oluştur")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID ile ürün detayı getir")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Slug ile ürün getir")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    @GetMapping
    @Operation(summary = "Tüm ürünleri sayfalı listele (@EntityGraph — N+1 çözümü)")
    public ResponseEntity<Page<ProductSummaryResponse>> getAllProducts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    /**
     * Specification Pattern ile dinamik filtreleme.
     * Query parametreleri → ProductFilterRequest → Specification → SQL
     *
     * Örnek:
     *   GET /products/filter?keyword=telefon&minPrice=1000&maxPrice=50000&inStockOnly=true
     */
    @GetMapping("/filter")
    @Operation(summary = "Ürünleri dinamik filtrele (Specification Pattern)")
    public ResponseEntity<Page<ProductSummaryResponse>> filterProducts(
            @Valid ProductFilterRequest filter,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.filterProducts(filter, pageable));
    }

    @GetMapping("/best-selling")
    @Operation(summary = "En çok satan ürünler")
    public ResponseEntity<List<ProductSummaryResponse>> getBestSelling(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getBestSelling(limit));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ürün güncelle (PATCH davranışı — sadece dolu alanlar)")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ürünü soft delete ile sil")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
