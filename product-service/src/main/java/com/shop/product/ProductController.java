package com.shop.product;

import com.shop.product.dto.ProductView;
import com.shop.product.service.ProductQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;
    private final ProductQueryService queryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) {
        product.setId(null);
        return repository.save(product);
    }

    @GetMapping
    public List<Product> findAll() {
        return repository.findAll();
    }

    /** Header X-Cache: HIT/MISS giúp quan sát việc cache bị xóa khi có thông báo khuyến mãi. */
    @GetMapping("/{id}")
    public ResponseEntity<ProductView> findById(@PathVariable Long id) {
        ProductQueryService.Lookup lookup = queryService.find(id);
        return ResponseEntity.ok()
                .header("X-Cache", lookup.cacheHit() ? "HIT" : "MISS")
                .body(lookup.view());
    }
}
