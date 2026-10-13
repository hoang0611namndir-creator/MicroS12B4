package com.shop.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Khóa phân tán theo sản phẩm, key = lock:product:{id}.
 * Nhiều sản phẩm trong cùng đơn -> khóa theo thứ tự id tăng dần để không bị deadlock giữa các đơn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLockService {

    private static final String KEY_PREFIX = "lock:product:";

    private final RedissonClient redisson;

    @Value("${app.lock.wait-seconds}")
    private long waitSeconds;

    @Value("${app.lock.lease-seconds}")
    private long leaseSeconds;

    public <T> T executeWithLocks(Collection<Long> productIds, Supplier<T> action) {
        List<RLock> acquired = new ArrayList<>();
        try {
            for (Long id : new TreeSet<>(productIds)) {
                RLock lock = redisson.getLock(KEY_PREFIX + id);
                boolean ok;
                try {
                    ok = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Yêu cầu bị gián đoạn, vui lòng thử lại");
                }
                if (!ok) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Hệ thống đang bận, vui lòng thử lại");
                }
                acquired.add(lock);
            }
            return action.get();
        } finally {
            // Luôn nhả khóa (theo thứ tự ngược). Kiểm tra isHeldByCurrentThread vì nếu lease đã hết hạn
            // thì unlock() sẽ ném IllegalMonitorStateException và che mất lỗi gốc.
            for (int i = acquired.size() - 1; i >= 0; i--) {
                RLock lock = acquired.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
