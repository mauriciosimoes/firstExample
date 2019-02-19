package com.example.dao

import com.example.model.Conta
import java.io.File
import org.ehcache.CacheManagerBuilder
import org.ehcache.config.CacheConfigurationBuilder
import org.ehcache.config.ResourcePoolsBuilder
import org.ehcache.config.persistence.CacheManagerPersistenceConfiguration
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit

/**
 * An Ehcache based implementation for the [DAOFacade] that uses a [delegate] facade and a [storagePath]
 * and perform several caching strategies for each domain operation.
 */
class DAOFacadeCache(val delegate: DAOFacade, val storagePath: File) : DAOFacade {
    /**
     * Build a cache manager with a cache for kweets and other for users.
     * It uses the specified [storagePath] for persistence.
     * Limits the cache to 1000 entries, 10MB in memory, and 100MB in disk per both caches.
     */
    val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerPersistenceConfiguration(storagePath))
        .withCache("contaCache",
            CacheConfigurationBuilder.newCacheConfigurationBuilder<Int, Conta>()
                .withResourcePools(
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(100, EntryUnit.ENTRIES)
                    .offheap(1, MemoryUnit.MB)
                    .disk(10, MemoryUnit.MB, true)
                )
                .buildConfig(Int::class.javaObjectType, Conta::class.java))
        .build(true)

    /**
     * Gets the cache for Conta represented by an [Int] key and a  [Conta] value.
     */
    val contaCache = cacheManager.getCache("contaCache", Int::class.javaObjectType, Conta::class.java)

    override fun init() {
        delegate.init()
    }

    override fun conta(contaId: Int): Conta? {
        val cached = contaCache.get(contaId)
        if (cached != null) {
            return cached
        }

        val conta = delegate.conta(contaId)
        if (conta != null) {
            contaCache.put(contaId, conta)
        }

        return conta
    }

    override fun conta(): ListIterator<Conta> {
        return delegate.conta()
    }

    override fun createConta(text: String, isDefaut: Boolean): Int {
        val id = delegate.createConta(text, isDefaut)
        val conta = Conta(id, text, isDefaut)
        contaCache.put(id, conta)
        return id
    }

    override fun close() {
        try {
            delegate.close()
        } finally {
            cacheManager.close()
        }
    }
}
