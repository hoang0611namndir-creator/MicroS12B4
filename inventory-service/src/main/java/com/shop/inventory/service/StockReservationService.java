package com.shop.inventory.service;

import com.shop.inventory.domain.Stock;
import com.shop.inventory.domain.StockRepository;
import com.shop.inventory.dto.StockRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    public static final String OUT_OF_STOCK = "Sản phẩm đã hết hàng";

    private final ProductLockService lockService;
    private final StockRepository stockRepository;
    private final TransactionTemplate tx;

    /**
     * Giữ hàng (trừ kho). Thứ tự BẮT BUỘC: giành khóa -> mở transaction -> kiểm tra & trừ -> commit -> nhả khóa.
     * Nếu mở transaction TRƯỚC khi lock (vd @Transactional trên method này), người giành khóa kế tiếp có thể
     * đọc số liệu cũ khi transaction trước chưa commit -> vẫn bán lố.
     */
    public void reserve(List<StockRequest.Item> items) {
        Map<Long, Integer> need = aggregate(items);

        lockService.executeWithLocks(need.keySet(), () -> tx.execute(status -> {
            List<Stock> stocks = new ArrayList<>();
            for (Map.Entry<Long, Integer> e : need.entrySet()) {
                Stock stock = stockRepository.findById(e.getKey()).orElse(null);
                if (stock == null || stock.getQuantity() < e.getValue()) {
                    log.info("[Inventory] HẾT HÀNG productId={} (cần {}, còn {})", e.getKey(), e.getValue(),
                            stock == null ? 0 : stock.getQuantity());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, OUT_OF_STOCK);
                }
                stocks.add(stock);
            }
            for (Stock stock : stocks) {
                stock.setQuantity(stock.getQuantity() - need.get(stock.getProductId()));
                log.info("[Inventory] Giữ hàng productId={} -{} => còn {}", stock.getProductId(),
                        need.get(stock.getProductId()), stock.getQuantity());
            }
            stockRepository.saveAll(stocks);
            return null;
        }));
    }

    /** Hoàn kho (bù trừ khi lưu đơn thất bại sau khi đã giữ hàng). */
    public void release(List<StockRequest.Item> items) {
        Map<Long, Integer> back = aggregate(items);

        lockService.executeWithLocks(back.keySet(), () -> tx.execute(status -> {
            for (Map.Entry<Long, Integer> e : back.entrySet()) {
                Stock stock = stockRepository.findById(e.getKey()).orElse(new Stock(e.getKey(), 0));
                stock.setQuantity(stock.getQuantity() + e.getValue());
                stockRepository.save(stock);
                log.info("[Inventory] Hoàn kho productId={} +{} => còn {}", e.getKey(), e.getValue(), stock.getQuantity());
            }
            return null;
        }));
    }

    private Map<Long, Integer> aggregate(List<StockRequest.Item> items) {
        Map<Long, Integer> map = new TreeMap<>();
        items.forEach(i -> map.merge(i.productId(), i.quantity(), Integer::sum));
        return map;
    }
}
