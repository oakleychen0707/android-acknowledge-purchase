private void checkPaymentStatus() {
        String orderId = SharedPreferencesUtility.getOrderId(this);
        String paymentId = SharedPreferencesUtility.getPaymentId(this);

        // 產生 paymentId 後，跳出 App，未紀錄到 orderId
        if (orderId.isEmpty()) {
                initBillingClient(paymentId);
                startBillingConnection(paymentId);

        }
}

// 初始化 BillingClient
private void initBillingClient(String paymentId) {
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        handlePurchases(purchases, paymentId);
                    }
                })
                .build();
}

// 開始與 BillingClient 建立連線
private void startBillingConnection(String paymentId) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    AccessLogUtility.showInfoMessage(true, TAG, "[INFO | onBillingSetupFinished] Billing setup finished successfully", null);
                    queryPurchases(paymentId);
                } else {
                    AccessLogUtility.showInfoMessage(false, TAG, "[ERROR | onBillingSetupFinished] Billing setup failed, response code: " + billingResult.getResponseCode(), null);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // BillingClient 服務中斷
                AccessLogUtility.showInfoMessage(false, TAG, "[ERROR] Billing service disconnected", null);
            }
        });
}

// 查詢訂閱型商品的購買紀錄（SBS，訂閱型）
private void queryPurchases(String paymentId) {
        billingClient.queryPurchasesAsync(BillingClient.ProductType.SUBS, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // 查詢成功，處理購買紀錄
                AccessLogUtility.showInfoMessage(true, TAG, "[INFO | onQueryPurchasesResponse] Found " + purchases.size() + " purchases.", null);
                handlePurchases(purchases, paymentId);
            } else {
                // 查詢失敗
                AccessLogUtility.showInfoMessage(false, TAG, "[ERROR | onQueryPurchasesResponse] Failed to query purchases, response code: " + billingResult.getResponseCode(), null);
            }
        });
}

// 處理購買紀錄，確認每筆購買是否已確認
private void handlePurchases(List<Purchase> purchases, String paymentId) {
        for (Purchase purchase : purchases) {
            if (purchase.isAcknowledged()) {
                // 購買已確認
                AccessLogUtility.showInfoMessage(true, TAG, "[INFO] Purchase already acknowledged, Order ID: " + purchase.getOrderId(), null);
            } else {
                // 購買未確認，進行確認
                acknowledgePurchase(purchase, paymentId);
            }
        }
}

private void acknowledgePurchase(Purchase purchase, String paymentId) {
        // 確認訂單
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                AccessLogUtility.showInfoMessage(true, TAG,
                        "[INFO | acknowledgePurchase] Purchase acknowledged successfully, Order ID: " + purchase.getOrderId(), null);
            } else {
                AccessLogUtility.showInfoMessage(false, TAG,
                        "[ERROR | acknowledgePurchase] Failed to acknowledge purchase, Order ID: " + purchase.getOrderId(), null);
            }
        });
}
