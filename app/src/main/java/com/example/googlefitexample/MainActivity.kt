package com.example.googlefitexample

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.google.android.gms.fitness.FitnessOptions

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"
    private val REQUEST_CODE = 12345

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .build()

        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions
            )
        ) {
            GoogleSignIn.requestPermissions(
                this,
                REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // 認証成功時の処理を書く
                Log.d(tag, "認証成功")

                val TAG = "Fitness"
                // 取得範囲の指定
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                val start = sdf.parse("2022/04/01 00:00:00").time
                val end = sdf.parse("2022/04/31 23:59:59").time

                val request = DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                    .setTimeRange(start, end, TimeUnit.MILLISECONDS)
                    // 取得間隔を変えれる（以下なら１日単位）
                    .bucketByTime(1, TimeUnit.DAYS)
                    .build()

                // 歩数取得
                GoogleSignIn.getLastSignedInAccount(this)?.let {
                    Fitness.getHistoryClient(this, it)
                        .readData(request)
                        .addOnSuccessListener {
                            val buckets = it.buckets
                            buckets.forEach { bucket ->
                                val st = bucket.getStartTime(TimeUnit.MILLISECONDS)
                                val ed = bucket.getEndTime(TimeUnit.MILLISECONDS)

                                Log.d(
                                    tag,
                                    "TYPE_STEP_COUNT_DELTA:${bucket.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)}"
                                )

                                Log.d(
                                    tag,
                                    "AGGREGATE_STEP_COUNT_DELTA:${bucket.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA)}"
                                )

                                Log.d(
                                    tag,
                                    "TYPE_STEP_COUNT_CUMULATIVE:${bucket.getDataSet(DataType.TYPE_STEP_COUNT_CUMULATIVE)}"
                                )

                                try {
                                    val dataSet =
                                        bucket.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA)
                                    val value =
                                        dataSet?.dataPoints?.first()?.getValue(Field.FIELD_STEPS)
                                    Log.d(
                                        TAG,
                                        "取得成功 ${sdf.format(Date(st))} ～ ${sdf.format(Date(ed))} ${value}歩"
                                    )
                                } catch (ex: Exception) {
                                    Log.d(TAG, "Exception ${ex.message}")
                                }
                            }
                        }
                        .addOnFailureListener {
                            Log.d(TAG, "失敗 ${it.message}")
                        }
                        .addOnCanceledListener {
                            Log.d(TAG, "キャンセル")
                        }
                }
            } else {
                // 認証失敗時の処理を書く
            }
        }
    }
}