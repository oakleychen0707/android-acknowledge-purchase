# AndroidAcknowledgePurchase

## Overview
This code handles the process of checking the payment status and managing in-app purchases using Google's BillingClient.

## Features
- Retrieves stored payment and order IDs.
- Initializes BillingClient and establishes a connection.
- Queries purchase records and acknowledges unconfirmed purchases.

## Code Structure
### 1. **Checking Payment Status**
```java
private void checkPaymentStatus() {
    String orderId = SharedPreferencesUtility.getOrderId(this);
    String paymentId = SharedPreferencesUtility.getPaymentId(this);

    // If orderId is missing but paymentId exists, reinitialize billing client
    if (orderId.isEmpty()) {
        initBillingClient(paymentId);
        startBillingConnection(paymentId);
    }
}
```

### 2. **Initializing BillingClient**
```java
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
```

### 3. **Starting Billing Connection**
```java
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
            AccessLogUtility.showInfoMessage(false, TAG, "[ERROR] Billing service disconnected", null);
        }
    });
}
```

### 4. **Querying Purchases**
```java
private void queryPurchases(String paymentId) {
    billingClient.queryPurchasesAsync(BillingClient.ProductType.SUBS, (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            AccessLogUtility.showInfoMessage(true, TAG, "[INFO | onQueryPurchasesResponse] Found " + purchases.size() + " purchases.", null);
            handlePurchases(purchases, paymentId);
        } else {
            AccessLogUtility.showInfoMessage(false, TAG, "[ERROR | onQueryPurchasesResponse] Failed to query purchases, response code: " + billingResult.getResponseCode(), null);
        }
    });
}
```

### 5. **Handling Purchases**
```java
private void handlePurchases(List<Purchase> purchases, String paymentId) {
    for (Purchase purchase : purchases) {
        if (purchase.isAcknowledged()) {
            AccessLogUtility.showInfoMessage(true, TAG, "[INFO] Purchase already acknowledged, Order ID: " + purchase.getOrderId(), null);
        } else {
            acknowledgePurchase(purchase, paymentId);
        }
    }
}
```

### 6. **Acknowledging Purchases**
```java
private void acknowledgePurchase(Purchase purchase, String paymentId) {
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
```

## How It Works
1. **Retrieves** `orderId` and `paymentId` from `SharedPreferencesUtility`.
2. If `orderId` is missing, it **reinitializes** the BillingClient and starts the billing connection.
3. On a successful billing setup, it **queries existing purchases**.
4. If any purchases are found, it checks if they have been **acknowledged**.
5. If a purchase is unacknowledged, it **confirms the purchase** via `acknowledgePurchase()`.

## Error Handling
- Logs messages using `AccessLogUtility` to track billing responses.
- Handles BillingClient disconnections.
- Checks for and processes unacknowledged purchases.

## Dependencies
Ensure you have the **Google Play Billing Library** added to your `build.gradle`:
```gradle
dependencies {
    implementation "com.android.billingclient:billing:7.0.0"
    implementation "com.android.billingclient:billing-ktx:7.0.0"
}
```

## Notes
- This code supports **subscription purchases** (`BillingClient.ProductType.SUBS`).
- Purchases must be **acknowledged** to prevent refunds.
- Make sure to handle network issues and BillingClient disconnections properly.

