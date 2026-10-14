package com.vplayed.core.network

import okhttp3.Request
import retrofit2.Invocation

/**
 * Marks a Retrofit method whose request must not carry the geo token (`x-bearer-token`), e.g. the
 * request that fetches the token itself.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithoutGeoToken

/**
 * Marks a Retrofit method whose request/response bodies contain secrets or personal data
 * (encrypted credentials, tokens, user profiles). Its bodies are never logged.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedactBody

/** True when the Retrofit method that created this request carries annotation [A]. */
internal inline fun <reified A : Annotation> Request.hasMethodAnnotation(): Boolean =
    tag(Invocation::class.java)?.method()?.isAnnotationPresent(A::class.java) == true
