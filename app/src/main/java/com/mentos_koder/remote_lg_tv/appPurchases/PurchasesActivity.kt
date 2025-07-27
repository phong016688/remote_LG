package com.mentos_koder.remote_lg_tv.appPurchases

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetailsParams
import com.mentos_koder.remote_lg_tv.BuildConfig
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.view.MainActivity


class PurchasesActivity : AppCompatActivity() {
    private lateinit var billingClient: BillingClient
    private lateinit var linearLayoutStartFree: LinearLayout
    private lateinit var linearLayoutMonthly: LinearLayout
    private lateinit var linearLayoutLifeTime: LinearLayout
    private lateinit var imgClosePurchases: ImageView
    val TAG = "#######Billing"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchases)

        mapping()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "debug")
        } else {
            Log.d(TAG, "release")
        }

        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult, purchases ->
                // Xử lý kết quả mua hàng
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    purchases.forEach { purchase ->
                        handlePurchase(purchase)
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Log.e(TAG, "onCreate:  huy giao dich", )
                } else {
                    Log.e(TAG, "onCreate: billingResult.responseCode" + billingResult.responseCode )
                }
            }
            .build()
        listenEvent()
        establishConnection()

    }

    private fun listenEvent(){
        linearLayoutStartFree.setOnClickListener { launchPurchaseFlow("com.mentos_koder.universalremote_yearly") }
        linearLayoutMonthly.setOnClickListener { launchPurchaseFlow("com.mentos_koder.universalremote_monthly") }
        linearLayoutLifeTime.setOnClickListener { launchPurchaseFlow("com.mentos_koder.universalremote_lifetime") }
         imgClosePurchases.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun mapping(){
        linearLayoutStartFree = findViewById(R.id.startFree)
        linearLayoutMonthly = findViewById(R.id.monthly)
        linearLayoutLifeTime = findViewById(R.id.lifeTime)
        imgClosePurchases = findViewById(R.id.img_closePurchases)
        imgClosePurchases.visibility = View.INVISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            imgClosePurchases.visibility = View.VISIBLE
        }, 3000)
    }
    fun establishConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    showProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                establishConnection()
            }
        })
    }

    private fun showProducts() {
        val skuList = ArrayList<String>()
        skuList.add("com.mentos_koder.universalremote_yearly")
        skuList.add("com.mentos_koder.universalremote_monthly")
        skuList.add("com.mentos_koder.samsungremote.lifetime")

        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP)
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (skuDetails in skuDetailsList) {
                    Log.d(TAG, "Product ID: ${skuDetails.sku}, Price: ${skuDetails.price}, Title: ${skuDetails.title}, Description: ${skuDetails.description}")
                }
            } else {
                Log.e(TAG, "Failed to query SKU details: ${billingResult.responseCode}")
            }
        }
    }

    private fun launchPurchaseFlow(skuId: String) {
        val skuDetailsParams = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(skuId))
            .setType(BillingClient.SkuType.SUBS)
            .build()

        billingClient.querySkuDetailsAsync(skuDetailsParams) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                val skuDetails = skuDetailsList[0]
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build()

                val billingResult = billingClient.launchBillingFlow(this@PurchasesActivity, billingFlowParams)
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "launchPurchaseFlow: "+ billingResult.responseCode )
                }
            } else {
                Log.e(TAG, "launchPurchaseFlow duoi: "+ billingResult.responseCode  )
            }
        }
    }
    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            ) { billingResult: BillingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "handlePurchase: "+ billingResult.responseCode )
                }
            }
            Log.d(TAG, "Purchase Token: ${purchase.purchaseToken}")
            Log.d(TAG, "Purchase Time: ${purchase.purchaseTime}")
            Log.d(TAG, "Purchase OrderID: ${purchase.orderId}")
        }
    }

}