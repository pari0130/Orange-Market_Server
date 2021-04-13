package kr.hs.dgsw.orange_market.service.jwt

import io.jsonwebtoken.*
import kr.hs.dgsw.orange_market.domain.entity.user.UserEntity
import kr.hs.dgsw.orange_market.domain.repository.user.UserRepository
import kr.hs.dgsw.orange_market.util.Constants
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.lang.Exception
import java.security.Key
import java.util.*
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.HashMap

@Service
class JwtServiceImpl(
    private val userRepository: UserRepository
): JwtService {

    @Value("\${jwt.secret.access}")
    private lateinit var secretAccessKey: String

    private val signInKey: Key by lazy {
        SecretKeySpec(secretAccessKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)
    }

    override fun createToken(idx: Int): Mono<String> {
        val date = Date(Date().time + Constants.MILLISECONDS_FOR_A_HOUR * 1)

        val headerParams = HashMap<String, Any>().apply {
            this["typ"] = "JWT"
            this["alg"] = "HS256"
        }

        val claims = HashMap<String, Any>().apply {
            this["idx"] = idx
        }

        return Mono.justOrEmpty(Jwts.builder()
            .setHeaderParams(headerParams)
            .setClaims(claims)
            .setExpiration(date)
            .signWith(signInKey)
            .compact())
    }

    override fun validateToken(token: String): Mono<UserEntity> =
        try {
            val idx = Jwts
                .parserBuilder()
                .setSigningKey(signInKey)
                .build()
                .parseClaimsJws(token)
                .body["idx"].toString().toInt()

            Mono.justOrEmpty(userRepository.findByIdx(idx).map { it })
        } catch (e: ExpiredJwtException) {
            Mono.error(e)
        } catch (e: SignatureException) {
            Mono.error(e)
        } catch (e: MalformedJwtException) {
            Mono.error(e)
        } catch (e: IllegalArgumentException) {
            Mono.error(e)
        } catch (e: Exception) {
            Mono.error(e)
        }
}