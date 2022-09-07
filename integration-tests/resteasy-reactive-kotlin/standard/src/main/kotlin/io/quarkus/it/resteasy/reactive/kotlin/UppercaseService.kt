package io.quarkus.it.resteasy.reactive.kotlin

import java.util.Locale
import jakarta.enterprise.context.RequestScoped

@RequestScoped
class UppercaseService {

    fun convert(input: String) = input.uppercase(Locale.ROOT)
}
