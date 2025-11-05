package com.example.multistoprouter.cache

class SimpleLruCache<K, V>(private val maxSize: Int) {
    init {
        require(maxSize > 0) { "maxSize must be greater than zero" }
    }

    private val cache = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: K): V? = cache[key]

    @Synchronized
    fun put(key: K, value: V) {
        cache[key] = value
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
