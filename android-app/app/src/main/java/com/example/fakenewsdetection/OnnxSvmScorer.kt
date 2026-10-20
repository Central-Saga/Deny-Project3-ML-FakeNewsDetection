
package com.example.fakenewsdetection

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.nio.FloatBuffer

class OnnxSvmScorer(context: Context) : AutoCloseable {

    private val environment = OrtEnvironment.getEnvironment()

    private val englishSession = environment.createSession(
        context.assets.open("fake_news_model_en_v13.onnx")
            .use { it.readBytes() }
    )

    private val indonesianSession = environment.createSession(
        context.assets.open("fake_news_model_id_political_v3.onnx")
            .use { it.readBytes() }
    )

    fun scoreEnglish(
        wordTfidf: FloatArray,
        charTfidf: FloatArray
    ): Double {

        require(wordTfidf.size == 100000)
        require(charTfidf.size == 50000)

        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(wordTfidf),
            longArrayOf(1, wordTfidf.size.toLong())
        ).use { wordTensor ->

            OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(charTfidf),
                longArrayOf(1, charTfidf.size.toLong())
            ).use { charTensor ->

                englishSession.run(
                    mapOf(
                        "word_tfidf" to wordTensor,
                        "char_tfidf" to charTensor
                    )
                ).use { result ->

                    @Suppress("UNCHECKED_CAST")
                    val output = result[0].value
                            as Array<FloatArray>

                    return output[0][0].toDouble()
                }
            }
        }
    }

    fun scoreIndonesian(
        tfidf: FloatArray
    ): Double {

        require(tfidf.size == 100000)

        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(tfidf),
            longArrayOf(1, tfidf.size.toLong())
        ).use { inputTensor ->

            indonesianSession.run(
                mapOf("tfidf" to inputTensor)
            ).use { result ->

                @Suppress("UNCHECKED_CAST")
                val output = result[0].value
                        as Array<FloatArray>

                return output[0][0].toDouble()
            }
        }
    }

    override fun close() {
        indonesianSession.close()
        englishSession.close()
    }
}