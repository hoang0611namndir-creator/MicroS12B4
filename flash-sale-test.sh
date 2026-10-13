#!/usr/bin/env bash
# Mô phỏng flash sale: tồn kho = STOCK, có USERS request đặt mua đồng thời.
# Dùng: ./flash-sale-test.sh [STOCK=1] [USERS=100] [PRODUCT_ID=1]
STOCK=${1:-1}; USERS=${2:-100}; PID=${3:-1}
GW=http://localhost:8080/api

curl -s -X PUT "$GW/inventory/$PID?quantity=$STOCK" > /dev/null
echo ">> Tồn kho sản phẩm $PID = $STOCK, bắn $USERS request đồng thời..."

BODY="{\"customerId\":1,\"items\":[{\"productId\":$PID,\"quantity\":1}]}"
seq "$USERS" | xargs -P "$USERS" -I{} \
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$GW/orders" \
  -H 'Content-Type: application/json' -d "$BODY" | sort | uniq -c

echo ">> Mong đợi: $STOCK x '202' (thành công), $((USERS-STOCK)) x '409' (Sản phẩm đã hết hàng)"
echo ">> Tồn kho còn lại:"; curl -s "$GW/inventory/$PID"; echo
echo ">> Số đơn trong DB:"; curl -s "$GW/orders" | grep -o '"id"' | wc -l
