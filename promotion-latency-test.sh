#!/usr/bin/env bash
# Đo thời gian từ lúc admin đổi khuyến mãi đến lúc giá mới hiển thị qua API.
# Dùng: ./promotion-latency-test.sh [PRODUCT_ID=1] [DISCOUNT_PERCENT=30]
PID=${1:-1}; PCT=${2:-30}
GW=http://localhost:8080/api
JSON='Content-Type: application/json'

show() { curl -s -D - "$GW/products/$PID" | tr -d '\r' | grep -iE '^x-cache|^\{'; }

echo "== Reset: xóa khuyến mãi cũ (nếu có), rồi gọi 2 lần để làm nóng cache"
curl -s -o /dev/null -X DELETE "$GW/promotions/$PID"
sleep 0.5
show; show

echo; echo "== Admin đặt khuyến mãi $PCT% cho sản phẩm $PID"
start=$(date +%s%N)
curl -s -o /dev/null -X PUT "$GW/promotions/$PID" -H "$JSON" -d "{\"discountPercent\":$PCT,\"active\":true}"

body=""
while (( $(date +%s%N) - start < 5000000000 )); do
  body=$(curl -s "$GW/products/$PID")
  if echo "$body" | grep -q "\"discountPercent\":$PCT"; then
    end=$(date +%s%N)
    echo "Giá mới hiển thị sau $(( (end - start) / 1000000 )) ms (đã gồm cả thời gian gọi PUT):"
    echo "$body"
    exit 0
  fi
done
echo "Quá 5 giây vẫn chưa thấy giá mới. Kiểm tra Redis và log product-service."
exit 1
