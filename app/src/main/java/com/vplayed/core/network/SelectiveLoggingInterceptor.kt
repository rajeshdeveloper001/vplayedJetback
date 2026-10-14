package com.vplayed.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * HTTP logging for debuggable environments. Logs bodies, except for methods annotated
 * [RedactBody] (headers only), and always redacts the geo token header.
 */
class SelectiveLoggingInterceptor(
    logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT,
) : Interceptor {

    private val withBody = HttpLoggingInterceptor(logger).apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader(ApiHeadersInterceptor.GEO_TOKEN_HEADER)
        redactHeader(ApiHeadersInterceptor.AUTHORIZATION_HEADER)
    }
    private val headersOnly = HttpLoggingInterceptor(logger).apply {
        level = HttpLoggingInterceptor.Level.HEADERS
        redactHeader(ApiHeadersInterceptor.GEO_TOKEN_HEADER)
        redactHeader(ApiHeadersInterceptor.AUTHORIZATION_HEADER)
    }

    override fun intercept(chain: Interceptor.Chain): Response =
        if (chain.request().hasMethodAnnotation<RedactBody>()) {
            headersOnly.intercept(chain)
        } else {
            withBody.intercept(chain)
        }
}
