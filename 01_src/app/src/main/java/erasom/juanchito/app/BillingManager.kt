package erasom.juanchito.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.*

class BillingManager private constructor(context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_REMOVE_ADS = "remove_ads"
        private const val PREFS_NAME = "billing_prefs"
        private const val KEY_NO_ADS = "no_ads_purchased"

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var productDetailsMap = mutableMapOf<String, ProductDetails>()
    var onPurchaseCompleted: (() -> Unit)? = null

    init {
        startConnection()
    }

    fun isAdsRemoved(): Boolean = prefs.getBoolean(KEY_NO_ADS, false)

    private fun setAdsRemoved(removed: Boolean) {
        prefs.edit().putBoolean(KEY_NO_ADS, removed).apply()
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Reintentar en siguiente interacción
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                }
            }
        }
    }

    fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasNoAds = purchases.any { purchase ->
                    purchase.products.contains(PRODUCT_REMOVE_ADS) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                setAdsRemoved(hasNoAds)
                if (hasNoAds) onPurchaseCompleted?.invoke()
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String = PRODUCT_REMOVE_ADS) {
        val details = productDetailsMap[productId] ?: return
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PRODUCT_REMOVE_ADS)) {
                setAdsRemoved(true)
                onPurchaseCompleted?.invoke()
            }
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { }
            }
        }
    }
}
