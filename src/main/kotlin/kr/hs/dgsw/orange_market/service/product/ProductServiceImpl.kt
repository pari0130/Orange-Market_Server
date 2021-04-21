package kr.hs.dgsw.orange_market.service.product

import kr.hs.dgsw.orange_market.domain.entity.product.ProductImageEntity
import kr.hs.dgsw.orange_market.domain.mapper.toEntity
import kr.hs.dgsw.orange_market.domain.mapper.toResponse
import kr.hs.dgsw.orange_market.domain.repository.product.ProductImageRepository
import kr.hs.dgsw.orange_market.domain.repository.product.ProductRepository
import kr.hs.dgsw.orange_market.domain.request.product.ProductRequest
import kr.hs.dgsw.orange_market.domain.response.product.ProductResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import reactor.core.publisher.Mono
import javax.transaction.Transactional

@Service
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository
): ProductService {

    @Transactional
    override fun getAllProduct(city: String): Mono<List<ProductResponse>> =
        Mono.justOrEmpty(productRepository.findAllByCityEquals(city).map { productEntity ->
            val imageList = productImageRepository
                .findAllByProductIdxEquals(productEntity.idx!!)
                .map(ProductImageEntity::imageUrl)

            productEntity.toResponse(imageList)
        }).switchIfEmpty(Mono.error(HttpClientErrorException(HttpStatus.NOT_FOUND, "조회 실패")))

    @Transactional
    override fun getProduct(idx: Int): Mono<ProductResponse> =
        Mono.justOrEmpty(
            productRepository.findByIdxEquals(idx)?.toResponse(
                productImageRepository.findAllByProductIdxEquals(idx).map(ProductImageEntity::imageUrl)
            )
        ).switchIfEmpty(Mono.error(HttpClientErrorException(HttpStatus.NOT_FOUND, "조회 실패")))

    @Transactional
    override fun saveProduct(productRequest: ProductRequest): Mono<Unit> =
        Mono.justOrEmpty(productRepository.save(productRequest.toEntity()))
            .switchIfEmpty(Mono.error(HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "저장 실패")))
            .flatMap { saveProductImage(productRequest.imageList, it.idx) }

    @Transactional
    override fun saveProductImage(imageList: List<String>?, idx: Int?): Mono<Unit> =
        Mono.justOrEmpty(
            imageList?.map { image ->
                ProductImageEntity().apply {
                    this.productIdx = idx
                    this.imageUrl = image
                }
            }?.map(productImageRepository::save)
        ).switchIfEmpty(Mono.error(HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "저장 실패")))
         .flatMap { Mono.just(Unit) }

    @Transactional
    override fun updateProduct(idx: Int, productRequest: ProductRequest): Mono<Unit> =
        Mono.justOrEmpty(productImageRepository.deleteAllByProductIdxEquals(idx)).flatMap {
            Mono.justOrEmpty(productRepository.save(productRequest.toEntity().apply { this.idx = idx }))
                .switchIfEmpty(Mono.error(HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "저장 실패")))
        }.flatMap { saveProductImage(productRequest.imageList, idx) }

    @Transactional
    override fun updateSold(idx: Int): Mono<Unit> =
        Mono.justOrEmpty(productRepository.findByIdxEquals(idx))
            .flatMap {
                val entity = it.apply { it.idx = idx }
                if (entity.isSold == 0) {
                    entity.isSold = 1
                } else {
                    entity.isSold = 0
                }
                Mono.justOrEmpty(productRepository.save(entity))
            }.flatMap {
                Mono.just(Unit)
            }.switchIfEmpty(Mono.error(HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "저장 실패")))

    @Transactional
    override fun deleteProduct(idx: Int): Mono<Unit> =
        Mono.justOrEmpty(productRepository.deleteByIdxEquals(idx))
            .flatMap {
                if (it == 0) Mono.error(HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "저장 실패"))
                else Mono.just(Unit)
            }
}